package com.adamjohnson.pokebinder.data.repository

import com.adamjohnson.pokebinder.data.local.PokemonCardLocalDataSource
import com.adamjohnson.pokebinder.data.model.PokemonCard

class CardRepository(
    private val localDataSource: PokemonCardLocalDataSource
) {

    private var cachedCards: List<PokemonCard>? = null

    fun getPokemonCards(): List<PokemonCard> {
        val currentCards = cachedCards
        if (currentCards != null) {
            return currentCards
        }

        val loadedCards = localDataSource.getPokemonCards()
        cachedCards = loadedCards
        return loadedCards
    }

    fun searchPokemonCards(query: String): List<PokemonCard> {
        val trimmedQuery = query.trim()

        if (trimmedQuery.isBlank()) {
            return getPokemonCards()
        }

        return getPokemonCards().filter { card ->
            card.name.contains(trimmedQuery, ignoreCase = true) ||
                    card.setName.contains(trimmedQuery, ignoreCase = true) ||
                    card.cardId.contains(trimmedQuery, ignoreCase = true) ||
                    card.types.any { type ->
                        type.contains(trimmedQuery, ignoreCase = true)
                    }
        }
    }

    fun getPokemonCard(cardId: String): PokemonCard? {
        return getPokemonCards().find { card ->
            card.cardId == cardId
        }
    }
}