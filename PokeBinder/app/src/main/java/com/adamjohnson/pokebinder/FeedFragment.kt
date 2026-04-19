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
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.ui.detail.PageDetailFragment
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.FeedViewModel

class FeedFragment : Fragment(R.layout.fragment_feed) {

    private val feedViewModel: FeedViewModel by viewModels()

    private lateinit var feedAdapter: FeedAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val resultCountTextView: TextView = view.findViewById(R.id.resultCountTextView)
        val feedRecyclerView: RecyclerView = view.findViewById(R.id.feedRecyclerView)
        val loadingView: View = view.findViewById(R.id.loadingView)
        val emptyStateView: View = view.findViewById(R.id.emptyStateView)
        val emptyTitleTextView: TextView = view.findViewById(R.id.emptyTitleTextView)
        val emptyMessageTextView: TextView = view.findViewById(R.id.emptyMessageTextView)

        feedAdapter = FeedAdapter { binderPage ->
            openPublishedPageDetail(binderPage)
        }

        feedRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        feedRecyclerView.adapter = feedAdapter

        feedViewModel.feedState.observe(viewLifecycleOwner) { state ->
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
        feedViewModel.loadFeedPages()
    }

    private fun openPublishedPageDetail(binderPage: BinderPage) {
        if (binderPage.pageId.isBlank()) {
            return
        }

        if (findNavController().currentDestination?.id == R.id.feedFragment) {
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
            emptyTitleTextView.text = getString(R.string.feed_empty_title)
            emptyMessageTextView.text = getString(R.string.feed_empty_message)
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
        emptyTitleTextView.text = getString(R.string.feed_error_title)
        emptyMessageTextView.text = message
    }
}