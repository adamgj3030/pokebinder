package com.adamjohnson.pokebinder.ui.cardsearch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.bumptech.glide.Glide

class CardSearchAdapter(
    private val onSaveToggleClick: (PokemonCard) -> Unit,
    private val onCardLongClick: ((PokemonCard) -> Unit)? = null
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
        holder.bind(card, isSaved, onSaveToggleClick, onCardLongClick)
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    class CardSearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardImageView: ImageView = itemView.findViewById(R.id.cardImageView)
        private val savedOverlayView: View = itemView.findViewById(R.id.savedOverlayView)
        private val savedBadgeTextView: TextView = itemView.findViewById(R.id.savedBadgeTextView)

        fun bind(
            card: PokemonCard,
            isSaved: Boolean,
            onSaveToggleClick: (PokemonCard) -> Unit,
            onCardLongClick: ((PokemonCard) -> Unit)?
        ) {
            Glide.with(itemView)
                .load(card.imageUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_report_image)
                .into(cardImageView)

            savedOverlayView.visibility = if (isSaved) View.VISIBLE else View.GONE
            savedBadgeTextView.visibility = if (isSaved) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onSaveToggleClick(card)
            }

            itemView.setOnLongClickListener {
                onCardLongClick?.invoke(card)
                onCardLongClick != null
            }
        }
    }
}