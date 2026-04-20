package com.adamjohnson.pokebinder.ui.feed

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.local.PokemonCardLocalDataSource
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.repository.CardRepository
import com.adamjohnson.pokebinder.ui.detail.PageDetailFragment
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.SocialPagesViewModel

class SocialPagesFragment : Fragment(R.layout.fragment_feed) {

    private val socialPagesViewModel: SocialPagesViewModel by viewModels()

    private val cardRepository by lazy {
        CardRepository(PokemonCardLocalDataSource(requireContext().applicationContext))
    }

    private lateinit var feedAdapter: FeedAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mode = getListMode()
        val feedTitleTextView: TextView = view.findViewById(R.id.feedTitleTextView)
        val resultCountTextView: TextView = view.findViewById(R.id.resultCountTextView)
        val loadingTextView: TextView = view.findViewById(R.id.loadingTextView)
        val feedRecyclerView: RecyclerView = view.findViewById(R.id.feedRecyclerView)
        val loadingView: View = view.findViewById(R.id.loadingView)
        val emptyStateView: View = view.findViewById(R.id.emptyStateView)
        val emptyTitleTextView: TextView = view.findViewById(R.id.emptyTitleTextView)
        val emptyMessageTextView: TextView = view.findViewById(R.id.emptyMessageTextView)

        feedTitleTextView.text = getScreenTitle(mode)
        loadingTextView.text = getLoadingMessage(mode)

        feedAdapter = FeedAdapter(cardRepository) { binderPage ->
            openPublishedPageDetail(binderPage)
        }

        feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        feedRecyclerView.adapter = feedAdapter

        socialPagesViewModel.pagesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> {
                    renderLoading(
                        resultCountTextView = resultCountTextView,
                        feedRecyclerView = feedRecyclerView,
                        loadingView = loadingView,
                        emptyStateView = emptyStateView
                    )
                }

                is UiState.Success -> {
                    renderSuccess(
                        mode = mode,
                        publishedPages = state.data,
                        resultCountTextView = resultCountTextView,
                        feedRecyclerView = feedRecyclerView,
                        loadingView = loadingView,
                        emptyStateView = emptyStateView,
                        emptyTitleTextView = emptyTitleTextView,
                        emptyMessageTextView = emptyMessageTextView
                    )
                }

                is UiState.Error -> {
                    renderError(
                        message = state.message,
                        resultCountTextView = resultCountTextView,
                        feedRecyclerView = feedRecyclerView,
                        loadingView = loadingView,
                        emptyStateView = emptyStateView,
                        emptyTitleTextView = emptyTitleTextView,
                        emptyMessageTextView = emptyMessageTextView
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        socialPagesViewModel.loadPages(getListMode())
    }

    private fun getListMode(): String {
        val mode = arguments?.getString(ARG_LIST_MODE).orEmpty()
        return if (mode == MODE_COMMENTS) {
            MODE_COMMENTS
        } else {
            MODE_LIKES
        }
    }

    private fun getScreenTitle(mode: String): String {
        return if (mode == MODE_COMMENTS) {
            getString(R.string.social_pages_comments_title)
        } else {
            getString(R.string.social_pages_likes_title)
        }
    }

    private fun getLoadingMessage(mode: String): String {
        return if (mode == MODE_COMMENTS) {
            getString(R.string.social_pages_comments_loading)
        } else {
            getString(R.string.social_pages_likes_loading)
        }
    }

    private fun openPublishedPageDetail(binderPage: BinderPage) {
        if (binderPage.pageId.isBlank()) {
            return
        }

        if (findNavController().currentDestination?.id == R.id.socialPagesFragment) {
            findNavController().navigate(
                R.id.pageDetailFragment,
                bundleOf(PageDetailFragment.ARG_PAGE_ID to binderPage.pageId)
            )
        }
    }

    private fun renderLoading(
        resultCountTextView: TextView,
        feedRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View
    ) {
        resultCountTextView.text = getString(R.string.feed_results_default)
        feedRecyclerView.visibility = View.GONE
        loadingView.visibility = View.VISIBLE
        emptyStateView.visibility = View.GONE
    }

    private fun renderSuccess(
        mode: String,
        publishedPages: List<BinderPage>,
        resultCountTextView: TextView,
        feedRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View,
        emptyTitleTextView: TextView,
        emptyMessageTextView: TextView
    ) {
        feedAdapter.submitList(publishedPages)
        resultCountTextView.text = getString(R.string.feed_results_label, publishedPages.size)
        loadingView.visibility = View.GONE

        if (publishedPages.isEmpty()) {
            feedRecyclerView.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
            if (mode == MODE_COMMENTS) {
                emptyTitleTextView.text = getString(R.string.social_pages_comments_empty_title)
                emptyMessageTextView.text = getString(R.string.social_pages_comments_empty_message)
            } else {
                emptyTitleTextView.text = getString(R.string.social_pages_likes_empty_title)
                emptyMessageTextView.text = getString(R.string.social_pages_likes_empty_message)
            }
        } else {
            feedRecyclerView.visibility = View.VISIBLE
            emptyStateView.visibility = View.GONE
        }
    }

    private fun renderError(
        message: String,
        resultCountTextView: TextView,
        feedRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View,
        emptyTitleTextView: TextView,
        emptyMessageTextView: TextView
    ) {
        feedAdapter.submitList(emptyList())
        resultCountTextView.text = getString(R.string.feed_results_default)
        feedRecyclerView.visibility = View.GONE
        loadingView.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
        emptyTitleTextView.text = getString(R.string.social_pages_error_title)
        emptyMessageTextView.text = message
    }

    companion object {
        const val ARG_LIST_MODE = "listMode"
        const val MODE_LIKES = "likes"
        const val MODE_COMMENTS = "comments"
    }
}