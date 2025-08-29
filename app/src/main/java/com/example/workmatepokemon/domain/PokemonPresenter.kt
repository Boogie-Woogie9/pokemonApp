package com.example.workmatepokemon.domain

import android.util.Log
import com.example.workmatepokemon.data.Pokemon
import com.example.workmatepokemon.data.PokemonRepository
import com.example.workmatepokemon.data.SortType
import com.example.workmatepokemon.presentation.PokemonView
import kotlinx.coroutines.*

class PokemonPresenter(
    private var view: PokemonView?,
    private val repository: PokemonRepository
) {

    private var currentQuery: String? = null
    private var selectedTypes: List<String> = emptyList()
    var currentSort: SortType = SortType.NUMBER

    private val pageLimit = 20
    private var currentOffset = 0
    private var isLoadingPage = false
    private var allLoaded = false

    // Буферы
    private val allPokemons = mutableListOf<Pokemon>()        // всё, что знаем локально/из сети
    private val displayedPokemons = mutableListOf<Pokemon>()  // то, что показано с учётом фильтров
    private val loadedIds = mutableSetOf<Int>()               // для дедупликации

    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun onViewCreated() {
        resetAndLoad()
    }

    private fun resetAndLoad() {
        currentOffset = 0
        allLoaded = false
        isLoadingPage = false
        allPokemons.clear()
        displayedPokemons.clear()
        loadedIds.clear()
        loadPokemons(reset = true)
    }

    fun loadNextPage() {
        if (isLoadingPage || allLoaded) return
        loadPokemons(reset = false)
    }

    /**
     * Оффлайн-first:
     * 1) Быстро показываем локальные данные (Room), сдвигаем offset = локальному размеру
     * 2) Пытаемся догрузить страницу из сети (по текущему offset)
     * 3) В UI при reset отправляем весь отфильтрованный список, при подзагрузке — только дельту
     */
    private fun loadPokemons(reset: Boolean) {
        if (isLoadingPage) return
        isLoadingPage = true

        presenterScope.launch {
            view?.showLoading()
            try {
                // 1) Локальный снимок (всегда можно показать мгновенно)
                val local = withContext(Dispatchers.IO) { repository.fetchPokemonsFromLocalStorage() }
                if (local.isNotEmpty()) {
                    // добавляем уникальные
                    val newlyAdded = local.filter { loadedIds.add(it.id) }
                    if (newlyAdded.isNotEmpty()) {
                        allPokemons.addAll(newlyAdded)
                    }

                    // Если это первый запуск/сброс — показываем полную выборку по текущим фильтрам
                    if (reset) {
                        val filteredFull = applyFiltersAndSort(allPokemons)
                        displayedPokemons.clear()
                        displayedPokemons.addAll(filteredFull)
                        view?.showPokemons(filteredFull, replace = true)
                        view?.hideNoResultsMessage()
                    }

                    // Очень важно: сдвигаем offset к количеству локальных,
                    // чтобы сетевой запрос начинался с "следующей" страницы
                    if (local.size > currentOffset) {
                        currentOffset = local.size
                    }
                }

                // 2) Сетевая страница (если сеть недоступна, репозиторий вернёт all() — обработаем)
                val rawNetwork = withContext(Dispatchers.IO) {
                    repository.fetchPokemonsFromNetwork(limit = pageLimit, offset = currentOffset)
                }

                // Если репозиторий в оффлайне вернул весь Room (size > pageLimit),
                // нормализуем до страницы текущего offset
                val networkPage =
                    if (rawNetwork.size > pageLimit) {
                        rawNetwork.drop(currentOffset).take(pageLimit)
                    } else rawNetwork

                if (networkPage.isEmpty()) {
                    // либо реально пустая страница (дошли до конца), либо оффлайн и локально уже всё показали
                    allLoaded = true
                } else {
                    // 3) Добавляем только новые id (дедуп)
                    val trulyNew = networkPage.filter { loadedIds.add(it.id) }
                    if (trulyNew.isNotEmpty()) {
                        allPokemons.addAll(trulyNew)

                        // Считаем дельту под текущие фильтры и добавляем только её
                        val deltaForUi = applyFiltersAndSort(trulyNew)
                        if (reset) {
                            // при reset мы уже показали локальные — теперь заменим на полную (локал+сеть)
                            val fullNow = applyFiltersAndSort(allPokemons)
                            displayedPokemons.clear()
                            displayedPokemons.addAll(fullNow)
                            view?.showPokemons(fullNow, replace = true)
                        } else {
                            if (deltaForUi.isNotEmpty()) {
                                displayedPokemons.addAll(deltaForUi)
                                view?.showPokemons(deltaForUi, replace = false)
                            }
                        }
                    }

                    // Сдвигаем offset на фактический размер полученной страницы
                    currentOffset += networkPage.size
                }

                if (displayedPokemons.isEmpty() && reset) {
                    view?.showNoResultsMessage("Покемоны не найдены")
                }

            } catch (e: Exception) {
                Log.e("Presenter", "loadPokemons error", e)
                if (displayedPokemons.isEmpty()) {
                    view?.showError(e.message ?: "Unknown Error")
                }
            } finally {
                view?.hideLoading()
                isLoadingPage = false
            }
        }
    }

    // --- фильтры/сортировка — без доп. сетевых вызовов ---
    fun onSearchQueryChanged(query: String) {
        currentQuery = query.trim().ifBlank { null }
        applyFilters()
    }

    fun onFilterChanged(types: List<String>) {
        selectedTypes = types
        applyFilters()
    }

    fun onSortSelected(sortType: SortType) {
        currentSort = sortType
        applyFilters()
    }

    private fun applyFilters() {
        val filtered = applyFiltersAndSort(allPokemons)
        displayedPokemons.clear()
        displayedPokemons.addAll(filtered)
        view?.showPokemons(filtered, replace = true)
        if (filtered.isEmpty()) view?.showNoResultsMessage("Покемоны не найдены") else view?.hideNoResultsMessage()
    }

    private fun applyFiltersAndSort(list: List<Pokemon>): List<Pokemon> {
        var res = list

        currentQuery?.let { q ->
            res = res.filter { it.name.contains(q, ignoreCase = true) }
        }

        if (selectedTypes.isNotEmpty()) {
            res = res.filter { p ->
                val types = p.types?.split(",")?.map { it.trim() } ?: emptyList()
                types.any { it in selectedTypes }
            }
        }

        res = when (currentSort) {
            SortType.NAME -> res.sortedBy { it.name }
            SortType.HP -> res.sortedByDescending { it.hp }
            SortType.ATTACK -> res.sortedByDescending { it.attack }
            SortType.DEFENSE -> res.sortedByDescending { it.defense }
            SortType.NUMBER -> res.sortedBy { it.id }
        }

        return res
    }

    fun onDestroy() {
        presenterScope.cancel()
        view = null
    }
}
