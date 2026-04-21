package com.adamjohnson.pokebinder.data.repository

import com.adamjohnson.pokebinder.data.local.PokemonCardLocalDataSource
import com.adamjohnson.pokebinder.data.model.CardFilter
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
                    card.rarity.contains(trimmedQuery, ignoreCase = true) ||
                    card.types.any { type ->
                        type.contains(trimmedQuery, ignoreCase = true)
                    } ||
                    card.subtypes.any { subtype ->
                        subtype.contains(trimmedQuery, ignoreCase = true)
                    }
        }
    }

    fun filterPokemonCards(
        filter: CardFilter,
        cards: List<PokemonCard> = getPokemonCards()
    ): List<PokemonCard> {
        val normalizedFilter = filter.normalized()

        return cards.filter { card ->
            matchesText(card.name, normalizedFilter.nameQuery) &&
                    matchesText(card.setName, normalizedFilter.setQuery) &&
                    matchesList(card.types, normalizedFilter.typeQuery) &&
                    matchesText(card.cardId, normalizedFilter.cardIdQuery) &&
                    matchesList(card.subtypes, normalizedFilter.subtypeQuery) &&
                    matchesText(card.rarity, normalizedFilter.rarityQuery) &&
                    matchesFullArt(card, normalizedFilter.fullArtOnly)
        }
    }

    fun getNameSuggestions(
        query: String,
        cards: List<PokemonCard> = getPokemonCards(),
        limit: Int = 8
    ): List<String> {
        return getSuggestions(
            query = query,
            values = cards.map { it.name },
            limit = limit
        )
    }

    fun getSetSuggestions(
        query: String,
        cards: List<PokemonCard> = getPokemonCards(),
        limit: Int = 8
    ): List<String> {
        return getSuggestions(
            query = query,
            values = cards.map { it.setName },
            limit = limit
        )
    }

    fun getTypeSuggestions(
        query: String,
        cards: List<PokemonCard> = getPokemonCards(),
        limit: Int = 8
    ): List<String> {
        return getSuggestions(
            query = query,
            values = cards.flatMap { it.types },
            limit = limit
        )
    }

    fun getSubtypeSuggestions(
        query: String,
        cards: List<PokemonCard> = getPokemonCards(),
        limit: Int = 8
    ): List<String> {
        return getSuggestions(
            query = query,
            values = cards.flatMap { it.subtypes },
            limit = limit
        )
    }

    fun getRaritySuggestions(
        query: String,
        cards: List<PokemonCard> = getPokemonCards(),
        limit: Int = 8
    ): List<String> {
        return getSuggestions(
            query = query,
            values = cards.map { it.rarity },
            limit = limit
        )
    }

    fun getPokemonCard(cardId: String): PokemonCard? {
        return getPokemonCards().find { card ->
            card.cardId == cardId
        }
    }

    private fun matchesText(value: String, query: String): Boolean {
        return query.isBlank() || value.contains(query, ignoreCase = true)
    }

    private fun matchesList(values: List<String>, query: String): Boolean {
        return query.isBlank() || values.any { value ->
            value.contains(query, ignoreCase = true)
        }
    }

    private fun matchesFullArt(card: PokemonCard, fullArtOnly: Boolean): Boolean {
        return !fullArtOnly || isFullArtApproximation(card)
    }

    private fun isFullArtApproximation(card: PokemonCard): Boolean {
        val normalizedRarity = card.rarity.trim().lowercase()

        return normalizedRarity.contains("ultra") ||
                normalizedRarity.contains("rainbow") ||
                normalizedRarity.contains("secret")
    }

    private fun getSuggestions(
        query: String,
        values: List<String>,
        limit: Int
    ): List<String> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return emptyList()
        }

        return values
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .filter { value ->
                value.contains(trimmedQuery, ignoreCase = true)
            }
            .sortedWith(
                compareBy<String> { !it.startsWith(trimmedQuery, ignoreCase = true) }
                    .thenBy { it.lowercase() }
            )
            .take(limit)
    }
}