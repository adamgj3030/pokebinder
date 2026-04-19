package com.adamjohnson.pokebinder.ui.cardsearch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.bumptech.glide.Glide

class CardSearchAdapter(
    private val onSaveClick: (PokemonCard) -> Unit,
    private val onCardClick: ((PokemonCard) -> Unit)? = null
) : RecyclerView.Adapter<CardSearchAdapter.CardViewHolder>() {

    private val cards = mutableListOf<PokemonCard>()
    private var savedCardIds = emptySet<String>()

    fun submitList(
        newCards: List<PokemonCard>,
        newSavedCardIds: Set<String>
    ) {
        cards.clear()
        cards.addAll(newCards)
        savedCardIds = newSavedCardIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_search_result, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val card = cards[position]
        val isSaved = savedCardIds.contains(card.cardId)
        holder.bind(card, isSaved, onSaveClick, onCardClick)
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardImageView: ImageView = itemView.findViewById(R.id.cardImageView)
        private val cardNameTextView: TextView = itemView.findViewById(R.id.cardNameTextView)
        private val setNameTextView: TextView = itemView.findViewById(R.id.setNameTextView)
        private val typesTextView: TextView = itemView.findViewById(R.id.typesTextView)
        private val saveButton: Button = itemView.findViewById(R.id.saveButton)

        fun bind(
            card: PokemonCard,
            isSaved: Boolean,
            onSaveClick: (PokemonCard) -> Unit,
            onCardClick: ((PokemonCard) -> Unit)?
        ) {
            cardNameTextView.text = card.name
            setNameTextView.text = card.setName

            val typesText = if (card.types.isEmpty()) {
                itemView.context.getString(R.string.card_types_placeholder)
            } else {
                itemView.context.getString(
                    R.string.card_types_label,
                    card.types.joinToString(", ")
                )
            }

            typesTextView.text = typesText

            Glide.with(itemView)
                .load(card.imageUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_report_image)
                .into(cardImageView)

            saveButton.text = if (isSaved) {
                itemView.context.getString(R.string.saved_in_collection)
            } else {
                itemView.context.getString(R.string.save_to_collection)
            }

            saveButton.isEnabled = !isSaved

            if (isSaved) {
                saveButton.setOnClickListener(null)
            } else {
                saveButton.setOnClickListener {
                    onSaveClick(card)
                }
            }

            if (onCardClick != null) {
                itemView.setOnClickListener {
                    onCardClick(card)
                }
                itemView.isClickable = true
                itemView.isFocusable = true
            } else {
                itemView.setOnClickListener(null)
                itemView.isClickable = false
                itemView.isFocusable = false
            }
        }
    }
}