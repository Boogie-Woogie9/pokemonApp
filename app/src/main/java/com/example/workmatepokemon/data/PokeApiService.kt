package com.example.workmatepokemon.data

import android.util.Log
import androidx.compose.ui.text.capitalize
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.jvm.java

class PokeApiService(
    private val client: OkHttpClient,
    private val gson: Gson
) {

    suspend fun getPokemons(limit: Int, offset: Int): List<Pokemon> =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://pokeapi.co/api/v2/pokemon?limit=$limit&offset=$offset")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")

            val body = response.body?.string() ?: throw Exception("Empty body")
            val parsed = gson.fromJson(body, PokemonListResponse::class.java)

            // Загружаем детали каждого покемона параллельно
            parsed.results.map { result ->
                val id = result.url.trimEnd('/').substringAfterLast("/").toInt()
                getPokemonDetail(id, result.name)
            }
        }

    private fun getPokemonDetail(id: Int, name: String): Pokemon {
        val request = Request.Builder()
            .url("https://pokeapi.co/api/v2/pokemon/$id")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")

        val body = response.body?.string() ?: throw Exception("Empty body")
        val json = JSONObject(body)

        // Статы
        val statsArray = json.getJSONArray("stats")
        var hp = 0
        var attack = 0
        var defense = 0
        for (i in 0 until statsArray.length()) {
            val statObj = statsArray.getJSONObject(i)
            val statName = statObj.getJSONObject("stat").getString("name")
            val baseStat = statObj.getInt("base_stat")
            when (statName.lowercase()) {
                "hp" -> hp = baseStat
                "attack" -> attack = baseStat
                "defense" -> defense = baseStat
            }
        }

        // Спрайт
        val spriteUrl = json.getJSONObject("sprites")
            .optString("front_default", "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png")

        // Типы
        val typesArray = json.getJSONArray("types")
        val types = mutableListOf<String>()
        for (i in 0 until typesArray.length()) {
            val typeName = typesArray.getJSONObject(i).getJSONObject("type").getString("name")
            types.add(typeName)
        }

        val pokemon = Pokemon(
            id = id,
            name = name,
            sprites = spriteUrl,
            hp = hp,
            attack = attack,
            defense = defense,
            types = types.joinToString(",")
        )

        Log.d("PokemonDebug", pokemon.toString())
        return pokemon
    }
}
