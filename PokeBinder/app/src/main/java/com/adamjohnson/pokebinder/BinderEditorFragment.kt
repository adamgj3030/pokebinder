package com.adamjohnson.pokebinder.ui.binder

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.BinderEditorViewModel

class BinderEditorFragment : Fragment(R.layout.fragment_binder_editor) {

    private val binderEditorViewModel: BinderEditorViewModel by viewModels()

    private lateinit var cardPickerAdapter: BinderCardPickerAdapter
    private lateinit var slotViews: List<TextView>

    private var currentBinderPage: BinderPage? = null
    private var currentAvailableCards: List<PokemonCard> = emptyList()
    private var currentSelectedSlotIndex: Int? = null
    private var isPageLoading = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pageTitleEditText: EditText = view.findViewById(R.id.pageTitleEditText)
        val selectedSlotTextView: TextView = view.findViewById(R.id.selectedSlotTextView)
        val editorMessageTextView: TextView = view.findViewById(R.id.editorMessageTextView)
        val clearSelectedSlotButton: Button = view.findViewById(R.id.clearSelectedSlotButton)
        val saveDraftButton: Button = view.findViewById(R.id.saveDraftButton)
        val viewDraftsButton: Button = view.findViewById(R.id.viewDraftsButton)
        val cardPickerRecyclerView: RecyclerView = view.findViewById(R.id.cardPickerRecyclerView)
        val cardPickerLoadingView: View = view.findViewById(R.id.cardPickerLoadingView)
        val cardPickerEmptyStateView: View = view.findViewById(R.id.cardPickerEmptyStateView)
        val cardPickerEmptyTitleTextView: TextView =
            view.findViewById(R.id.cardPickerEmptyTitleTextView)
        val cardPickerEmptyMessageTextView: TextView =
            view.findViewById(R.id.cardPickerEmptyMessageTextView)
        val pageLoadingView: View = view.findViewById(R.id.pageLoadingView)

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

        cardPickerAdapter = BinderCardPickerAdapter { card ->
            if (currentSelectedSlotIndex == null) {
                showShortToast(getString(R.string.binder_editor_select_slot_first))
            } else {
                binderEditorViewModel.assignCardToSelectedSlot(card.cardId)
            }
        }

        cardPickerRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        cardPickerRecyclerView.adapter = cardPickerAdapter

        slotViews.forEachIndexed { index, slotView ->
            slotView.setOnClickListener {
                binderEditorViewModel.selectSlot(index)
            }
        }

        pageTitleEditText.doAfterTextChanged { editable ->
            val updatedTitle = editable?.toString().orEmpty()
            val currentTitle = currentBinderPage?.title.orEmpty()

            if (updatedTitle != currentTitle) {
                binderEditorViewModel.updateTitle(updatedTitle)
            }
        }

        clearSelectedSlotButton.setOnClickListener {
            if (currentSelectedSlotIndex == null) {
                showShortToast(getString(R.string.binder_editor_select_slot_first))
            } else {
                binderEditorViewModel.clearSelectedSlot()
            }
        }

        saveDraftButton.setOnClickListener {
            binderEditorViewModel.saveCurrentDraft { _, errorMessage ->
                if (!isAdded) {
                    return@saveCurrentDraft
                }

                val message = errorMessage ?: getString(R.string.binder_editor_save_success)
                showShortToast(message)
            }
        }

