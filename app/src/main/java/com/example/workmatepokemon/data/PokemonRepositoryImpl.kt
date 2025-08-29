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
        return try {
            val apiResult = api.getPokemons(limit, offset)
            insertPokemons(apiResult) // кешируем в Room
            apiResult
        } catch (e: Exception) {
            Log.e("FETCH", "Network error, fallback to Room", e)
            // если нет сети → берём локально
            dao.getAll()
        }
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
