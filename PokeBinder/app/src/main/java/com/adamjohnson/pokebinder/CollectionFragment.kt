package com.adamjohnson.pokebinder.ui.collection

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.adamjohnson.pokebinder.ui.cardpreview.PokemonCardPreviewDialogFragment
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.CollectionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CollectionFragment : Fragment(R.layout.fragment_collection) {

    private val collectionViewModel: CollectionViewModel by viewModels()

    private lateinit var collectionAdapter: CollectionAdapter

    private var currentCards: List<PokemonCard> = emptyList()
    private var isFilterPanelVisible = false
    private var isDeleteModeEnabled = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isFilterPanelVisible =
            savedInstanceState?.getBoolean(STATE_FILTER_PANEL_VISIBLE) ?: false
        isDeleteModeEnabled =
            savedInstanceState?.getBoolean(STATE_DELETE_MODE_ENABLED) ?: false

        val addCardsButton: Button = view.findViewById(R.id.addCardsButton)
        val deleteModeButton: Button = view.findViewById(R.id.deleteModeButton)
        val searchToggleButton: ImageButton = view.findViewById(R.id.searchToggleButton)
        val searchFiltersContainer: View = view.findViewById(R.id.searchFiltersContainer)
        val applySearchButton: Button = view.findViewById(R.id.applySearchButton)

        val nameSearchEditText: AutoCompleteTextView = view.findViewById(R.id.nameSearchEditText)
        val setSearchEditText: AutoCompleteTextView = view.findViewById(R.id.setSearchEditText)
        val typeSearchEditText: AutoCompleteTextView = view.findViewById(R.id.typeSearchEditText)
        val cardIdSearchEditText: EditText = view.findViewById(R.id.cardIdSearchEditText)
        val subtypeSearchEditText: AutoCompleteTextView =
            view.findViewById(R.id.subtypeSearchEditText)
        val raritySearchEditText: AutoCompleteTextView =
            view.findViewById(R.id.raritySearchEditText)
        val fullArtOnlyCheckBox: CheckBox = view.findViewById(R.id.fullArtOnlyCheckBox)
        val clearFiltersButton: Button = view.findViewById(R.id.clearFiltersButton)

        val emptyAddCardsButton: Button = view.findViewById(R.id.emptyAddCardsButton)
        val resultCountTextView: TextView = view.findViewById(R.id.resultCountTextView)
        val collectionRecyclerView: RecyclerView = view.findViewById(R.id.collectionRecyclerView)
        val loadingView: View = view.findViewById(R.id.loadingView)
        val emptyStateView: View = view.findViewById(R.id.emptyStateView)
        val emptyTitleTextView: TextView = view.findViewById(R.id.emptyTitleTextView)
        val emptyMessageTextView: TextView = view.findViewById(R.id.emptyMessageTextView)

        addCardsButton.setOnClickListener {
            openCardSearch()
        }

        emptyAddCardsButton.setOnClickListener {
            openCardSearch()
        }

        deleteModeButton.setOnClickListener {
            isDeleteModeEnabled = !isDeleteModeEnabled
            updateDeleteModeButton(deleteModeButton)
            collectionAdapter.submitData(currentCards, isDeleteModeEnabled)
        }

        searchToggleButton.setOnClickListener {
            setFilterPanelVisible(searchFiltersContainer, !isFilterPanelVisible)
        }

        applySearchButton.setOnClickListener {
            setFilterPanelVisible(searchFiltersContainer, false)
        }

        nameSearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (collectionViewModel.nameQuery.value != query) {
                collectionViewModel.updateNameQuery(query)
            }
        }

        setSearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (collectionViewModel.setQuery.value != query) {
                collectionViewModel.updateSetQuery(query)
            }
        }

        typeSearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (collectionViewModel.typeQuery.value != query) {
                collectionViewModel.updateTypeQuery(query)
            }
        }

        cardIdSearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (collectionViewModel.cardIdQuery.value != query) {
                collectionViewModel.updateCardIdQuery(query)
            }
        }

        subtypeSearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (collectionViewModel.subtypeQuery.value != query) {
                collectionViewModel.updateSubtypeQuery(query)
            }
        }

        raritySearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (collectionViewModel.rarityQuery.value != query) {
                collectionViewModel.updateRarityQuery(query)
            }
        }

        fullArtOnlyCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (collectionViewModel.fullArtOnly.value != isChecked) {
                collectionViewModel.updateFullArtOnly(isChecked)
            }
        }

        clearFiltersButton.setOnClickListener {
            collectionViewModel.clearFilters()
        }

        collectionAdapter = CollectionAdapter(
            onCardClick = { card ->
                showCardPreview(card)
            },
            onDeleteClick = { card ->
                confirmDeleteCard(card)
            }
        )

        collectionRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        collectionRecyclerView.adapter = collectionAdapter

        collectionViewModel.nameQuery.observe(viewLifecycleOwner) { query ->
            syncText(nameSearchEditText, query)
        }

        collectionViewModel.setQuery.observe(viewLifecycleOwner) { query ->
            syncText(setSearchEditText, query)
        }

        collectionViewModel.typeQuery.observe(viewLifecycleOwner) { query ->
            syncText(typeSearchEditText, query)
        }

        collectionViewModel.cardIdQuery.observe(viewLifecycleOwner) { query ->
            syncText(cardIdSearchEditText, query)
        }

        collectionViewModel.subtypeQuery.observe(viewLifecycleOwner) { query ->
            syncText(subtypeSearchEditText, query)
        }

        collectionViewModel.rarityQuery.observe(viewLifecycleOwner) { query ->
            syncText(raritySearchEditText, query)
        }

        collectionViewModel.fullArtOnly.observe(viewLifecycleOwner) { enabled ->
            if (fullArtOnlyCheckBox.isChecked != enabled) {
                fullArtOnlyCheckBox.isChecked = enabled
            }
        }

        collectionViewModel.nameSuggestions.observe(viewLifecycleOwner) { suggestions ->
            bindSuggestions(nameSearchEditText, suggestions)
        }

        collectionViewModel.setSuggestions.observe(viewLifecycleOwner) { suggestions ->
            bindSuggestions(setSearchEditText, suggestions)
        }

        collectionViewModel.typeSuggestions.observe(viewLifecycleOwner) { suggestions ->
            bindSuggestions(typeSearchEditText, suggestions)
        }

        collectionViewModel.subtypeSuggestions.observe(viewLifecycleOwner) { suggestions ->
            bindSuggestions(subtypeSearchEditText, suggestions)
        }

        collectionViewModel.raritySuggestions.observe(viewLifecycleOwner) { suggestions ->
            bindSuggestions(raritySearchEditText, suggestions)
        }

        collectionViewModel.collectionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> {
                    renderLoading(
                        resultCountTextView = resultCountTextView,
                        collectionRecyclerView = collectionRecyclerView,
                        loadingView = loadingView,
                        emptyStateView = emptyStateView
                    )
                }

                is UiState.Success -> {
                    renderSuccess(
                        cards = state.data,
                        resultCountTextView = resultCountTextView,
                        collectionRecyclerView = collectionRecyclerView,
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
                        collectionRecyclerView = collectionRecyclerView,
                        loadingView = loadingView,
                        emptyStateView = emptyStateView,
                        emptyTitleTextView = emptyTitleTextView,
                        emptyMessageTextView = emptyMessageTextView
                    )
                }
            }
        }

        updateDeleteModeButton(deleteModeButton)
        setFilterPanelVisible(searchFiltersContainer, isFilterPanelVisible)
    }

    override fun onResume() {
        super.onResume()
        collectionViewModel.loadCollection()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_FILTER_PANEL_VISIBLE, isFilterPanelVisible)
        outState.putBoolean(STATE_DELETE_MODE_ENABLED, isDeleteModeEnabled)
    }

    private fun openCardSearch() {
        if (findNavController().currentDestination?.id == R.id.collectionFragment) {
            findNavController().navigate(R.id.cardSearchFragment)
        }
    }

    private fun showCardPreview(card: PokemonCard) {
        if (!isAdded || isDeleteModeEnabled) {
            return
        }

        if (childFragmentManager.findFragmentByTag(PokemonCardPreviewDialogFragment.TAG) != null) {
            return
        }

        PokemonCardPreviewDialogFragment.newInstance(card.cardId)
            .show(childFragmentManager, PokemonCardPreviewDialogFragment.TAG)
    }

    private fun confirmDeleteCard(card: PokemonCard) {
        if (!isAdded || !isDeleteModeEnabled) {
            return
        }

        val title = card.name.ifBlank { card.cardId }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove card?")
            .setMessage("Remove \"$title\" from your collection?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                collectionViewModel.removeCard(card.cardId) { errorMessage ->
                    if (errorMessage != null && isAdded) {
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun updateDeleteModeButton(deleteModeButton: Button) {
        deleteModeButton.text = if (isDeleteModeEnabled) {
            "Done"
        } else {
            "Delete"
        }
    }

    private fun setFilterPanelVisible(
        searchFiltersContainer: View,
        visible: Boolean
    ) {
        isFilterPanelVisible = visible
        searchFiltersContainer.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun renderLoading(
        resultCountTextView: TextView,
        collectionRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View
    ) {
        resultCountTextView.text = getString(R.string.collection_results_default)
        collectionRecyclerView.visibility = View.GONE
        loadingView.visibility = View.VISIBLE
        emptyStateView.visibility = View.GONE
    }

    private fun renderSuccess(
        cards: List<PokemonCard>,
        resultCountTextView: TextView,
        collectionRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View,
        emptyTitleTextView: TextView,
        emptyMessageTextView: TextView
    ) {
        currentCards = cards
        collectionAdapter.submitData(currentCards, isDeleteModeEnabled)
        resultCountTextView.text = getString(R.string.collection_results_label, cards.size)
        loadingView.visibility = View.GONE

        if (cards.isEmpty()) {
            collectionRecyclerView.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
            emptyTitleTextView.text = getString(R.string.collection_empty_title)
            emptyMessageTextView.text = if (hasActiveFilters()) {
                "Try changing or clearing one of the filters."
            } else {
                getString(R.string.collection_empty_message)
            }
        } else {
            collectionRecyclerView.visibility = View.VISIBLE
            emptyStateView.visibility = View.GONE
        }
    }

    private fun renderError(
        message: String,
        resultCountTextView: TextView,
        collectionRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View,
        emptyTitleTextView: TextView,
        emptyMessageTextView: TextView
    ) {
        currentCards = emptyList()
        collectionAdapter.submitData(emptyList(), isDeleteModeEnabled)
        resultCountTextView.text = getString(R.string.collection_results_default)
        collectionRecyclerView.visibility = View.GONE
        loadingView.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
        emptyTitleTextView.text = getString(R.string.collection_error_title)
        emptyMessageTextView.text = message
    }

    private fun syncText(editText: EditText, value: String) {
        if (editText.text.toString() != value) {
            editText.setText(value)
            editText.setSelection(value.length)
        }
    }

    private fun bindSuggestions(
        autoCompleteTextView: AutoCompleteTextView,
        suggestions: List<String>
    ) {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            suggestions
        )
        autoCompleteTextView.setAdapter(adapter)

        if (suggestions.isEmpty()) {
            autoCompleteTextView.dismissDropDown()
        } else if (
            autoCompleteTextView.hasFocus() &&
            autoCompleteTextView.text.toString().isNotBlank()
        ) {
            autoCompleteTextView.showDropDown()
        }
    }

    private fun hasActiveFilters(): Boolean {
        return collectionViewModel.nameQuery.value.orEmpty().isNotBlank() ||
                collectionViewModel.setQuery.value.orEmpty().isNotBlank() ||
                collectionViewModel.typeQuery.value.orEmpty().isNotBlank() ||
                collectionViewModel.cardIdQuery.value.orEmpty().isNotBlank() ||
                collectionViewModel.subtypeQuery.value.orEmpty().isNotBlank() ||
                collectionViewModel.rarityQuery.value.orEmpty().isNotBlank() ||
                (collectionViewModel.fullArtOnly.value ?: false)
    }

    companion object {
        private const val STATE_FILTER_PANEL_VISIBLE = "stateFilterPanelVisible"
        private const val STATE_DELETE_MODE_ENABLED = "stateDeleteModeEnabled"
    }
}