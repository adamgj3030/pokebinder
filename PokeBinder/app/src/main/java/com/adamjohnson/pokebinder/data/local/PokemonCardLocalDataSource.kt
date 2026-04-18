package com.adamjohnson.pokebinder.data.local

import android.content.Context
import com.adamjohnson.pokebinder.data.model.PokemonCard

class PokemonCardLocalDataSource(
    context: Context
) {
    private val assetLoader = PokemonCardAssetLoader(context)

    fun getPokemonCards(): List<PokemonCard> {
        return assetLoader.loadPokemonCards()
    }
}