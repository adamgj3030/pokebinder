package com.adamjohnson.pokebinder.ui.cardsearch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.bumptech.glide.Glide

class CardSearchAdapter(
    private val onSaveToggleClick: (PokemonCard) -> Unit,
    private val onCardClick: ((PokemonCard) -> Unit)? = null
) : RecyclerView.Adapter<CardSearchAdapter.CardSearchViewHolder>() {

    private val cards = mutableListOf<PokemonCard>()
    private var savedCardIds: Set<String> = emptySet()

    fun submitList(
        newCards: List<PokemonCard>,
        newSavedCardIds: Set<String>
    ) {
        cards.clear()
        cards.addAll(newCards)
        savedCardIds = newSavedCardIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardSearchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_search_result, parent, false)
        return CardSearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardSearchViewHolder, position: Int) {
        val card = cards[position]
        val isSaved = savedCardIds.contains(card.cardId)
        holder.bind(card, isSaved, onSaveToggleClick, onCardClick)
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    class CardSearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardImageView: ImageView = itemView.findViewById(R.id.cardImageView)
        private val cardNameTextView: TextView = itemView.findViewById(R.id.cardNameTextView)
        private val saveButton: TextView = itemView.findViewById(R.id.saveButton)

        fun bind(
            card: PokemonCard,
            isSaved: Boolean,
            onSaveToggleClick: (PokemonCard) -> Unit,
            onCardClick: ((PokemonCard) -> Unit)?
        ) {
            val context = itemView.context

            cardNameTextView.text = card.name.ifBlank { card.cardId }

            Glide.with(itemView)
                .load(card.imageUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_report_image)
                .into(cardImageView)

            saveButton.text = if (isSaved) {
                context.getString(R.string.saved_in_collection)
            } else {
                context.getString(R.string.save_to_collection)
            }

            saveButton.setBackgroundResource(
                if (isSaved) {
                    R.drawable.bg_cta_primary
                } else {
                    R.drawable.bg_cta_secondary
                }
            )

            saveButton.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isSaved) android.R.color.white else R.color.poke_text
                )
            )

            saveButton.setOnClickListener {
                onSaveToggleClick(card)
            }

            if (onCardClick != null) {
                itemView.setOnClickListener {
                    onCardClick(card)
                }
            } else {
                itemView.setOnClickListener(null)
            }
        }
    }
}