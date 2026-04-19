package com.adamjohnson.pokebinder.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.BinderPage
import java.text.DateFormat
import java.util.Date

class FeedAdapter(
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
        return FeedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FeedViewHolder, position: Int) {
        holder.bind(publishedPages[position], onPageClick)
    }

    override fun getItemCount(): Int {
        return publishedPages.size
    }

    class FeedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val filledSlotsTextView: TextView = itemView.findViewById(R.id.filledSlotsTextView)
        private val socialInfoTextView: TextView = itemView.findViewById(R.id.socialInfoTextView)
        private val publishedAtTextView: TextView = itemView.findViewById(R.id.publishedAtTextView)

        fun bind(binderPage: BinderPage, onPageClick: (BinderPage) -> Unit) {
            val context = itemView.context
            val filledSlotCount = binderPage.slots.count { !it.cardId.isNullOrBlank() }
            val title = binderPage.title.ifBlank {
                context.getString(R.string.feed_item_untitled)
            }

            titleTextView.text = title
            filledSlotsTextView.text = context.getString(
                R.string.feed_item_filled_slots,
                filledSlotCount
            )
            socialInfoTextView.text = context.getString(
                R.string.feed_item_social_summary,
                binderPage.likeCount,
                binderPage.commentCount
            )

            if ((binderPage.publishedAt ?: 0L) > 0L) {
                val formattedPublishedAt = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.SHORT
                ).format(Date(binderPage.publishedAt ?: 0L))

                publishedAtTextView.text = context.getString(
                    R.string.feed_item_published_at,
                    formattedPublishedAt
                )
                publishedAtTextView.visibility = View.VISIBLE
            } else {
                publishedAtTextView.text = ""
                publishedAtTextView.visibility = View.GONE
            }

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
    }
}