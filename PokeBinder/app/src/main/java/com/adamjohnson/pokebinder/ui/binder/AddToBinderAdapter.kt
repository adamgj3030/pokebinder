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

class AddToBinderAdapter(
    private val onCardClick: (PokemonCard) -> Unit
) : RecyclerView.Adapter<AddToBinderAdapter.AddToBinderViewHolder>() {

    private val cards = mutableListOf<PokemonCard>()
    private var selectedCardIds: Set<String> = emptySet()

    fun submitData(
        newCards: List<PokemonCard>,
        newSelectedCardIds: Set<String>
    ) {
        cards.clear()
        cards.addAll(newCards)
        selectedCardIds = newSelectedCardIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddToBinderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_add_to_binder_card, parent, false)
        return AddToBinderViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddToBinderViewHolder, position: Int) {
        val card = cards[position]
        holder.bind(
            card = card,
            isSelected = selectedCardIds.contains(card.cardId),
            onCardClick = onCardClick
        )
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    class AddToBinderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardImageView: ImageView = itemView.findViewById(R.id.cardImageView)
        private val cardNameTextView: TextView = itemView.findViewById(R.id.cardNameTextView)
        private val setNameTextView: TextView = itemView.findViewById(R.id.setNameTextView)
        private val typesTextView: TextView = itemView.findViewById(R.id.typesTextView)
        private val selectionOverlayView: View = itemView.findViewById(R.id.selectionOverlayView)
        private val removeSelectionTextView: TextView =
            itemView.findViewById(R.id.removeSelectionTextView)

        fun bind(
            card: PokemonCard,
            isSelected: Boolean,
            onCardClick: (PokemonCard) -> Unit
        ) {
            cardNameTextView.text = card.name
            setNameTextView.text = card.setName

            val typesText = if (card.types.isEmpty()) {
                "No type"
            } else {
                "Type: ${card.types.joinToString(", ")}"
            }

            typesTextView.text = typesText

            Glide.with(itemView)
                .load(card.imageUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_report_image)
                .into(cardImageView)

            selectionOverlayView.visibility = if (isSelected) View.VISIBLE else View.GONE
            removeSelectionTextView.visibility = if (isSelected) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onCardClick(card)
            }

            removeSelectionTextView.setOnClickListener {
                onCardClick(card)
            }
        }
    }
}