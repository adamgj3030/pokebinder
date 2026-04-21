package com.adamjohnson.pokebinder.data.model

data class CardFilter(
    val nameQuery: String = "",
    val setQuery: String = "",
    val typeQuery: String = "",
    val cardIdQuery: String = "",
    val subtypeQuery: String = "",
    val rarityQuery: String = "",
    val fullArtOnly: Boolean = false
) {

    fun normalized(): CardFilter {
        return copy(
            nameQuery = nameQuery.trim(),
            setQuery = setQuery.trim(),
            typeQuery = typeQuery.trim(),
            cardIdQuery = cardIdQuery.trim(),
            subtypeQuery = subtypeQuery.trim(),
            rarityQuery = rarityQuery.trim()
        )
    }

    fun hasActiveFilters(): Boolean {
        val normalizedFilter = normalized()
        return normalizedFilter.nameQuery.isNotBlank() ||
                normalizedFilter.setQuery.isNotBlank() ||
                normalizedFilter.typeQuery.isNotBlank() ||
                normalizedFilter.cardIdQuery.isNotBlank() ||
                normalizedFilter.subtypeQuery.isNotBlank() ||
                normalizedFilter.rarityQuery.isNotBlank() ||
                normalizedFilter.fullArtOnly
    }
}