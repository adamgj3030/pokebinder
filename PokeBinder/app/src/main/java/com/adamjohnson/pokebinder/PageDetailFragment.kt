package com.adamjohnson.pokebinder.ui.detail

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.local.PokemonCardLocalDataSource
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.Comment
import com.adamjohnson.pokebinder.data.repository.CardRepository
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.PageDetailViewModel
import java.text.DateFormat
import java.util.Date

class PageDetailFragment : Fragment(R.layout.fragment_page_detail) {

    private val pageDetailViewModel: PageDetailViewModel by viewModels()

    private val cardRepository by lazy {
        CardRepository(PokemonCardLocalDataSource(requireContext().applicationContext))
    }

    private lateinit var slotViews: List<TextView>
    private lateinit var commentsAdapter: CommentsAdapter

    private lateinit var contentScrollView: View
    private lateinit var pageTitleTextView: TextView
    private lateinit var pageStatusTextView: TextView
    private lateinit var filledSlotsTextView: TextView
    private lateinit var publishedAtTextView: TextView
    private lateinit var likeCountTextView: TextView
    private lateinit var commentCountTextView: TextView
    private lateinit var likeButton: Button
    private lateinit var commentEditText: EditText
    private lateinit var submitCommentButton: Button
    private lateinit var commentSubmissionMessageTextView: TextView
    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentsLoadingView: View
    private lateinit var commentsMessageTextView: TextView
    private lateinit var pageMessageTextView: TextView
    private lateinit var loadingView: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contentScrollView = view.findViewById(R.id.contentScrollView)
        pageTitleTextView = view.findViewById(R.id.pageTitleTextView)
        pageStatusTextView = view.findViewById(R.id.pageStatusTextView)
        filledSlotsTextView = view.findViewById(R.id.filledSlotsTextView)
        publishedAtTextView = view.findViewById(R.id.publishedAtTextView)
        likeCountTextView = view.findViewById(R.id.likeCountTextView)
        commentCountTextView = view.findViewById(R.id.commentCountTextView)
        likeButton = view.findViewById(R.id.likeButton)
        commentEditText = view.findViewById(R.id.commentEditText)
        submitCommentButton = view.findViewById(R.id.submitCommentButton)
        commentSubmissionMessageTextView =
            view.findViewById(R.id.commentSubmissionMessageTextView)
        commentsRecyclerView = view.findViewById(R.id.commentsRecyclerView)
        commentsLoadingView = view.findViewById(R.id.commentsLoadingView)
        commentsMessageTextView = view.findViewById(R.id.commentsMessageTextView)
        pageMessageTextView = view.findViewById(R.id.pageMessageTextView)
        loadingView = view.findViewById(R.id.loadingView)

        slotViews = listOf(
            view.findViewById(R.id.slot1TextView),
            view.findViewById(R.id.slot2TextView),
            view.findViewById(R.id.slot3TextView),
            view.findViewById(R.id.slot4TextView),
            view.findViewById(R.id.slot5TextView),
            view.findViewById(R.id.slot6TextView),
            view.findViewById(R.id.slot7TextView),
            view.findViewById(R.id.slot8TextView),
            view.findViewById(R.id.slot9TextView)
        )

        commentsAdapter = CommentsAdapter()
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        commentsRecyclerView.adapter = commentsAdapter

        likeButton.setOnClickListener {
            pageDetailViewModel.toggleLike()
        }

        submitCommentButton.setOnClickListener {
            val commentText = commentEditText.text.toString()

            if (commentText.isBlank()) {
                commentEditText.error = getString(R.string.page_detail_comment_required)
                return@setOnClickListener
            }

            commentEditText.error = null
            pageDetailViewModel.submitComment(commentText)
        }

