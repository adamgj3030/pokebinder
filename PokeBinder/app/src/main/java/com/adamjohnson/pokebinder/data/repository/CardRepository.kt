package com.adamjohnson.pokebinder.data.repository

import com.adamjohnson.pokebinder.data.local.PokemonCardLocalDataSource
import com.adamjohnson.pokebinder.data.model.PokemonCard

class CardRepository(
    private val localDataSource: PokemonCardLocalDataSource
) {

    fun getPokemonCards(): List<PokemonCard> {
        return localDataSource.getPokemonCards()
    }

    fun getPokemonCard(cardId: String): PokemonCard? {
        return localDataSource.getPokemonCards().find { card ->
            card.cardId == cardId
        }
    }
}