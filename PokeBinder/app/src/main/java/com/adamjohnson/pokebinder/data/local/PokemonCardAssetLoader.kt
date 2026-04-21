package com.adamjohnson.pokebinder.data.local

import android.content.Context
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PokemonCardAssetLoader(
    private val context: Context,
    private val gson: Gson = Gson()
) {

    fun loadPokemonCards(): List<PokemonCard> {
        val json = context.assets.open(ASSET_FILE_NAME).bufferedReader().use { reader ->
            reader.readText()
        }

        val listType = object : TypeToken<List<PokemonCardAssetDto>>() {}.type
        val rawCards: List<PokemonCardAssetDto> = gson.fromJson(json, listType) ?: emptyList()

        return rawCards.map { rawCard ->
            PokemonCard(
                cardId = rawCard.cardId.orEmpty(),
                name = rawCard.name.orEmpty(),
                imageUrl = rawCard.imageUrl.orEmpty(),
                setName = rawCard.setName.orEmpty(),
                types = sanitizeList(rawCard.types),
                subtypes = sanitizeList(rawCard.subtypes),
                rarity = rawCard.rarity.orEmpty()
            )
        }
    }

    private fun sanitizeList(values: List<String>?): List<String> {
        return values.orEmpty()
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private data class PokemonCardAssetDto(
        val cardId: String? = null,
        val name: String? = null,
        val imageUrl: String? = null,
        val setName: String? = null,
        val types: List<String>? = null,
        val subtypes: List<String>? = null,
        val rarity: String? = null
    )

    companion object {
        private const val ASSET_FILE_NAME = "pokemon_cards.json"
    }
}