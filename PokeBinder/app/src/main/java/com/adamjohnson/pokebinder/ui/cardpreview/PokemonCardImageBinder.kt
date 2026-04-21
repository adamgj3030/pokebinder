package com.adamjohnson.pokebinder.ui.cardpreview

import android.widget.ImageView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.bumptech.glide.Glide

object PokemonCardImageBinder {

    fun bind(
        imageView: ImageView,
        pokemonCard: PokemonCard?,
        onCardClick: ((PokemonCard) -> Unit)? = null
    ) {
        if (pokemonCard == null) {
            Glide.with(imageView).clear(imageView)
            imageView.setImageDrawable(null)
            imageView.isClickable = false
            imageView.isFocusable = false
            imageView.setOnClickListener(null)
            imageView.contentDescription = imageView.context.getString(R.string.pokemon_card_image)
            return
        }

        Glide.with(imageView)
            .load(pokemonCard.imageUrl)
            .error(android.R.drawable.ic_menu_report_image)
            .into(imageView)

        imageView.isClickable = onCardClick != null
        imageView.isFocusable = onCardClick != null
        imageView.setOnClickListener {
            onCardClick?.invoke(pokemonCard)
        }
        imageView.contentDescription = imageView.context.getString(
            R.string.card_preview_card_content_description,
            pokemonCard.name
        )
    }
}