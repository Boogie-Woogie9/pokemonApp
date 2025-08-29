package com.example.workmatepokemon.data

interface PokemonRepository {
    suspend fun fetchPokemonsFromNetwork(limit: Int, offset: Int): List<Pokemon>
    suspend fun insertPokemons(pokemons: List<Pokemon>)
    suspend fun fetchPokemonsFromLocalStorage(): List<Pokemon>
//    suspend fun getTotalCount(): Int
}
