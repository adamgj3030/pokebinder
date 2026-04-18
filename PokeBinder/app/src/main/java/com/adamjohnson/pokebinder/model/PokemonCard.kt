package com.adamjohnson.pokebinder.data.model

data class PokemonCard(
    val cardId: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val setName: String = "",
    val types: List<String> = emptyList()
)