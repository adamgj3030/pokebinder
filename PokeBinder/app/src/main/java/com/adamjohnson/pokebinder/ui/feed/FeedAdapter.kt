package com.adamjohnson.pokebinder.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.repository.CardRepository
import com.adamjohnson.pokebinder.ui.cardpreview.PokemonCardImageBinder

class FeedAdapter(
    private val cardRepository: CardRepository,
    private val onPageClick: (BinderPage) -> Unit
) : RecyclerView.Adapter<FeedAdapter.FeedViewHolder>() {

    private val publishedPages = mutableListOf<BinderPage>()

    fun submitList(newPublishedPages: List<BinderPage>) {
        publishedPages.clear()
        publishedPages.addAll(newPublishedPages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feed_page, parent, false)
        return FeedViewHolder(view, cardRepository)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(publishedPages[position], onPageClick)
    }

    override fun getItemCount(): Int {
        return publishedPages.size
    }

    class FeedViewHolder(
        itemView: View,
        private val cardRepository: CardRepository
    ) : RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val creatorUsernameTextView: TextView =
            itemView.findViewById(R.id.creatorUsernameTextView)
        private val socialInfoTextView: TextView = itemView.findViewById(R.id.socialInfoTextView)

        private val slotImageViews = listOf(
            itemView.findViewById<ImageView>(R.id.slot1ImageView),
            itemView.findViewById<ImageView>(R.id.slot2ImageView),
            itemView.findViewById<ImageView>(R.id.slot3ImageView),
            itemView.findViewById<ImageView>(R.id.slot4ImageView),
            itemView.findViewById<ImageView>(R.id.slot5ImageView),
            itemView.findViewById<ImageView>(R.id.slot6ImageView),
            itemView.findViewById<ImageView>(R.id.slot7ImageView),
            itemView.findViewById<ImageView>(R.id.slot8ImageView),
            itemView.findViewById<ImageView>(R.id.slot9ImageView)
        )

        fun bind(binderPage: BinderPage, onPageClick: (BinderPage) -> Unit) {
            val context = itemView.context
            val title = binderPage.title.ifBlank {
                context.getString(R.string.feed_item_untitled)
            }
            val creatorUsername = binderPage.creatorUsername.trim().ifBlank {
                context.getString(R.string.feed_item_unknown_user)
            }

            titleTextView.text = title
            creatorUsernameTextView.text = context.getString(
                R.string.feed_item_by_username,
                creatorUsername
            )
            socialInfoTextView.text = context.getString(
                R.string.feed_item_social_summary,
                binderPage.likeCount,
                binderPage.commentCount
            )

            renderPreviewImages(binderPage)

            val canOpenPage = binderPage.pageId.isNotBlank()
            itemView.isEnabled = canOpenPage
            itemView.isClickable = canOpenPage
            itemView.isFocusable = canOpenPage

            if (canOpenPage) {
                itemView.setOnClickListener {
                    onPageClick(binderPage)
                }
            } else {
                itemView.setOnClickListener(null)
            }
        }

        private fun renderPreviewImages(binderPage: BinderPage) {
            slotImageViews.forEachIndexed { index, imageView ->
                val cardId = binderPage.slots.getOrNull(index)?.cardId
                val pokemonCard = if (cardId.isNullOrBlank()) {
                    null
                } else {
                    cardRepository.getPokemonCard(cardId)
                }

                PokemonCardImageBinder.bind(imageView, pokemonCard)
            }
        }
    }
}