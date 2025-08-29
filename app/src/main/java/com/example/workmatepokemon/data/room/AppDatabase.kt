package com.example.workmatepokemon.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.workmatepokemon.data.Pokemon

@Database(entities = [Pokemon::class], version = 1, exportSchema = false)
//@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pokemonDao() : PokemonDAO
}