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

        val listType = object : TypeToken<List<PokemonCard>>() {}.type
        return gson.fromJson(json, listType) ?: emptyList()
    }

    companion object {
        private const val ASSET_FILE_NAME = "pokemon_cards.json"
    }
}