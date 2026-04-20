package com.adamjohnson.pokebinder.ui.binder

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.fragment.app.setFragmentResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.AddToBinderViewModel

class AddToBinderDialogFragment : DialogFragment(R.layout.fragment_add_to_binder) {

    private val addToBinderViewModel: AddToBinderViewModel by viewModels()

    private lateinit var addToBinderAdapter: AddToBinderAdapter

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cancelButton: Button = view.findViewById(R.id.cancelButton)
        val addButton: Button = view.findViewById(R.id.addButton)
        val clearFiltersButton: Button = view.findViewById(R.id.clearFiltersButton)
        val selectedCountTextView: TextView = view.findViewById(R.id.selectedCountTextView)

        val nameSearchEditText: AutoCompleteTextView = view.findViewById(R.id.nameSearchEditText)
        val setSearchEditText: AutoCompleteTextView = view.findViewById(R.id.setSearchEditText)
        val typeSearchEditText: AutoCompleteTextView = view.findViewById(R.id.typeSearchEditText)
        val cardIdSearchEditText: EditText = view.findViewById(R.id.cardIdSearchEditText)

        val cardResultsRecyclerView: RecyclerView = view.findViewById(R.id.cardResultsRecyclerView)
        val loadingView: View = view.findViewById(R.id.loadingView)
        val emptyStateView: View = view.findViewById(R.id.emptyStateView)
        val emptyTitleTextView: TextView = view.findViewById(R.id.emptyTitleTextView)
        val emptyMessageTextView: TextView = view.findViewById(R.id.emptyMessageTextView)

        val startSlotIndex = arguments?.getInt(ARG_START_SLOT_INDEX, 0) ?: 0
        val maxSelectable = arguments?.getInt(ARG_MAX_SELECTABLE, 0) ?: 0

        addToBinderAdapter = AddToBinderAdapter { card ->
            handleCardTapped(card)
        }

        cardResultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        cardResultsRecyclerView.adapter = addToBinderAdapter

        cancelButton.setOnClickListener {
            dismiss()
        }

