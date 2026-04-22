package com.adamjohnson.pokebinder.ui.collection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.bumptech.glide.Glide

class CollectionAdapter(
    private val onCardClick: (PokemonCard) -> Unit,
    private val onCardLongClick: (PokemonCard) -> Unit,
    private val onDeleteClick: (PokemonCard) -> Unit
) : RecyclerView.Adapter<CollectionAdapter.CollectionViewHolder>() {

    private val cards = mutableListOf<PokemonCard>()
    private var deleteModeEnabled = false

    fun submitData(
        newCards: List<PokemonCard>,
        deleteModeEnabled: Boolean
    ) {
        cards.clear()
        cards.addAll(newCards)
        this.deleteModeEnabled = deleteModeEnabled
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collection_card, parent, false)
        return CollectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        holder.bind(cards[position], deleteModeEnabled, onCardClick, onCardLongClick, onDeleteClick)
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    class CollectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val cardImageView: ImageView = itemView.findViewById(R.id.cardImageView)
        private val deleteBadgeTextView: TextView = itemView.findViewById(R.id.deleteBadgeTextView)

        fun bind(
            card: PokemonCard,
            deleteModeEnabled: Boolean,
            onCardClick: (PokemonCard) -> Unit,
            onCardLongClick: (PokemonCard) -> Unit,
            onDeleteClick: (PokemonCard) -> Unit
        ) {
            Glide.with(itemView)
                .load(card.imageUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_report_image)
                .into(cardImageView)

            deleteBadgeTextView.visibility = if (deleteModeEnabled) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onCardClick(card)
            }

            itemView.setOnLongClickListener {
                onCardLongClick(card)
                true
            }

            deleteBadgeTextView.setOnClickListener {
                onDeleteClick(card)
            }
        }
    }
}