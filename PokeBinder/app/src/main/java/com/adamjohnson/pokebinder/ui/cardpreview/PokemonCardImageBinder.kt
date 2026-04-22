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
            imageView.setImageResource(android.R.drawable.ic_menu_report_image)
            imageView.alpha = 0.35f
            imageView.isClickable = false
            imageView.isFocusable = false
            imageView.setOnClickListener(null)
            imageView.contentDescription = imageView.context.getString(R.string.pokemon_card_image)
            return
        }

        imageView.alpha = 1f

        Glide.with(imageView)
            .load(pokemonCard.imageUrl)
            .placeholder(android.R.drawable.ic_menu_report_image)
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