        viewDraftsButton.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.binderEditorFragment) {
                findNavController().navigate(R.id.draftsFragment)
            }
        }

        binderEditorViewModel.binderPageState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> {
                    isPageLoading = true
                    pageLoadingView.visibility = View.VISIBLE
                    editorMessageTextView.visibility = View.GONE
                    renderEditorControls(
                        pageTitleEditText = pageTitleEditText,
                        clearSelectedSlotButton = clearSelectedSlotButton,
                        saveDraftButton = saveDraftButton,
                        viewDraftsButton = viewDraftsButton
                    )
                }

                is UiState.Success -> {
                    isPageLoading = false
                    currentBinderPage = state.data
                    pageLoadingView.visibility = View.GONE
                    editorMessageTextView.visibility = View.GONE

                    if (pageTitleEditText.text.toString() != state.data.title) {
                        pageTitleEditText.setText(state.data.title)
                        pageTitleEditText.setSelection(state.data.title.length)
                    }

                    renderSlotViews()
                    renderSelectedSlotFeedback(selectedSlotTextView)
                    renderEditorControls(
                        pageTitleEditText = pageTitleEditText,
                        clearSelectedSlotButton = clearSelectedSlotButton,
                        saveDraftButton = saveDraftButton,
                        viewDraftsButton = viewDraftsButton
                    )
                }

                is UiState.Error -> {
                    isPageLoading = false
                    pageLoadingView.visibility = View.GONE
                    editorMessageTextView.text = state.message
                    editorMessageTextView.visibility = View.VISIBLE

                    renderSlotViews()
                    renderSelectedSlotFeedback(selectedSlotTextView)
                    renderEditorControls(
                        pageTitleEditText = pageTitleEditText,
                        clearSelectedSlotButton = clearSelectedSlotButton,
                        saveDraftButton = saveDraftButton,
                        viewDraftsButton = viewDraftsButton
                    )
                }
            }
        }

        binderEditorViewModel.availableCardsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> {
                    renderCardPickerLoading(
                        cardPickerRecyclerView = cardPickerRecyclerView,
                        cardPickerLoadingView = cardPickerLoadingView,
                        cardPickerEmptyStateView = cardPickerEmptyStateView
                    )
                }

                is UiState.Success -> {
                    currentAvailableCards = state.data
                    renderCardPickerSuccess(
                        cards = state.data,
                        cardPickerRecyclerView = cardPickerRecyclerView,
                        cardPickerLoadingView = cardPickerLoadingView,
                        cardPickerEmptyStateView = cardPickerEmptyStateView,
                        cardPickerEmptyTitleTextView = cardPickerEmptyTitleTextView,
                        cardPickerEmptyMessageTextView = cardPickerEmptyMessageTextView
                    )
                    renderSlotViews()
                    renderSelectedSlotFeedback(selectedSlotTextView)
                }

                is UiState.Error -> {
                    currentAvailableCards = emptyList()
                    renderCardPickerError(
                        message = state.message,
                        cardPickerRecyclerView = cardPickerRecyclerView,
                        cardPickerLoadingView = cardPickerLoadingView,
                        cardPickerEmptyStateView = cardPickerEmptyStateView,
                        cardPickerEmptyTitleTextView = cardPickerEmptyTitleTextView,
                        cardPickerEmptyMessageTextView = cardPickerEmptyMessageTextView
                    )
                    renderSlotViews()
                    renderSelectedSlotFeedback(selectedSlotTextView)
                }
            }
        }

        binderEditorViewModel.selectedSlotIndex.observe(viewLifecycleOwner) { selectedSlotIndex ->
            currentSelectedSlotIndex = selectedSlotIndex
            renderSlotViews()
            renderSelectedSlotFeedback(selectedSlotTextView)
            renderEditorControls(
                pageTitleEditText = pageTitleEditText,
                clearSelectedSlotButton = clearSelectedSlotButton,
                saveDraftButton = saveDraftButton,
                viewDraftsButton = viewDraftsButton
            )
        }

        renderSlotViews()
        renderSelectedSlotFeedback(selectedSlotTextView)
        renderEditorControls(
            pageTitleEditText = pageTitleEditText,
            clearSelectedSlotButton = clearSelectedSlotButton,
            saveDraftButton = saveDraftButton,
            viewDraftsButton = viewDraftsButton
        )

        val pageId = arguments?.getString(ARG_PAGE_ID)
        binderEditorViewModel.initializeEditor(pageId)
    }

    private fun renderEditorControls(
        pageTitleEditText: EditText,
        clearSelectedSlotButton: Button,
        saveDraftButton: Button,
        viewDraftsButton: Button
    ) {
        val hasLoadedPage = !currentBinderPage?.pageId.isNullOrBlank()
        val controlsEnabled = !isPageLoading && hasLoadedPage

        pageTitleEditText.isEnabled = controlsEnabled
        setSlotViewsEnabled(controlsEnabled)

        clearSelectedSlotButton.isEnabled = controlsEnabled && currentSelectedSlotIndex != null
        saveDraftButton.isEnabled = controlsEnabled
        viewDraftsButton.isEnabled = !isPageLoading
    }

    private fun setSlotViewsEnabled(isEnabled: Boolean) {
        slotViews.forEach { slotView ->
            slotView.isEnabled = isEnabled
        }
    }

    private fun renderSlotViews() {
        val binderPage = currentBinderPage

        slotViews.forEachIndexed { index, slotView ->
            val slot = binderPage?.slots?.getOrNull(index)
            val cardText = if (slot?.cardId.isNullOrBlank()) {
                getString(R.string.binder_editor_slot_empty)
            } else {
                getSlotCardDisplayName(slot?.cardId)
            }

            slotView.text = getString(R.string.binder_editor_slot_label, index + 1, cardText)

            if (currentSelectedSlotIndex == index) {
                slotView.setBackgroundResource(android.R.color.holo_blue_light)
            } else {
                slotView.setBackgroundResource(android.R.color.darker_gray)
            }
        }
    }

    private fun renderSelectedSlotFeedback(selectedSlotTextView: TextView) {
        val selectedIndex = currentSelectedSlotIndex

        if (selectedIndex == null) {
            selectedSlotTextView.text = getString(R.string.binder_editor_no_slot_selected)
            return
        }

        val slot = currentBinderPage?.slots?.getOrNull(selectedIndex)
        if (slot?.cardId.isNullOrBlank()) {
            selectedSlotTextView.text = getString(
                R.string.binder_editor_selected_slot_empty_label,
                selectedIndex + 1
            )
        } else {
            selectedSlotTextView.text = getString(
                R.string.binder_editor_selected_slot_filled_label,
                selectedIndex + 1,
                getSlotCardDisplayName(slot?.cardId)
            )
        }
    }

    private fun getSlotCardDisplayName(cardId: String?): String {
        if (cardId.isNullOrBlank()) {
            return getString(R.string.binder_editor_slot_empty)
        }

        return currentAvailableCards.firstOrNull { card ->
            card.cardId == cardId
        }?.name ?: cardId
    }

    private fun renderCardPickerLoading(
        cardPickerRecyclerView: RecyclerView,
        cardPickerLoadingView: View,
        cardPickerEmptyStateView: View
    ) {
        cardPickerRecyclerView.visibility = View.GONE
        cardPickerLoadingView.visibility = View.VISIBLE
        cardPickerEmptyStateView.visibility = View.GONE
    }

    private fun renderCardPickerSuccess(
        cards: List<PokemonCard>,
        cardPickerRecyclerView: RecyclerView,
        cardPickerLoadingView: View,
        cardPickerEmptyStateView: View,
        cardPickerEmptyTitleTextView: TextView,
        cardPickerEmptyMessageTextView: TextView
    ) {
        cardPickerAdapter.submitList(cards)
        cardPickerLoadingView.visibility = View.GONE

        if (cards.isEmpty()) {
            cardPickerRecyclerView.visibility = View.GONE
            cardPickerEmptyStateView.visibility = View.VISIBLE
            cardPickerEmptyTitleTextView.text =
                getString(R.string.binder_editor_picker_empty_title)
            cardPickerEmptyMessageTextView.text =
                getString(R.string.binder_editor_picker_empty_message)
        } else {
            cardPickerRecyclerView.visibility = View.VISIBLE
            cardPickerEmptyStateView.visibility = View.GONE
        }
    }

    private fun renderCardPickerError(
        message: String,
        cardPickerRecyclerView: RecyclerView,
        cardPickerLoadingView: View,
        cardPickerEmptyStateView: View,
        cardPickerEmptyTitleTextView: TextView,
        cardPickerEmptyMessageTextView: TextView
    ) {
        cardPickerAdapter.submitList(emptyList())
        cardPickerRecyclerView.visibility = View.GONE
        cardPickerLoadingView.visibility = View.GONE
        cardPickerEmptyStateView.visibility = View.VISIBLE
        cardPickerEmptyTitleTextView.text =
            getString(R.string.binder_editor_picker_error_title)
        cardPickerEmptyMessageTextView.text = message
    }

    private fun showShortToast(message: String) {
        if (!isAdded) {
            return
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ARG_PAGE_ID = "pageId"
    }
}