package com.adamjohnson.pokebinder.ui.cardsearch

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.adamjohnson.pokebinder.ui.cardpreview.PokemonCardPreviewDialogFragment
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.CardSearchViewModel

class CardSearchFragment : Fragment(R.layout.fragment_card_search) {

    private val cardSearchViewModel: CardSearchViewModel by viewModels()

    private lateinit var cardSearchAdapter: CardSearchAdapter

    private var currentCards: List<PokemonCard> = emptyList()
    private var currentSavedCardIds: Set<String> = emptySet()
    private var isFilterPanelVisible = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isFilterPanelVisible =
            savedInstanceState?.getBoolean(STATE_FILTER_PANEL_VISIBLE) ?: false

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

        val resultCountTextView: TextView = view.findViewById(R.id.resultCountTextView)
        val cardResultsRecyclerView: RecyclerView = view.findViewById(R.id.cardResultsRecyclerView)
        val loadingView: View = view.findViewById(R.id.loadingView)
        val emptyStateView: View = view.findViewById(R.id.emptyStateView)
        val emptyTitleTextView: TextView = view.findViewById(R.id.emptyTitleTextView)
        val emptyMessageTextView: TextView = view.findViewById(R.id.emptyMessageTextView)

        cardSearchAdapter = CardSearchAdapter(
            onSaveToggleClick = { card ->
                cardSearchViewModel.toggleSavedCard(card.cardId) { errorMessage ->
                    if (errorMessage != null && isAdded) {
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onCardLongClick = { card ->
                showCardPreview(card)
            }
        )

        cardResultsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        cardResultsRecyclerView.adapter = cardSearchAdapter

        updateSearchToggleButtonDescription(searchToggleButton)

        searchToggleButton.setOnClickListener {
            setFilterPanelVisible(
                searchFiltersContainer = searchFiltersContainer,
                searchToggleButton = searchToggleButton,
                visible = !isFilterPanelVisible
            )
        }

        applySearchButton.setOnClickListener {
            setFilterPanelVisible(
                searchFiltersContainer = searchFiltersContainer,
                searchToggleButton = searchToggleButton,
                visible = false
            )
        }

        nameSearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (cardSearchViewModel.nameQuery.value != query) {
                cardSearchViewModel.updateNameQuery(query)
            }
        }

        setSearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (cardSearchViewModel.setQuery.value != query) {
                cardSearchViewModel.updateSetQuery(query)
            }
        }

        typeSearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (cardSearchViewModel.typeQuery.value != query) {
                cardSearchViewModel.updateTypeQuery(query)
            }
        }

        cardIdSearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (cardSearchViewModel.cardIdQuery.value != query) {
                cardSearchViewModel.updateCardIdQuery(query)
            }
        }

        subtypeSearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (cardSearchViewModel.subtypeQuery.value != query) {
                cardSearchViewModel.updateSubtypeQuery(query)
            }
        }

        raritySearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (cardSearchViewModel.rarityQuery.value != query) {
                cardSearchViewModel.updateRarityQuery(query)
            }
        }

        fullArtOnlyCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (cardSearchViewModel.fullArtOnly.value != isChecked) {
                cardSearchViewModel.updateFullArtOnly(isChecked)
            }
        }

        clearFiltersButton.setOnClickListener {
            cardSearchViewModel.clearFilters()
        }

        cardSearchViewModel.nameQuery.observe(viewLifecycleOwner) { query ->
            syncText(nameSearchEditText, query)
        }

        cardSearchViewModel.setQuery.observe(viewLifecycleOwner) { query ->
            syncText(setSearchEditText, query)
        }

        cardSearchViewModel.typeQuery.observe(viewLifecycleOwner) { query ->
            syncText(typeSearchEditText, query)
        }

        cardSearchViewModel.cardIdQuery.observe(viewLifecycleOwner) { query ->
            syncText(cardIdSearchEditText, query)
        }

        cardSearchViewModel.subtypeQuery.observe(viewLifecycleOwner) { query ->
            syncText(subtypeSearchEditText, query)
        }

        cardSearchViewModel.rarityQuery.observe(viewLifecycleOwner) { query ->
            syncText(raritySearchEditText, query)
        }

        cardSearchViewModel.fullArtOnly.observe(viewLifecycleOwner) { enabled ->
            if (fullArtOnlyCheckBox.isChecked != enabled) {
                fullArtOnlyCheckBox.isChecked = enabled
            }
        }

        cardSearchViewModel.nameSuggestions.observe(viewLifecycleOwner) { suggestions ->
            bindSuggestions(nameSearchEditText, suggestions)
        }

        cardSearchViewModel.setSuggestions.observe(viewLifecycleOwner) { suggestions ->
            bindSuggestions(setSearchEditText, suggestions)
        }

        cardSearchViewModel.typeSuggestions.observe(viewLifecycleOwner) { suggestions ->
            bindSuggestions(typeSearchEditText, suggestions)
        }

        cardSearchViewModel.subtypeSuggestions.observe(viewLifecycleOwner) { suggestions ->
            bindSuggestions(subtypeSearchEditText, suggestions)
        }

        cardSearchViewModel.raritySuggestions.observe(viewLifecycleOwner) { suggestions ->
            bindSuggestions(raritySearchEditText, suggestions)
        }

        cardSearchViewModel.savedCardIds.observe(viewLifecycleOwner) { savedCardIds ->
            currentSavedCardIds = savedCardIds
            cardSearchAdapter.submitList(currentCards, currentSavedCardIds)
        }

        cardSearchViewModel.cardsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> {
                    renderLoading(
                        resultCountTextView = resultCountTextView,
                        cardResultsRecyclerView = cardResultsRecyclerView,
                        loadingView = loadingView,
                        emptyStateView = emptyStateView
                    )
                }

                is UiState.Success -> {
                    renderSuccess(
                        cards = state.data,
                        resultCountTextView = resultCountTextView,
                        cardResultsRecyclerView = cardResultsRecyclerView,
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
                        cardResultsRecyclerView = cardResultsRecyclerView,
                        loadingView = loadingView,
                        emptyStateView = emptyStateView,
                        emptyTitleTextView = emptyTitleTextView,
                        emptyMessageTextView = emptyMessageTextView
                    )
                }
            }
        }

        setFilterPanelVisible(
            searchFiltersContainer = searchFiltersContainer,
            searchToggleButton = searchToggleButton,
            visible = isFilterPanelVisible
        )
    }

    override fun onResume() {
        super.onResume()
        cardSearchViewModel.loadSavedCardIds()
        cardSearchViewModel.loadCards()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_FILTER_PANEL_VISIBLE, isFilterPanelVisible)
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

    private fun setFilterPanelVisible(
        searchFiltersContainer: View,
        searchToggleButton: ImageButton,
        visible: Boolean
    ) {
        isFilterPanelVisible = visible
        searchFiltersContainer.visibility = if (visible) View.VISIBLE else View.GONE
        updateSearchToggleButtonDescription(searchToggleButton)
    }

    private fun updateSearchToggleButtonDescription(searchToggleButton: ImageButton) {
        searchToggleButton.contentDescription = if (isFilterPanelVisible) {
            getString(R.string.hide_search_and_filters)
        } else {
            getString(R.string.show_search_and_filters)
        }
    }

    private fun renderLoading(
        resultCountTextView: TextView,
        cardResultsRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View
    ) {
        resultCountTextView.text = getString(R.string.card_search_results_default)
        cardResultsRecyclerView.visibility = View.GONE
        loadingView.visibility = View.VISIBLE
        emptyStateView.visibility = View.GONE
    }

    private fun renderSuccess(
        cards: List<PokemonCard>,
        resultCountTextView: TextView,
        cardResultsRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View,
        emptyTitleTextView: TextView,
        emptyMessageTextView: TextView
    ) {
        currentCards = cards
        cardSearchAdapter.submitList(currentCards, currentSavedCardIds)
        resultCountTextView.text = getString(R.string.card_search_results_label, cards.size)
        loadingView.visibility = View.GONE

        if (cards.isEmpty()) {
            cardResultsRecyclerView.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
            emptyTitleTextView.text = getString(R.string.card_search_empty_title)
            emptyMessageTextView.text = if (hasActiveFilters()) {
                getString(R.string.common_filters_empty_message)
            } else {
                getString(R.string.card_search_empty_message)
            }
        } else {
            cardResultsRecyclerView.visibility = View.VISIBLE
            emptyStateView.visibility = View.GONE
        }
    }

    private fun renderError(
        message: String,
        resultCountTextView: TextView,
        cardResultsRecyclerView: RecyclerView,
        loadingView: View,
        emptyStateView: View,
        emptyTitleTextView: TextView,
        emptyMessageTextView: TextView
    ) {
        currentCards = emptyList()
        cardSearchAdapter.submitList(emptyList(), currentSavedCardIds)
        resultCountTextView.text = getString(R.string.card_search_results_default)
        cardResultsRecyclerView.visibility = View.GONE
        loadingView.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
        emptyTitleTextView.text = getString(R.string.card_search_error_title)
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
        return cardSearchViewModel.nameQuery.value.orEmpty().isNotBlank() ||
                cardSearchViewModel.setQuery.value.orEmpty().isNotBlank() ||
                cardSearchViewModel.typeQuery.value.orEmpty().isNotBlank() ||
                cardSearchViewModel.cardIdQuery.value.orEmpty().isNotBlank() ||
                cardSearchViewModel.subtypeQuery.value.orEmpty().isNotBlank() ||
                cardSearchViewModel.rarityQuery.value.orEmpty().isNotBlank() ||
                (cardSearchViewModel.fullArtOnly.value ?: false)
    }

    companion object {
        private const val STATE_FILTER_PANEL_VISIBLE = "stateFilterPanelVisible"
    }
}