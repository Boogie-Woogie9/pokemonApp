package com.example.workmatepokemon.presentation

import com.example.workmatepokemon.data.Pokemon

interface PokemonView {
    fun showPokemons(pokemons: List<Pokemon>, replace: Boolean)
    fun showError(message: String)
    fun showLoading()
    fun hideLoading()
    fun clearPokemons()
//    fun showSearchResults(pokemons: List<Pokemon>)
    fun showNoResultsMessage(message: String)
    fun hideNoResultsMessage()
}