package com.example.workmatepokemon.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.workmatepokemon.data.Pokemon

@Dao
interface PokemonDAO {

    @Query("SELECT * FROM pokemon")
    suspend fun getAll(): List<Pokemon>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pokemons: Array<Pokemon>)

    @Query("DELETE FROM pokemon")
    suspend fun clearAll()

    //    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insert(pokemon: Pokemon)
}

