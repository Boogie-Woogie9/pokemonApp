package com.example.workmatepokemon.config

import android.app.Application
import androidx.room.Room
import com.example.workmatepokemon.data.PokeApiService
import com.example.workmatepokemon.data.PokemonRepository
import com.example.workmatepokemon.data.PokemonRepositoryImpl
import com.example.workmatepokemon.data.room.AppDatabase
import com.google.gson.Gson
import okhttp3.OkHttpClient

class PokemonApp : Application() {
    lateinit var db: AppDatabase
    lateinit var repository: PokemonRepository

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "pokemon-db"
        ).build()

        val api = PokeApiService(OkHttpClient(), Gson())

        repository = PokemonRepositoryImpl(db.pokemonDao(), api)
    }

}