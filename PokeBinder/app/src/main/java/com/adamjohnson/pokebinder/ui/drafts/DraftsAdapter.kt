package com.adamjohnson.pokebinder.ui.drafts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.BinderPageStatus
import com.adamjohnson.pokebinder.data.repository.CardRepository
import com.bumptech.glide.Glide
import java.text.DateFormat
import java.util.Date

class DraftsAdapter(
    private val cardRepository: CardRepository,
    private val onCreateNewClick: () -> Unit,
    private val onBinderClick: (BinderPage) -> Unit,
    private val onDeleteClick: (BinderPage) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val binderPages = mutableListOf<BinderPage>()

    fun submitList(newBinderPages: List<BinderPage>) {
        binderPages.clear()
        binderPages.addAll(newBinderPages)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return binderPages.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            VIEW_TYPE_CREATE_NEW
        } else {
            VIEW_TYPE_BINDER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_CREATE_NEW) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_create_new_binder, parent, false)
            CreateNewViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_draft_page, parent, false)
            BinderViewHolder(view, cardRepository)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CreateNewViewHolder) {
            holder.bind(onCreateNewClick)
        } else if (holder is BinderViewHolder) {
            holder.bind(binderPages[position - 1], onBinderClick, onDeleteClick)
        }
    }

    class CreateNewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(onCreateNewClick: () -> Unit) {
            itemView.setOnClickListener {
                onCreateNewClick()
            }
        }
    }

    class BinderViewHolder(
        itemView: View,
        private val cardRepository: CardRepository
    ) : RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        private val metaTextView: TextView = itemView.findViewById(R.id.metaTextView)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

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

        fun bind(
            binderPage: BinderPage,
            onBinderClick: (BinderPage) -> Unit,
            onDeleteClick: (BinderPage) -> Unit
        ) {
            val context = itemView.context
            val title = binderPage.title.ifBlank {
                context.getString(R.string.binders_item_untitled)
            }

            titleTextView.text = title
            statusTextView.text = if (binderPage.status == BinderPageStatus.PUBLISHED) {
                context.getString(R.string.binders_item_status_published)
            } else {
                context.getString(R.string.binders_item_status_draft)
            }

            if (binderPage.status == BinderPageStatus.PUBLISHED) {
                metaTextView.text = context.getString(
                    R.string.binders_item_social_summary,
                    binderPage.likeCount,
                    binderPage.commentCount
                )
                metaTextView.visibility = View.VISIBLE
            } else if (binderPage.updatedAt > 0L) {
                val formattedUpdatedAt = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.SHORT
                ).format(Date(binderPage.updatedAt))

                metaTextView.text = context.getString(
                    R.string.binders_item_updated_at,
                    formattedUpdatedAt
                )
                metaTextView.visibility = View.VISIBLE
            } else {
                metaTextView.text = ""
                metaTextView.visibility = View.GONE
            }

            renderPreviewImages(binderPage)

            val canOpenBinder = binderPage.pageId.isNotBlank()
            itemView.isEnabled = canOpenBinder
            itemView.isClickable = canOpenBinder
            itemView.isFocusable = canOpenBinder
            deleteButton.isEnabled = canOpenBinder

            if (canOpenBinder) {
                itemView.setOnClickListener {
                    onBinderClick(binderPage)
                }

                deleteButton.setOnClickListener {
                    onDeleteClick(binderPage)
                }
            } else {
                itemView.setOnClickListener(null)
                deleteButton.setOnClickListener(null)
            }
        }

        private fun renderPreviewImages(binderPage: BinderPage) {
            slotImageViews.forEachIndexed { index, imageView ->
                val cardId = binderPage.slots.getOrNull(index)?.cardId
                bindCardImage(imageView, cardId)
            }
        }

        private fun bindCardImage(imageView: ImageView, cardId: String?) {
            val pokemonCard = if (cardId.isNullOrBlank()) {
                null
            } else {
                cardRepository.getPokemonCard(cardId)
            }

            if (pokemonCard == null) {
                Glide.with(itemView).clear(imageView)
                imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                imageView.alpha = 0.35f
            } else {
                imageView.alpha = 1f
                Glide.with(itemView)
                    .load(pokemonCard.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imageView)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_CREATE_NEW = 0
        private const val VIEW_TYPE_BINDER = 1
    }
}