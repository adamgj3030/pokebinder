package com.adamjohnson.pokebinder.ui.drafts

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.local.PokemonCardLocalDataSource
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.BinderPageStatus
import com.adamjohnson.pokebinder.data.repository.CardRepository
import com.adamjohnson.pokebinder.ui.binder.BinderEditorFragment
import com.adamjohnson.pokebinder.ui.detail.PageDetailFragment
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.DraftsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DraftsFragment : Fragment(R.layout.fragment_drafts) {

    private val draftsViewModel: DraftsViewModel by viewModels()

    private val cardRepository by lazy {
        CardRepository(PokemonCardLocalDataSource(requireContext().applicationContext))
    }

    private lateinit var draftsAdapter: DraftsAdapter

    private var isSearchPanelVisible = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isSearchPanelVisible = savedInstanceState?.getBoolean(STATE_SEARCH_PANEL_VISIBLE) ?: false

        val totalBindersCountTextView: TextView = view.findViewById(R.id.totalBindersCountTextView)
        val searchToggleButton: ImageButton = view.findViewById(R.id.searchToggleButton)
        val searchFiltersContainer: View = view.findViewById(R.id.searchFiltersContainer)
        val applySearchButton: Button = view.findViewById(R.id.applySearchButton)
        val searchEditText: EditText = view.findViewById(R.id.searchEditText)
        val filterRadioGroup: RadioGroup = view.findViewById(R.id.filterRadioGroup)
        val resultCountTextView: TextView = view.findViewById(R.id.resultCountTextView)
        val draftsRecyclerView: RecyclerView = view.findViewById(R.id.draftsRecyclerView)
        val loadingView: View = view.findViewById(R.id.loadingView)
        val emptyStateView: View = view.findViewById(R.id.emptyStateView)
        val emptyTitleTextView: TextView = view.findViewById(R.id.emptyTitleTextView)
        val emptyMessageTextView: TextView = view.findViewById(R.id.emptyMessageTextView)

        draftsAdapter = DraftsAdapter(
            cardRepository = cardRepository,
            onCreateNewClick = { openCreateNewBinder() },
            onBinderClick = { binderPage -> openBinder(binderPage) },
            onDeleteClick = { binderPage -> confirmDeleteBinder(binderPage) }
        )

        draftsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        draftsRecyclerView.adapter = draftsAdapter

        updateSearchToggleButtonDescription(searchToggleButton)

        searchToggleButton.setOnClickListener {
            setSearchPanelVisible(
                searchFiltersContainer = searchFiltersContainer,
                searchToggleButton = searchToggleButton,
                visible = !isSearchPanelVisible
            )
        }

        applySearchButton.setOnClickListener {
            setSearchPanelVisible(
                searchFiltersContainer = searchFiltersContainer,
                searchToggleButton = searchToggleButton,
                visible = false
            )
        }

        searchEditText.doAfterTextChanged { editable ->
            draftsViewModel.updateSearchQuery(editable?.toString().orEmpty())
        }

        filterRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val filter = when (checkedId) {
                R.id.draftsOnlyRadioButton -> DraftsViewModel.BinderFilter.DRAFTS
                R.id.publishedOnlyRadioButton -> DraftsViewModel.BinderFilter.PUBLISHED
                else -> DraftsViewModel.BinderFilter.ALL
            }

            draftsViewModel.updateFilter(filter)
        }

        draftsViewModel.bindersState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> {
                    renderLoading(
                        totalBindersCountTextView = totalBindersCountTextView,
                        resultCountTextView = resultCountTextView,
                        draftsRecyclerView = draftsRecyclerView,
                        loadingView = loadingView,
                        emptyStateView = emptyStateView
                    )
                }

                is UiState.Success -> {
                    renderSuccess(
                        screenState = state.data,
                        searchEditText = searchEditText,
                        filterRadioGroup = filterRadioGroup,
                        totalBindersCountTextView = totalBindersCountTextView,
                        resultCountTextView = resultCountTextView,
                        draftsRecyclerView = draftsRecyclerView,
                        loadingView = loadingView,
                        emptyStateView = emptyStateView
                    )
                }

                is UiState.Error -> {
                    renderError(
                        message = state.message,
                        totalBindersCountTextView = totalBindersCountTextView,
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

        setSearchPanelVisible(
            searchFiltersContainer = searchFiltersContainer,
            searchToggleButton = searchToggleButton,
            visible = isSearchPanelVisible
        )
    }

    override fun onResume() {
        super.onResume()
        draftsViewModel.loadBinderPages()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_SEARCH_PANEL_VISIBLE, isSearchPanelVisible)
    }

    private fun openCreateNewBinder() {
        if (findNavController().currentDestination?.id == R.id.draftsFragment) {
            findNavController().navigate(
                R.id.binderEditorFragment,
                bundleOf(BinderEditorFragment.ARG_PAGE_ID to null)
            )
        }
    }

    private fun openBinder(binderPage: BinderPage) {
        if (binderPage.pageId.isBlank()) {
            return
        }

        if (binderPage.status == BinderPageStatus.PUBLISHED) {
            openPublishedPageDetail(binderPage.pageId)
        } else {
            openDraftInEditor(binderPage.pageId)
        }
    }

    private fun openDraftInEditor(pageId: String) {
        if (findNavController().currentDestination?.id == R.id.draftsFragment) {
            findNavController().navigate(
                R.id.binderEditorFragment,
                bundleOf(BinderEditorFragment.ARG_PAGE_ID to pageId)
            )
        }
    }

    private fun openPublishedPageDetail(pageId: String) {
        if (findNavController().currentDestination?.id == R.id.draftsFragment) {
            findNavController().navigate(
                R.id.pageDetailFragment,
                bundleOf(PageDetailFragment.ARG_PAGE_ID to pageId)
            )
        }
    }

    private fun confirmDeleteBinder(binderPage: BinderPage) {
        if (binderPage.pageId.isBlank()) {
            return
        }

        val title = binderPage.title.ifBlank {
            getString(R.string.binders_item_untitled)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.binders_delete_title))
            .setMessage(getString(R.string.binders_delete_message, title))
            .setNegativeButton(R.string.binders_delete_cancel, null)
            .setPositiveButton(R.string.binders_delete_confirm) { _, _ ->
                deleteBinder(binderPage)
            }
            .show()
    }

    private fun deleteBinder(binderPage: BinderPage) {
        draftsViewModel.deleteBinderPage(binderPage) { errorMessage ->
            if (!isAdded) {
                return@deleteBinderPage
            }

            val message = errorMessage ?: getString(R.string.binders_delete_success)
            showShortToast(message)
        }
    }

    private fun setSearchPanelVisible(
        searchFiltersContainer: View,
        searchToggleButton: ImageButton,
        visible: Boolean
    ) {
        isSearchPanelVisible = visible
        searchFiltersContainer.visibility = if (visible) View.VISIBLE else View.GONE
        updateSearchToggleButtonDescription(searchToggleButton)
    }

    private fun updateSearchToggleButtonDescription(searchToggleButton: ImageButton) {
        searchToggleButton.contentDescription = if (isSearchPanelVisible) {
            getString(R.string.binders_hide_search)
        } else {
            getString(R.string.binders_show_search)
        }
    }

    private fun renderLoading(
        totalBindersCountTextView: TextView,
        resultCountTextView: TextView,
        draftsRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View
    ) {
        totalBindersCountTextView.text = getString(R.string.binders_total_count, 0)
        resultCountTextView.text = getString(R.string.binders_results_default)
        draftsRecyclerView.visibility = View.GONE
        loadingView.visibility = View.VISIBLE
        emptyStateView.visibility = View.GONE
    }

    private fun renderSuccess(
        screenState: DraftsViewModel.BindersScreenState,
        searchEditText: EditText,
        filterRadioGroup: RadioGroup,
        totalBindersCountTextView: TextView,
        resultCountTextView: TextView,
        draftsRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View
    ) {
        syncSearchAndFilter(
            searchEditText = searchEditText,
            filterRadioGroup = filterRadioGroup,
            screenState = screenState
        )

        draftsAdapter.submitList(screenState.binders)
        totalBindersCountTextView.text =
            getString(R.string.binders_total_count, screenState.totalBindersCount)
        resultCountTextView.text = getString(
            R.string.binders_results_label,
            screenState.binders.size
        )

        draftsRecyclerView.visibility = View.VISIBLE
        loadingView.visibility = View.GONE
        emptyStateView.visibility = View.GONE
    }

    private fun renderError(
        message: String,
        totalBindersCountTextView: TextView,
        resultCountTextView: TextView,
        draftsRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View,
        emptyTitleTextView: TextView,
        emptyMessageTextView: TextView
    ) {
        draftsAdapter.submitList(emptyList())
        totalBindersCountTextView.text = getString(R.string.binders_total_count, 0)
        resultCountTextView.text = getString(R.string.binders_results_default)
        draftsRecyclerView.visibility = View.GONE
        loadingView.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
        emptyTitleTextView.text = getString(R.string.binders_error_title)
        emptyMessageTextView.text = message
    }

    private fun syncSearchAndFilter(
        searchEditText: EditText,
        filterRadioGroup: RadioGroup,
        screenState: DraftsViewModel.BindersScreenState
    ) {
        if (searchEditText.text.toString() != screenState.searchQuery) {
            searchEditText.setText(screenState.searchQuery)
            searchEditText.setSelection(screenState.searchQuery.length)
        }

        val radioButtonId = when (screenState.filter) {
            DraftsViewModel.BinderFilter.ALL -> R.id.allRadioButton
            DraftsViewModel.BinderFilter.DRAFTS -> R.id.draftsOnlyRadioButton
            DraftsViewModel.BinderFilter.PUBLISHED -> R.id.publishedOnlyRadioButton
        }

        if (filterRadioGroup.checkedRadioButtonId != radioButtonId) {
            filterRadioGroup.check(radioButtonId)
        }
    }

    private fun showShortToast(message: String) {
        if (!isAdded) {
            return
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val STATE_SEARCH_PANEL_VISIBLE = "stateSearchPanelVisible"
    }
}