        addButton.setOnClickListener {
            val selectedCardIds = ArrayList(addToBinderViewModel.getSelectedCardIds())
            if (selectedCardIds.isEmpty()) {
                return@setOnClickListener
            }

            setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    RESULT_START_SLOT_INDEX to startSlotIndex,
                    RESULT_SELECTED_CARD_IDS to selectedCardIds
                )
            )

            dismiss()
        }

        clearFiltersButton.setOnClickListener {
            addToBinderViewModel.clearFilters()
        }

        nameSearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (addToBinderViewModel.nameQuery.value != query) {
                addToBinderViewModel.updateNameQuery(query)
            }
        }

        setSearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (addToBinderViewModel.setQuery.value != query) {
                addToBinderViewModel.updateSetQuery(query)
            }
        }

        typeSearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (addToBinderViewModel.typeQuery.value != query) {
                addToBinderViewModel.updateTypeQuery(query)
            }
        }

        cardIdSearchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (addToBinderViewModel.cardIdQuery.value != query) {
                addToBinderViewModel.updateCardIdQuery(query)
            }
        }

        addToBinderViewModel.nameQuery.observe(viewLifecycleOwner) { query ->
            syncText(nameSearchEditText, query)
        }

        addToBinderViewModel.setQuery.observe(viewLifecycleOwner) { query ->
            syncText(setSearchEditText, query)
        }

        addToBinderViewModel.typeQuery.observe(viewLifecycleOwner) { query ->
            syncText(typeSearchEditText, query)
        }

        addToBinderViewModel.cardIdQuery.observe(viewLifecycleOwner) { query ->
            syncText(cardIdSearchEditText, query)
        }

        addToBinderViewModel.nameSuggestions.observe(viewLifecycleOwner) { suggestions ->
            bindSuggestions(nameSearchEditText, suggestions)
        }

        addToBinderViewModel.setSuggestions.observe(viewLifecycleOwner) { suggestions ->
            bindSuggestions(setSearchEditText, suggestions)
        }

        addToBinderViewModel.typeSuggestions.observe(viewLifecycleOwner) { suggestions ->
            bindSuggestions(typeSearchEditText, suggestions)
        }

        addToBinderViewModel.maxSelectable.observe(viewLifecycleOwner) { latestMaxSelectable ->
            renderSelectedCount(
                selectedCountTextView = selectedCountTextView,
                selectedCardIds = addToBinderViewModel.selectedCardIds.value.orEmpty(),
                maxSelectable = latestMaxSelectable
            )
        }

        addToBinderViewModel.selectedCardIds.observe(viewLifecycleOwner) { selectedCardIds ->
            val currentCards = when (val state = addToBinderViewModel.cardsState.value) {
                is UiState.Success -> state.data
                else -> emptyList()
            }

            addToBinderAdapter.submitData(
                newCards = currentCards,
                newSelectedCardIds = selectedCardIds.toSet()
            )

            renderSelectedCount(
                selectedCountTextView = selectedCountTextView,
                selectedCardIds = selectedCardIds,
                maxSelectable = addToBinderViewModel.maxSelectable.value ?: 0
            )

            addButton.isEnabled = selectedCardIds.isNotEmpty()
        }

        addToBinderViewModel.cardsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> {
                    cardResultsRecyclerView.visibility = View.GONE
                    loadingView.visibility = View.VISIBLE
                    emptyStateView.visibility = View.GONE
                }

                is UiState.Success -> {
                    loadingView.visibility = View.GONE
                    addToBinderAdapter.submitData(
                        newCards = state.data,
                        newSelectedCardIds = addToBinderViewModel.selectedCardIds.value.orEmpty().toSet()
                    )

                    if (state.data.isEmpty()) {
                        cardResultsRecyclerView.visibility = View.GONE
                        emptyStateView.visibility = View.VISIBLE

                        if (hasActiveFilters()) {
                            emptyTitleTextView.text = "No matching cards"
                            emptyMessageTextView.text = "Try clearing one or more filters."
                        } else {
                            emptyTitleTextView.text = "No saved cards"
                            emptyMessageTextView.text = "Save some cards first, then add them to your binder."
                        }
                    } else {
                        cardResultsRecyclerView.visibility = View.VISIBLE
                        emptyStateView.visibility = View.GONE
                    }
                }

                is UiState.Error -> {
                    cardResultsRecyclerView.visibility = View.GONE
                    loadingView.visibility = View.GONE
                    emptyStateView.visibility = View.VISIBLE
                    emptyTitleTextView.text = "Unable to load cards"
                    emptyMessageTextView.text = state.message
                }
            }
        }

        addToBinderViewModel.setMaxSelectable(maxSelectable)
        addToBinderViewModel.loadSavedCards()
        addButton.isEnabled = false
    }

    private fun handleCardTapped(card: PokemonCard) {
        val message = addToBinderViewModel.toggleCardSelection(card.cardId)
        if (message != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderSelectedCount(
        selectedCountTextView: TextView,
        selectedCardIds: List<String>,
        maxSelectable: Int
    ) {
        selectedCountTextView.text = "${selectedCardIds.size}/$maxSelectable"
    }

    private fun syncText(textView: TextView, value: String) {
        if (textView.text.toString() != value) {
            textView.text = value
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
        return addToBinderViewModel.nameQuery.value.orEmpty().isNotBlank() ||
                addToBinderViewModel.setQuery.value.orEmpty().isNotBlank() ||
                addToBinderViewModel.typeQuery.value.orEmpty().isNotBlank() ||
                addToBinderViewModel.cardIdQuery.value.orEmpty().isNotBlank()
    }

    companion object {
        const val TAG = "AddToBinderDialog"
        const val REQUEST_KEY = "addToBinderResult"
        const val RESULT_START_SLOT_INDEX = "resultStartSlotIndex"
        const val RESULT_SELECTED_CARD_IDS = "resultSelectedCardIds"

        private const val ARG_START_SLOT_INDEX = "argStartSlotIndex"
        private const val ARG_MAX_SELECTABLE = "argMaxSelectable"

        fun newInstance(
            startSlotIndex: Int,
            maxSelectable: Int
        ): AddToBinderDialogFragment {
            return AddToBinderDialogFragment().apply {
                arguments = bundleOf(
                    ARG_START_SLOT_INDEX to startSlotIndex,
                    ARG_MAX_SELECTABLE to maxSelectable
                )
            }
        }
    }
}