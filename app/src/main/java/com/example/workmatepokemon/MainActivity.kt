package com.example.workmatepokemon

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.workmatepokemon.config.PokemonApp
import com.example.workmatepokemon.data.Pokemon
import com.example.workmatepokemon.data.SortType
import com.example.workmatepokemon.databinding.ActivityMainBinding
import com.example.workmatepokemon.domain.PokemonAdapter
import com.example.workmatepokemon.presentation.PokemonView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.workmatepokemon.domain.PokemonPresenter

class MainActivity : AppCompatActivity(), PokemonView {

    private lateinit var adapter: PokemonAdapter
    private val pokemonList = ArrayList<Pokemon>()
    private lateinit var presenter: PokemonPresenter
    private lateinit var binding: ActivityMainBinding

    private val selectedTypes = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.layoutManager = layoutManager
        adapter = PokemonAdapter(pokemonList, this)
        binding.recyclerView.adapter = adapter

        presenter = PokemonPresenter(this, pokemonApp().repository)

        // pull-to-refresh
        binding.swipeRefresh.setOnRefreshListener { presenter.onViewCreated() }

        // Пагинация при скролле
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val total = layoutManager.itemCount
                val last = layoutManager.findLastVisibleItemPosition()
                if (total <= last + 5) { // за 5 элементов до конца
                    presenter.loadNextPage()
                }
            }
        })

        // SearchBar
        findViewById<ComposeView>(R.id.searchBar).setContent {
            MaterialTheme {
                var searchText by remember { mutableStateOf("") }
                PokemonSearchBar(
                    query = searchText,
                    onQueryChange = {
                        searchText = it
                        presenter.onSearchQueryChanged(it)
                    },
                    onSearch = {}
                )
            }
        }

        setupFilterButton()
        presenter.onViewCreated()
    }

    private fun setupFilterButton() {
        binding.filterButton.setOnClickListener { showFilterDialog() }
    }

    private fun showFilterDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        dialog.setContentView(view)

        var tempSort: SortType? = presenter.currentSort
        val tempTypes = selectedTypes.toMutableSet()

        // Сортировка: сразу создаём Map<Button, SortType>
        val sortButtons: Map<Button, SortType> = mapOf(
            view.findViewById<Button>(R.id.sort_by_number) to SortType.NUMBER,
            view.findViewById<Button>(R.id.sort_by_name) to SortType.NAME,
            view.findViewById<Button>(R.id.sort_by_hp) to SortType.HP,
            view.findViewById<Button>(R.id.sort_by_attack) to SortType.ATTACK,
            view.findViewById<Button>(R.id.sort_by_defense) to SortType.DEFENSE
        )

        // Подсветка текущей сортировки при открытии
        sortButtons.forEach { (button, type) ->
            updateSortButtonState(button, tempSort == type)
        }

        // Обработка нажатий
        sortButtons.forEach { (button, type) ->
            button.setOnClickListener {
                tempSort = if (tempSort == type) null else type
                // обновляем подсветку всех кнопок
                sortButtons.forEach { (b, t) ->
                    updateSortButtonState(b, tempSort == t)
                }
            }
        }

        // Фильтры по типам
        val typeButtons = mapOf(
            "normal" to R.id.filter_normal, "fire" to R.id.filter_fire,
            "water" to R.id.filter_water, "electric" to R.id.filter_electric,
            "grass" to R.id.filter_grass, "ice" to R.id.filter_ice,
            "fighting" to R.id.filter_fighting, "poison" to R.id.filter_poison,
            "flying" to R.id.filter_flying, "ground" to R.id.filter_ground,
            "psychic" to R.id.filter_psychic, "bug" to R.id.filter_bug,
            "rock" to R.id.filter_rock, "ghost" to R.id.filter_ghost,
            "dragon" to R.id.filter_dragon, "dark" to R.id.filter_dark,
            "steel" to R.id.filter_steel, "fairy" to R.id.filter_fairy
        )
        typeButtons.forEach { (type, id) ->
            val button = view.findViewById<Button>(id)
            updateFilterButtonState(button, tempTypes.contains(type))
            button.setOnClickListener {
                if (tempTypes.contains(type)) tempTypes.remove(type) else tempTypes.add(type)
                updateFilterButtonState(button, tempTypes.contains(type))
            }
        }

        // Apply filters & sort
        view.findViewById<Button>(R.id.submit_button).setOnClickListener {
            selectedTypes.clear()
            selectedTypes.addAll(tempTypes)
            presenter.onFilterChanged(selectedTypes.toList())
            presenter.onSortSelected(tempSort ?: SortType.NUMBER)
            dialog.dismiss()
        }

        view.findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setCancelable(false)
        dialog.show()
    }

    // Подсветка кнопки сортировки
    private fun updateSortButtonState(button: Button, selected: Boolean) {
        if (selected) {
            button.setBackgroundColor(Color.GREEN)
            button.setTextColor(Color.WHITE)
        } else {
            button.setBackgroundColor(button.context.getColor(android.R.color.white))
            button.setTextColor(Color.BLACK)
        }
    }

    private fun updateFilterButtonState(button: Button, selected: Boolean) {
        button.isSelected = selected
        if (selected) {
            button.setBackgroundColor(Color.GREEN)
            button.setTextColor(Color.WHITE)
        } else {
            button.setBackgroundColor(button.context.getColor(android.R.color.darker_gray))
            button.setTextColor(Color.BLACK)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PokemonSearchBar(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
        val focusManager = LocalFocusManager.current
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = {
                onSearch()
                focusManager.clearFocus()
            },
            active = false,
            onActiveChange = {},
            placeholder = { Text("Поиск покемона...") }
        ) {}
    }

    private fun Context.pokemonApp(): PokemonApp = applicationContext as PokemonApp

    // ======== PokemonView методы ========
    override fun showPokemons(pokemons: List<Pokemon>, replace: Boolean) {
        if (replace) pokemonList.clear()
        pokemonList.addAll(pokemons)
        adapter.notifyDataSetChanged()
        hideNoResultsMessage()
    }

    override fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        binding.progressBar.visibility = View.INVISIBLE
        binding.swipeRefresh.isRefreshing = false
    }

    override fun clearPokemons() {
        pokemonList.clear()
        adapter.notifyDataSetChanged()
    }

    override fun showNoResultsMessage(message: String) {
        binding.noResultsText.text = message
        binding.noResultsText.visibility = View.VISIBLE
    }

    override fun hideNoResultsMessage() {
        binding.noResultsText.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }
}
