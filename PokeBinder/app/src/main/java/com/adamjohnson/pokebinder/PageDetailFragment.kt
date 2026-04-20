package com.adamjohnson.pokebinder.ui.detail

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
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
import com.bumptech.glide.Glide
import java.text.DateFormat
import java.util.Date

class PageDetailFragment : Fragment(R.layout.fragment_page_detail) {

    private val pageDetailViewModel: PageDetailViewModel by viewModels()

    private val cardRepository by lazy {
        CardRepository(PokemonCardLocalDataSource(requireContext().applicationContext))
    }

    private lateinit var slotImageViews: List<ImageView>
    private lateinit var commentsAdapter: CommentsAdapter

    private lateinit var contentScrollView: View
    private lateinit var pageTitleTextView: TextView
    private lateinit var creatorUsernameTextView: TextView
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

    private var shouldRefreshOnNextResume = false
    private var suppressCommentTextWatcher = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contentScrollView = view.findViewById(R.id.contentScrollView)
        pageTitleTextView = view.findViewById(R.id.pageTitleTextView)
        creatorUsernameTextView = view.findViewById(R.id.creatorUsernameTextView)
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

        slotImageViews = listOf(
            view.findViewById(R.id.slot1ImageView),
            view.findViewById(R.id.slot2ImageView),
            view.findViewById(R.id.slot3ImageView),
            view.findViewById(R.id.slot4ImageView),
            view.findViewById(R.id.slot5ImageView),
            view.findViewById(R.id.slot6ImageView),
            view.findViewById(R.id.slot7ImageView),
            view.findViewById(R.id.slot8ImageView),
            view.findViewById(R.id.slot9ImageView)
        )

        commentsAdapter = CommentsAdapter()
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        commentsRecyclerView.adapter = commentsAdapter

        likeButton.setOnClickListener {
            pageDetailViewModel.toggleLike()
        }

        commentEditText.doAfterTextChanged {
            if (suppressCommentTextWatcher) {
                return@doAfterTextChanged
            }

            if (commentEditText.error != null) {
                commentEditText.error = null
            }

            pageDetailViewModel.clearCommentSubmissionState()
            renderCommentControls()
            renderCommentSubmissionMessage(pageDetailViewModel.commentSubmissionState.value)
        }

        submitCommentButton.setOnClickListener {
            val commentText = commentEditText.text.toString().trim()

            if (commentText.isBlank()) {
                commentEditText.error = getString(R.string.page_detail_comment_required)
                renderCommentControls()
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

        pageDetailViewModel.userSignedInState.observe(viewLifecycleOwner) {
            renderInteractionControls()
        }

        pageDetailViewModel.commentSubmissionState.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) {
                commentEditText.error = null
                clearCommentInput()
            }

            renderCommentControls()
            renderCommentSubmissionMessage(state)
        }

