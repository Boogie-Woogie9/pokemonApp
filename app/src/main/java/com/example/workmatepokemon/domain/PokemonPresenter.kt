package com.example.workmatepokemon.domain

import com.example.workmatepokemon.data.Pokemon
import com.example.workmatepokemon.data.PokemonRepository
import com.example.workmatepokemon.data.SortType
import com.example.workmatepokemon.presentation.PokemonView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private val allPokemons = mutableListOf<Pokemon>()       // весь буфер с сервера
    private val displayedPokemons = mutableListOf<Pokemon>() // после фильтров

    private val presenterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun onViewCreated() {
        resetAndLoad()
    }

    private fun resetAndLoad() {
        currentOffset = 0
        allLoaded = false
        allPokemons.clear()
        displayedPokemons.clear()
        loadPokemons(reset = true)
    }

    fun loadNextPage() {
        if (isLoadingPage || allLoaded) return
        loadPokemons(reset = false)
    }

    private fun loadPokemons(reset: Boolean) {
        if (isLoadingPage) return
        isLoadingPage = true
        view?.showLoading()

        presenterScope.launch {
            try {
                val newPokemons = withContext(Dispatchers.IO) {
                    repository.fetchPokemonsFromNetwork(limit = pageLimit, offset = currentOffset)
                }

                if (newPokemons.isEmpty()) {
                    allLoaded = true
                } else {
                    currentOffset += pageLimit
                    allPokemons.addAll(newPokemons)
                }

                // Фильтруем и сортируем
                applyFilters()

            } catch (e: Exception) {
                view?.showError(e.message ?: "Unknown Error")
            } finally {
                view?.hideLoading()
                isLoadingPage = false
            }
        }
    }

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
        displayedPokemons.clear()
        var filtered = allPokemons.toList()

        currentQuery?.let { query ->
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) }
        }

        if (selectedTypes.isNotEmpty()) {
            filtered = filtered.filter { poke ->
                val typesList = poke.types?.split(",")?.map { it.trim() } ?: emptyList()
                typesList.any { it in selectedTypes }
            }
        }

        filtered = when (currentSort) {
            SortType.NAME -> filtered.sortedBy { it.name }
            SortType.HP -> filtered.sortedByDescending { it.hp }
            SortType.ATTACK -> filtered.sortedByDescending { it.attack }
            SortType.DEFENSE -> filtered.sortedByDescending { it.defense }
            SortType.NUMBER -> filtered.sortedBy { it.id }
        }

        displayedPokemons.addAll(filtered)
        view?.showPokemons(displayedPokemons, replace = true)

        if (displayedPokemons.isEmpty()) {
            view?.showNoResultsMessage("Покемоны не найдены")
        } else {
            view?.hideNoResultsMessage()
        }
    }

    fun onDestroy() {
        presenterScope.cancel()
        view = null
    }
}
