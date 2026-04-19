package com.adamjohnson.pokebinder.ui.drafts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.BinderPage
import java.text.DateFormat
import java.util.Date

class DraftsAdapter(
    private val onDraftClick: (BinderPage) -> Unit
) : RecyclerView.Adapter<DraftsAdapter.DraftViewHolder>() {

    private val draftPages = mutableListOf<BinderPage>()

    fun submitList(newDraftPages: List<BinderPage>) {
        draftPages.clear()
        draftPages.addAll(newDraftPages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraftViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_draft_page, parent, false)
        return DraftViewHolder(view)
    }

    override fun onBindViewHolder(holder: DraftViewHolder, position: Int) {
        holder.bind(draftPages[position], onDraftClick)
    }

    override fun getItemCount(): Int {
        return draftPages.size
    }

    class DraftViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val filledSlotsTextView: TextView = itemView.findViewById(R.id.filledSlotsTextView)
        private val updatedAtTextView: TextView = itemView.findViewById(R.id.updatedAtTextView)

        fun bind(binderPage: BinderPage, onDraftClick: (BinderPage) -> Unit) {
            val context = itemView.context
            val filledSlotCount = binderPage.slots.count { !it.cardId.isNullOrBlank() }
            val title = binderPage.title.ifBlank {
                context.getString(R.string.drafts_item_untitled)
            }

            titleTextView.text = title
            filledSlotsTextView.text = context.getString(
                R.string.drafts_item_filled_slots,
                filledSlotCount
            )

            if (binderPage.updatedAt > 0L) {
                val formattedUpdatedAt = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.SHORT
                ).format(Date(binderPage.updatedAt))

                updatedAtTextView.text = context.getString(
                    R.string.drafts_item_updated_at,
                    formattedUpdatedAt
                )
                updatedAtTextView.visibility = View.VISIBLE
            } else {
                updatedAtTextView.text = ""
                updatedAtTextView.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onDraftClick(binderPage)
            }
        }
    }
}