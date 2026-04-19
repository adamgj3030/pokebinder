package com.adamjohnson.pokebinder.ui.collection

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

class CollectionAdapter(
    private val onRemoveClick: (PokemonCard) -> Unit
) : RecyclerView.Adapter<CollectionAdapter.CollectionViewHolder>() {

    private val cards = mutableListOf<PokemonCard>()

    fun submitList(newCards: List<PokemonCard>) {
        cards.clear()
        cards.addAll(newCards)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection_card, parent, false)
        return CollectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        holder.bind(cards[position], onRemoveClick)
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    class CollectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardImageView: ImageView = itemView.findViewById(R.id.cardImageView)
        private val cardNameTextView: TextView = itemView.findViewById(R.id.cardNameTextView)
        private val setNameTextView: TextView = itemView.findViewById(R.id.setNameTextView)
        private val typesTextView: TextView = itemView.findViewById(R.id.typesTextView)
        private val removeButton: Button = itemView.findViewById(R.id.removeButton)

        fun bind(card: PokemonCard, onRemoveClick: (PokemonCard) -> Unit) {
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

            removeButton.setOnClickListener {
                onRemoveClick(card)
            }
        }
    }
}