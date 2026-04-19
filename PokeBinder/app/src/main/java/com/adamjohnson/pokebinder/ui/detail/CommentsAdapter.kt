package com.adamjohnson.pokebinder.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.Comment
import java.text.DateFormat
import java.util.Date

class CommentsAdapter : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    private val comments = mutableListOf<Comment>()

    fun submitList(newComments: List<Comment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val userDisplayNameTextView: TextView =
            itemView.findViewById(R.id.userDisplayNameTextView)
        private val createdAtTextView: TextView =
            itemView.findViewById(R.id.createdAtTextView)
        private val commentTextView: TextView =
            itemView.findViewById(R.id.commentTextView)

        fun bind(comment: Comment) {
            val context = itemView.context

            userDisplayNameTextView.text = comment.userDisplayName.ifBlank {
                context.getString(R.string.page_detail_comment_user_fallback)
            }

            commentTextView.text = comment.text

            if (comment.createdAt > 0L) {
                val formattedCreatedAt = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM,
                    DateFormat.SHORT
                ).format(Date(comment.createdAt))

                createdAtTextView.text = formattedCreatedAt
                createdAtTextView.visibility = View.VISIBLE
            } else {
                createdAtTextView.text = ""
                createdAtTextView.visibility = View.GONE
            }
        }
    }
}