package com.adamjohnson.pokebinder.ui.drafts

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
import com.adamjohnson.pokebinder.ui.binder.BinderEditorFragment
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.DraftsViewModel

class DraftsFragment : Fragment(R.layout.fragment_drafts) {

    private val draftsViewModel: DraftsViewModel by viewModels()

    private lateinit var draftsAdapter: DraftsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val resultCountTextView: TextView = view.findViewById(R.id.resultCountTextView)
        val draftsRecyclerView: RecyclerView = view.findViewById(R.id.draftsRecyclerView)
        val loadingView: View = view.findViewById(R.id.loadingView)
        val emptyStateView: View = view.findViewById(R.id.emptyStateView)
        val emptyTitleTextView: TextView = view.findViewById(R.id.emptyTitleTextView)
        val emptyMessageTextView: TextView = view.findViewById(R.id.emptyMessageTextView)

        draftsAdapter = DraftsAdapter { binderPage ->
            openDraftInEditor(binderPage)
        }

        draftsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        draftsRecyclerView.adapter = draftsAdapter

        draftsViewModel.draftsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> {
                    renderLoading(
                        resultCountTextView = resultCountTextView,
                        draftsRecyclerView = draftsRecyclerView,
                        loadingView = loadingView,
                        emptyStateView = emptyStateView
                    )
                }

                is UiState.Success -> {
                    renderSuccess(
                        draftPages = state.data,
                        resultCountTextView = resultCountTextView,
                        draftsRecyclerView = draftsRecyclerView,
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
                        draftsRecyclerView = draftsRecyclerView,
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
        draftsViewModel.loadDraftPages()
    }

    private fun openDraftInEditor(binderPage: BinderPage) {
        if (binderPage.pageId.isBlank()) {
            return
        }

        if (findNavController().currentDestination?.id == R.id.draftsFragment) {
            findNavController().navigate(
                R.id.binderEditorFragment,
                bundleOf(BinderEditorFragment.ARG_PAGE_ID to binderPage.pageId)
            )
        }
    }

    private fun renderLoading(
        resultCountTextView: TextView,
        draftsRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View
    ) {
        resultCountTextView.text = getString(R.string.drafts_results_default)
        draftsRecyclerView.visibility = View.GONE
        loadingView.visibility = View.VISIBLE
        emptyStateView.visibility = View.GONE
    }

    private fun renderSuccess(
        draftPages: List<BinderPage>,
        resultCountTextView: TextView,
        draftsRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View,
        emptyTitleTextView: TextView,
        emptyMessageTextView: TextView
    ) {
        draftsAdapter.submitList(draftPages)
        resultCountTextView.text = getString(R.string.drafts_results_label, draftPages.size)
        loadingView.visibility = View.GONE

        if (draftPages.isEmpty()) {
            draftsRecyclerView.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
            emptyTitleTextView.text = getString(R.string.drafts_empty_title)
            emptyMessageTextView.text = getString(R.string.drafts_empty_message)
        } else {
            draftsRecyclerView.visibility = View.VISIBLE
            emptyStateView.visibility = View.GONE
        }
    }

    private fun renderError(
        message: String,
        resultCountTextView: TextView,
        draftsRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View,
        emptyTitleTextView: TextView,
        emptyMessageTextView: TextView
    ) {
        draftsAdapter.submitList(emptyList())
        resultCountTextView.text = getString(R.string.drafts_results_default)
        draftsRecyclerView.visibility = View.GONE
        loadingView.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
        emptyTitleTextView.text = getString(R.string.drafts_error_title)
        emptyMessageTextView.text = message
    }
}