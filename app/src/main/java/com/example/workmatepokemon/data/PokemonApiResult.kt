package com.example.workmatepokemon.data

data class PokemonApiResult(
    val name: String,
    val url: String
)
//
//// extension-функция для маппинга в сущность Room
//fun PokemonApiResult.toPokemon(): Pokemon {
//    val id = url.trimEnd('/').substringAfterLast("/").toInt()
//    val spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"
//    return Pokemon(
//        id = id,
//        name = name,
//        sprites = spriteUrl
//    )
//}

