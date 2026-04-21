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
    private val onCardClick: (PokemonCard) -> Unit,
    private val onCardLongClick: (PokemonCard) -> Unit
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
            onCardClick = onCardClick,
            onCardLongClick = onCardLongClick
        )
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    class AddToBinderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardImageView: ImageView = itemView.findViewById(R.id.cardImageView)
        private val cardNameTextView: TextView = itemView.findViewById(R.id.cardNameTextView)
        private val selectionOverlayView: View = itemView.findViewById(R.id.selectionOverlayView)
        private val removeSelectionTextView: TextView =
            itemView.findViewById(R.id.removeSelectionTextView)

        fun bind(
            card: PokemonCard,
            isSelected: Boolean,
            onCardClick: (PokemonCard) -> Unit,
            onCardLongClick: (PokemonCard) -> Unit
        ) {
            cardNameTextView.text = card.name.ifBlank { card.cardId }

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

            itemView.setOnLongClickListener {
                onCardLongClick(card)
                true
            }

            removeSelectionTextView.setOnClickListener {
                onCardClick(card)
            }
        }
    }
}
