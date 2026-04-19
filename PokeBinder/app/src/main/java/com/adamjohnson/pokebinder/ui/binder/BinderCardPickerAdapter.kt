package com.adamjohnson.pokebinder.ui.binder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.bumptech.glide.Glide

class BinderCardPickerAdapter(
    private val onCardClick: (PokemonCard) -> Unit
) : RecyclerView.Adapter<BinderCardPickerAdapter.BinderCardViewHolder>() {

    private val cards = mutableListOf<PokemonCard>()

    fun submitList(newCards: List<PokemonCard>) {
        cards.clear()
        cards.addAll(newCards)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BinderCardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_binder_picker_card, parent, false)
        return BinderCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: BinderCardViewHolder, position: Int) {
        holder.bind(cards[position], onCardClick)
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    class BinderCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardImageView: ImageView = itemView.findViewById(R.id.cardImageView)
        private val cardNameTextView: TextView = itemView.findViewById(R.id.cardNameTextView)
        private val setNameTextView: TextView = itemView.findViewById(R.id.setNameTextView)
        private val typesTextView: TextView = itemView.findViewById(R.id.typesTextView)

        fun bind(card: PokemonCard, onCardClick: (PokemonCard) -> Unit) {
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

            itemView.setOnClickListener {
                onCardClick(card)
            }
        }
    }
}