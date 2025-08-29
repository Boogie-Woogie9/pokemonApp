package com.example.workmatepokemon.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pokemon")
//@TypeConverters(Converters::class)
data class Pokemon (
    @PrimaryKey val id: Int,
    var name: String,
    var sprites: String?,
    var hp: Int,
    var attack: Int,
    var defense: Int,
    var types: String?
){
    override fun toString(): String {
        return "Pokemon(id=$id, name=$name, hp=$hp, attack=$attack, defense=$defense, types=$types, sprites=$sprites)"
    }
}