        pageDetailViewModel.socialMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                pageDetailViewModel.clearSocialMessage()
            }
        }

        shouldRefreshOnNextResume = false
        renderInteractionControls()

        val pageId = arguments?.getString(ARG_PAGE_ID).orEmpty()
        pageDetailViewModel.loadPage(pageId)
    }

    override fun onResume() {
        super.onResume()

        if (shouldRefreshOnNextResume) {
            commentEditText.error = null
            pageDetailViewModel.clearCommentSubmissionState()
            pageDetailViewModel.refreshCurrentPage()
        } else {
            shouldRefreshOnNextResume = true
        }
    }

    private fun renderLoading() {
        contentScrollView.visibility = View.GONE
        pageMessageTextView.visibility = View.GONE
        loadingView.visibility = View.VISIBLE
        renderInteractionControls()
    }

    private fun renderSuccess(binderPage: BinderPage) {
        loadingView.visibility = View.GONE
        contentScrollView.visibility = View.VISIBLE
        pageMessageTextView.visibility = View.GONE

        pageTitleTextView.text = binderPage.title.ifBlank {
            getString(R.string.page_detail_untitled)
        }

        creatorUsernameTextView.text = getString(
            R.string.page_detail_by_username,
            binderPage.creatorUsername.trim().ifBlank {
                getString(R.string.page_detail_unknown_user)
            }
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

        renderSlotImages(binderPage)
        renderInteractionControls()
    }

    private fun renderError(message: String) {
        loadingView.visibility = View.GONE
        contentScrollView.visibility = View.GONE
        pageMessageTextView.text = message
        pageMessageTextView.visibility = View.VISIBLE
        renderSlotImages(null)
        renderInteractionControls()
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

    private fun renderInteractionControls() {
        renderLikeButton()
        renderCommentControls()
        renderCommentSubmissionMessage(pageDetailViewModel.commentSubmissionState.value)
    }

    private fun renderLikeButton() {
        val isPageVisible = contentScrollView.visibility == View.VISIBLE
        val isSignedIn = pageDetailViewModel.userSignedInState.value == true
        val isLiked = pageDetailViewModel.isLiked.value == true
        val isLikeLoading = pageDetailViewModel.isLikeLoading.value == true

        likeButton.text = when {
            !isSignedIn -> getString(R.string.page_detail_sign_in_to_like)
            isLikeLoading -> getString(R.string.page_detail_like_loading)
            isLiked -> getString(R.string.page_detail_unlike)
            else -> getString(R.string.page_detail_like)
        }

        likeButton.isEnabled = isPageVisible && isSignedIn && !isLikeLoading
    }

    private fun renderCommentControls() {
        val isPageVisible = contentScrollView.visibility == View.VISIBLE
        val isSignedIn = pageDetailViewModel.userSignedInState.value == true
        val isSubmitting = pageDetailViewModel.commentSubmissionState.value == UiState.Loading
        val hasCommentText = commentEditText.text.toString().trim().isNotBlank()

        commentEditText.hint = if (isSignedIn) {
            getString(R.string.page_detail_comment_hint)
        } else {
            getString(R.string.page_detail_comment_sign_in_hint)
        }

        commentEditText.isEnabled = isPageVisible && isSignedIn && !isSubmitting

        submitCommentButton.text = when {
            isSubmitting -> getString(R.string.page_detail_comment_submit_loading)
            !isSignedIn -> getString(R.string.page_detail_sign_in_to_comment)
            else -> getString(R.string.page_detail_submit_comment)
        }

        submitCommentButton.isEnabled =
            isPageVisible && isSignedIn && !isSubmitting && hasCommentText
    }

    private fun renderCommentSubmissionMessage(state: UiState<Comment>?) {
        val isPageVisible = contentScrollView.visibility == View.VISIBLE
        val isSignedIn = pageDetailViewModel.userSignedInState.value == true

        if (!isPageVisible) {
            commentSubmissionMessageTextView.visibility = View.GONE
            return
        }

        when {
            !isSignedIn -> {
                commentSubmissionMessageTextView.text =
                    getString(R.string.page_detail_sign_in_social_message)
                commentSubmissionMessageTextView.visibility = View.VISIBLE
            }

            state == null -> {
                commentSubmissionMessageTextView.visibility = View.GONE
            }

            state == UiState.Loading -> {
                commentSubmissionMessageTextView.text =
                    getString(R.string.page_detail_comment_submit_loading)
                commentSubmissionMessageTextView.visibility = View.VISIBLE
            }

            state is UiState.Success -> {
                commentSubmissionMessageTextView.text =
                    getString(R.string.page_detail_comment_submit_success)
                commentSubmissionMessageTextView.visibility = View.VISIBLE
            }

            state is UiState.Error -> {
                commentSubmissionMessageTextView.text = state.message
                commentSubmissionMessageTextView.visibility = View.VISIBLE
            }
        }
    }

    private fun clearCommentInput() {
        suppressCommentTextWatcher = true
        commentEditText.setText("")
        suppressCommentTextWatcher = false
    }

    private fun renderSlotImages(binderPage: BinderPage?) {
        slotImageViews.forEachIndexed { index, imageView ->
            val cardId = binderPage?.slots?.getOrNull(index)?.cardId
            bindSlotImage(imageView, cardId)
        }
    }

    private fun bindSlotImage(imageView: ImageView, cardId: String?) {
        val pokemonCard = if (cardId.isNullOrBlank()) {
            null
        } else {
            cardRepository.getPokemonCard(cardId)
        }

        if (pokemonCard == null) {
            Glide.with(this).clear(imageView)
            imageView.setImageResource(android.R.drawable.ic_menu_report_image)
            imageView.alpha = 0.35f
        } else {
            imageView.alpha = 1f
            Glide.with(this)
                .load(pokemonCard.imageUrl)
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_report_image)
                .into(imageView)
        }
    }

    companion object {
        const val ARG_PAGE_ID = "pageId"
    }
}