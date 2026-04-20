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

    fun filterPokemonCards(
        nameQuery: String = "",
        setQuery: String = "",
        typeQuery: String = "",
        cardIdQuery: String = ""
    ): List<PokemonCard> {
        val normalizedNameQuery = nameQuery.trim()
        val normalizedSetQuery = setQuery.trim()
        val normalizedTypeQuery = typeQuery.trim()
        val normalizedCardIdQuery = cardIdQuery.trim()

        return getPokemonCards().filter { card ->
            matchesText(card.name, normalizedNameQuery) &&
                    matchesText(card.setName, normalizedSetQuery) &&
                    matchesText(card.cardId, normalizedCardIdQuery) &&
                    matchesTypes(card.types, normalizedTypeQuery)
        }
    }

    fun getNameSuggestions(query: String, limit: Int = 8): List<String> {
        return getSuggestions(
            query = query,
            values = getPokemonCards().map { it.name },
            limit = limit
        )
    }

    fun getSetSuggestions(query: String, limit: Int = 8): List<String> {
        return getSuggestions(
            query = query,
            values = getPokemonCards().map { it.setName },
            limit = limit
        )
    }

    fun getTypeSuggestions(query: String, limit: Int = 8): List<String> {
        return getSuggestions(
            query = query,
            values = getPokemonCards().flatMap { it.types },
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

    private fun matchesTypes(types: List<String>, query: String): Boolean {
        return query.isBlank() || types.any { type ->
            type.contains(query, ignoreCase = true)
        }
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