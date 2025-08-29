package com.example.workmatepokemon.data

import android.util.Log
import com.example.workmatepokemon.data.room.PokemonDAO

class PokemonRepositoryImpl(
    private val dao: PokemonDAO,
    private val api: PokeApiService
) : PokemonRepository {

    override suspend fun fetchPokemonsFromNetwork(
        limit: Int,
        offset: Int
    ): List<Pokemon> {
        val apiResult = api.getPokemons(limit, offset)
        insertPokemons(apiResult)
        apiResult.forEach { pokemon ->
            Log.d("FETCH", "$pokemon")
        }
        return apiResult
    }

    override suspend fun insertPokemons(pokemons: List<Pokemon>) {
        dao.insertAll(pokemons.toTypedArray())
        pokemons.forEach { pokemon ->
            Log.d("INSERT", "$pokemon")
        }
    }

    override suspend fun fetchPokemonsFromLocalStorage(): List<Pokemon> {
        val pokemons = dao.getAll()
        pokemons.forEach { pokemon ->
            Log.d("ROOM", "Fetch from Room: $pokemon")
        }
        return pokemons
    }

}