        pageDetailViewModel.pageState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> renderLoading()
                is UiState.Success -> renderSuccess(state.data)
                is UiState.Error -> renderError(state.message)
            }
        }

        pageDetailViewModel.commentsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> renderCommentsLoading()
                is UiState.Success -> renderCommentsSuccess(state.data)
                is UiState.Error -> renderCommentsError(state.message)
            }
        }

        pageDetailViewModel.isLiked.observe(viewLifecycleOwner) {
            renderLikeButton()
        }

        pageDetailViewModel.isLikeLoading.observe(viewLifecycleOwner) {
            renderLikeButton()
        }

        pageDetailViewModel.commentSubmissionState.observe(viewLifecycleOwner) { state ->
            renderCommentSubmissionState(state)
        }

        pageDetailViewModel.socialMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                pageDetailViewModel.clearSocialMessage()
            }
        }

        val pageId = arguments?.getString(ARG_PAGE_ID).orEmpty()
        pageDetailViewModel.loadPage(pageId)
    }

    private fun renderLoading() {
        contentScrollView.visibility = View.GONE
        pageMessageTextView.visibility = View.GONE
        loadingView.visibility = View.VISIBLE
    }

    private fun renderSuccess(binderPage: BinderPage) {
        loadingView.visibility = View.GONE
        contentScrollView.visibility = View.VISIBLE
        pageMessageTextView.visibility = View.GONE

        pageTitleTextView.text = binderPage.title.ifBlank {
            getString(R.string.page_detail_untitled)
        }

        pageStatusTextView.text = getString(R.string.page_detail_status_published)
        filledSlotsTextView.text = getString(
            R.string.page_detail_filled_slots,
            getFilledSlotCount(binderPage)
        )

        likeCountTextView.text = getString(
            R.string.page_detail_like_count,
            binderPage.likeCount
        )
        commentCountTextView.text = getString(
            R.string.page_detail_comment_count,
            binderPage.commentCount
        )

        val publishedAt = binderPage.publishedAt ?: 0L
        if (publishedAt > 0L) {
            val formattedPublishedAt = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT
            ).format(Date(publishedAt))

            publishedAtTextView.text = getString(
                R.string.page_detail_published_at,
                formattedPublishedAt
            )
            publishedAtTextView.visibility = View.VISIBLE
        } else {
            publishedAtTextView.text = ""
            publishedAtTextView.visibility = View.GONE
        }

        renderSlotViews(binderPage)
        renderLikeButton()
    }

    private fun renderError(message: String) {
        loadingView.visibility = View.GONE
        contentScrollView.visibility = View.GONE
        pageMessageTextView.text = message
        pageMessageTextView.visibility = View.VISIBLE
        renderSlotViews(null)
    }

    private fun renderCommentsLoading() {
        commentsLoadingView.visibility = View.VISIBLE
        commentsMessageTextView.visibility = View.GONE
        commentsRecyclerView.visibility = View.GONE
    }

    private fun renderCommentsSuccess(comments: List<Comment>) {
        commentsLoadingView.visibility = View.GONE

        if (comments.isEmpty()) {
            commentsAdapter.submitList(emptyList())
            commentsRecyclerView.visibility = View.GONE
            commentsMessageTextView.text = getString(R.string.page_detail_comments_empty_message)
            commentsMessageTextView.visibility = View.VISIBLE
        } else {
            commentsAdapter.submitList(comments)
            commentsRecyclerView.visibility = View.VISIBLE
            commentsMessageTextView.visibility = View.GONE
        }
    }

    private fun renderCommentsError(message: String) {
        commentsLoadingView.visibility = View.GONE
        commentsAdapter.submitList(emptyList())
        commentsRecyclerView.visibility = View.GONE
        commentsMessageTextView.text = message
        commentsMessageTextView.visibility = View.VISIBLE
    }

    private fun renderLikeButton() {
        val isLiked = pageDetailViewModel.isLiked.value == true
        val isLikeLoading = pageDetailViewModel.isLikeLoading.value == true

        likeButton.text = when {
            isLikeLoading -> getString(R.string.page_detail_like_loading)
            isLiked -> getString(R.string.page_detail_unlike)
            else -> getString(R.string.page_detail_like)
        }

        likeButton.isEnabled = !isLikeLoading &&
                contentScrollView.visibility == View.VISIBLE &&
                pageDetailViewModel.isUserSignedIn()
    }

    private fun renderCommentSubmissionState(state: UiState<Comment>?) {
        when (state) {
            null -> {
                commentSubmissionMessageTextView.visibility = View.GONE
                commentEditText.isEnabled = pageDetailViewModel.isUserSignedIn()
                submitCommentButton.isEnabled = pageDetailViewModel.isUserSignedIn()
                submitCommentButton.text = getString(R.string.page_detail_submit_comment)
            }

            UiState.Loading -> {
                commentSubmissionMessageTextView.text =
                    getString(R.string.page_detail_comment_submit_loading)
                commentSubmissionMessageTextView.visibility = View.VISIBLE
                commentEditText.isEnabled = false
                submitCommentButton.isEnabled = false
                submitCommentButton.text = getString(R.string.page_detail_comment_submit_loading)
            }

            is UiState.Success -> {
                commentSubmissionMessageTextView.text =
                    getString(R.string.page_detail_comment_submit_success)
                commentSubmissionMessageTextView.visibility = View.VISIBLE
                commentEditText.setText("")
                commentEditText.isEnabled = pageDetailViewModel.isUserSignedIn()
                submitCommentButton.isEnabled = pageDetailViewModel.isUserSignedIn()
                submitCommentButton.text = getString(R.string.page_detail_submit_comment)
            }

            is UiState.Error -> {
                commentSubmissionMessageTextView.text = state.message
                commentSubmissionMessageTextView.visibility = View.VISIBLE
                commentEditText.isEnabled = pageDetailViewModel.isUserSignedIn()
                submitCommentButton.isEnabled = pageDetailViewModel.isUserSignedIn()
                submitCommentButton.text = getString(R.string.page_detail_submit_comment)
            }
        }
    }

    private fun renderSlotViews(binderPage: BinderPage?) {
        slotViews.forEachIndexed { index, slotView ->
            val cardId = binderPage?.slots?.getOrNull(index)?.cardId
            val cardText = getSlotCardDisplayText(cardId)

            slotView.text = getString(R.string.page_detail_slot_label, index + 1, cardText)
            slotView.setBackgroundResource(android.R.color.darker_gray)
        }
    }

    private fun getSlotCardDisplayText(cardId: String?): String {
        if (cardId.isNullOrBlank()) {
            return getString(R.string.page_detail_slot_empty)
        }

        val pokemonCard = cardRepository.getPokemonCard(cardId)
        return if (pokemonCard != null) {
            getString(
                R.string.page_detail_slot_card_info,
                pokemonCard.name,
                pokemonCard.setName
            )
        } else {
            cardId
        }
    }

    private fun getFilledSlotCount(binderPage: BinderPage): Int {
        return binderPage.slots.count { slot ->
            !slot.cardId.isNullOrBlank()
        }
    }

    companion object {
        const val ARG_PAGE_ID = "pageId"
    }
}