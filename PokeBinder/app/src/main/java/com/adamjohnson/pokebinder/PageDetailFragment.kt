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
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.adamjohnson.pokebinder.data.repository.CardRepository
import com.adamjohnson.pokebinder.ui.cardpreview.PokemonCardImageBinder
import com.adamjohnson.pokebinder.ui.cardpreview.PokemonCardPreviewDialogFragment
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.PageDetailViewModel

class PageDetailFragment : Fragment(R.layout.fragment_page_detail) {

    private val pageDetailViewModel: PageDetailViewModel by viewModels()

    private val cardRepository by lazy {
        CardRepository(PokemonCardLocalDataSource(requireContext().applicationContext))
    }

    private lateinit var slotImageViews: List<ImageView>
    private lateinit var commentsAdapter: CommentsAdapter

    private lateinit var contentScrollView: View
    private lateinit var titleTextView: TextView
    private lateinit var creatorUsernameTextView: TextView
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

    private var suppressCommentTextWatcher = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        contentScrollView = view.findViewById(R.id.contentScrollView)
        titleTextView = view.findViewById(R.id.titleTextView)
        creatorUsernameTextView = view.findViewById(R.id.creatorUsernameTextView)
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

        renderInteractionControls()

        val pageId = arguments?.getString(ARG_PAGE_ID).orEmpty()
        pageDetailViewModel.setPage(pageId)
    }

    override fun onStart() {
        super.onStart()
        pageDetailViewModel.startListening()
    }

    override fun onStop() {
        super.onStop()
        pageDetailViewModel.stopListening()
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

        titleTextView.text = binderPage.title.ifBlank {
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
            commentsMessageTextView.visibility = View.VISIBLE
            commentsMessageTextView.text = getString(R.string.page_detail_comments_empty_message)
        } else {
            commentsAdapter.submitList(comments)
            commentsRecyclerView.visibility = View.VISIBLE
            commentsMessageTextView.visibility = View.GONE
        }
    }

    private fun renderCommentsError(message: String) {
        commentsAdapter.submitList(emptyList())
        commentsRecyclerView.visibility = View.GONE
        commentsLoadingView.visibility = View.GONE
        commentsMessageTextView.visibility = View.VISIBLE
        commentsMessageTextView.text = message
    }

    private fun renderInteractionControls() {
        val isSignedIn = pageDetailViewModel.userSignedInState.value == true

        likeButton.isEnabled = isSignedIn && pageDetailViewModel.isLikeLoading.value != true

        if (isSignedIn) {
            commentEditText.isEnabled = true
            commentEditText.hint = getString(R.string.page_detail_comment_hint)
        } else {
            commentEditText.isEnabled = false
            commentEditText.hint = getString(R.string.page_detail_comment_sign_in_hint)
        }

        renderLikeButton()
        renderCommentControls()
    }

    private fun renderLikeButton() {
        val isSignedIn = pageDetailViewModel.userSignedInState.value == true
        val isLiked = pageDetailViewModel.isLiked.value == true
        val isLikeLoading = pageDetailViewModel.isLikeLoading.value == true

        likeButton.text = when {
            !isSignedIn -> getString(R.string.page_detail_sign_in_to_like)
            isLikeLoading -> getString(R.string.page_detail_like_loading)
            isLiked -> getString(R.string.page_detail_unlike)
            else -> getString(R.string.page_detail_like)
        }

        likeButton.isEnabled = isSignedIn && !isLikeLoading
    }

    private fun renderCommentControls() {
        val isSignedIn = pageDetailViewModel.userSignedInState.value == true
        val submissionState = pageDetailViewModel.commentSubmissionState.value
        val isSubmitting = submissionState == UiState.Loading
        val hasText = commentEditText.text?.toString().orEmpty().trim().isNotBlank()

        submitCommentButton.text = if (isSubmitting) {
            getString(R.string.page_detail_comment_submit_loading)
        } else {
            getString(R.string.page_detail_submit_comment)
        }

        submitCommentButton.isEnabled = isSignedIn && !isSubmitting && hasText
        commentEditText.isEnabled = isSignedIn && !isSubmitting
    }

    private fun renderCommentSubmissionMessage(state: UiState<Comment>?) {
        when (state) {
            null -> {
                commentSubmissionMessageTextView.text = ""
                commentSubmissionMessageTextView.visibility = View.GONE
            }

            UiState.Loading -> {
                commentSubmissionMessageTextView.text =
                    getString(R.string.page_detail_comment_submit_loading)
                commentSubmissionMessageTextView.visibility = View.VISIBLE
            }

            is UiState.Success -> {
                commentSubmissionMessageTextView.text =
                    getString(R.string.page_detail_comment_submit_success)
                commentSubmissionMessageTextView.visibility = View.VISIBLE
            }

            is UiState.Error -> {
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
            val pokemonCard = if (cardId.isNullOrBlank()) {
                null
            } else {
                cardRepository.getPokemonCard(cardId)
            }

            PokemonCardImageBinder.bind(imageView, pokemonCard) { clickedCard ->
                showCardPreview(clickedCard)
            }

            if (pokemonCard == null) {
                imageView.setImageResource(android.R.drawable.ic_menu_report_image)
                imageView.alpha = 0.35f
            } else {
                imageView.alpha = 1f
            }
        }
    }

    private fun showCardPreview(card: PokemonCard) {
        if (!isAdded) {
            return
        }

        if (childFragmentManager.findFragmentByTag(PokemonCardPreviewDialogFragment.TAG) != null) {
            return
        }

        PokemonCardPreviewDialogFragment.newInstance(card.cardId)
            .show(childFragmentManager, PokemonCardPreviewDialogFragment.TAG)
    }

    companion object {
        const val ARG_PAGE_ID = "pageId"
    }
}