package com.adamjohnson.pokebinder.ui.binder

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.local.PokemonCardLocalDataSource
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.repository.CardRepository
import com.adamjohnson.pokebinder.ui.detail.PageDetailFragment
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.BinderEditorViewModel
import com.bumptech.glide.Glide

class BinderEditorFragment : Fragment(R.layout.fragment_binder_editor) {

    private val binderEditorViewModel: BinderEditorViewModel by viewModels()

    private val cardRepository by lazy {
        CardRepository(PokemonCardLocalDataSource(requireContext().applicationContext))
    }

    private lateinit var slotContainerViews: List<View>
    private lateinit var slotImageViews: List<ImageView>
    private lateinit var slotPlusTextViews: List<TextView>
    private lateinit var slotRemoveTextViews: List<TextView>

    private var currentBinderPage: BinderPage? = null
    private var currentCanPublish = false
    private var isPageLoading = false
    private var selectedFilledSlotIndex: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pageTitleEditText: EditText = view.findViewById(R.id.pageTitleEditText)
        val cancelButton: Button = view.findViewById(R.id.cancelButton)
        val saveButton: Button = view.findViewById(R.id.saveButton)
        val publishButton: Button = view.findViewById(R.id.publishButton)
        val editorMessageTextView: TextView = view.findViewById(R.id.editorMessageTextView)
        val pageLoadingView: View = view.findViewById(R.id.pageLoadingView)

        slotContainerViews = listOf(
            view.findViewById(R.id.slot1Container),
            view.findViewById(R.id.slot2Container),
            view.findViewById(R.id.slot3Container),
            view.findViewById(R.id.slot4Container),
            view.findViewById(R.id.slot5Container),
            view.findViewById(R.id.slot6Container),
            view.findViewById(R.id.slot7Container),
            view.findViewById(R.id.slot8Container),
            view.findViewById(R.id.slot9Container)
        )

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

        slotPlusTextViews = listOf(
            view.findViewById(R.id.slot1PlusTextView),
            view.findViewById(R.id.slot2PlusTextView),
            view.findViewById(R.id.slot3PlusTextView),
            view.findViewById(R.id.slot4PlusTextView),
            view.findViewById(R.id.slot5PlusTextView),
            view.findViewById(R.id.slot6PlusTextView),
            view.findViewById(R.id.slot7PlusTextView),
            view.findViewById(R.id.slot8PlusTextView),
            view.findViewById(R.id.slot9PlusTextView)
        )

        slotRemoveTextViews = listOf(
            view.findViewById(R.id.slot1RemoveTextView),
            view.findViewById(R.id.slot2RemoveTextView),
            view.findViewById(R.id.slot3RemoveTextView),
            view.findViewById(R.id.slot4RemoveTextView),
            view.findViewById(R.id.slot5RemoveTextView),
            view.findViewById(R.id.slot6RemoveTextView),
            view.findViewById(R.id.slot7RemoveTextView),
            view.findViewById(R.id.slot8RemoveTextView),
            view.findViewById(R.id.slot9RemoveTextView)
        )

        parentFragmentManager.setFragmentResultListener(
            AddToBinderDialogFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, result ->
            val startSlotIndex = result.getInt(AddToBinderDialogFragment.RESULT_START_SLOT_INDEX, 0)
            val selectedCardIds =
                result.getStringArrayList(AddToBinderDialogFragment.RESULT_SELECTED_CARD_IDS)
                    .orEmpty()

            if (selectedCardIds.isNotEmpty()) {
                binderEditorViewModel.addCardsFromSlot(startSlotIndex, selectedCardIds)
            }
        }

        slotContainerViews.forEachIndexed { index, slotView ->
            slotView.setOnClickListener {
                handleSlotClick(index)
            }
        }

        slotRemoveTextViews.forEachIndexed { index, removeView ->
            removeView.setOnClickListener {
                if (!isCurrentPageEditable()) {
                    showShortToast("Published binders are read-only.")
                    return@setOnClickListener
                }

                binderEditorViewModel.removeCardFromSlot(index)
                selectedFilledSlotIndex = null
                renderSlotViews()
            }
        }

        pageTitleEditText.doAfterTextChanged { editable ->
            val updatedTitle = editable?.toString().orEmpty()
            val currentTitle = currentBinderPage?.title.orEmpty()

            if (updatedTitle != currentTitle) {
                binderEditorViewModel.updateTitle(updatedTitle)
            }
        }

        cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }

        saveButton.setOnClickListener {
            if (!isCurrentPageEditable()) {
                showShortToast("Published binders are read-only.")
                return@setOnClickListener
            }

            binderEditorViewModel.saveCurrentDraft { _, errorMessage ->
                if (!isAdded) {
                    return@saveCurrentDraft
                }

                showShortToast(errorMessage ?: "Draft saved.")
            }
        }

        publishButton.setOnClickListener {
            if (!isCurrentPageEditable()) {
                showShortToast("Published binders are read-only.")
                return@setOnClickListener
            }

            binderEditorViewModel.publishCurrentPage { publishedBinderPage, errorMessage ->
                if (!isAdded || findNavController().currentDestination?.id != R.id.binderEditorFragment) {
                    return@publishCurrentPage
                }

                if (publishedBinderPage != null && publishedBinderPage.pageId.isNotBlank()) {
                    showShortToast("Binder published.")
                    openPublishedPage(publishedBinderPage.pageId)
                } else {
                    showShortToast(errorMessage ?: "Unable to publish binder.")
                }
            }
        }

        binderEditorViewModel.binderPageState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> {
                    isPageLoading = true
                    pageLoadingView.visibility = View.VISIBLE
                    renderEditorMessage(editorMessageTextView, null)
                    renderEditorControls(
                        pageTitleEditText = pageTitleEditText,
                        saveButton = saveButton,
                        publishButton = publishButton
                    )
                }

                is UiState.Success -> {
                    isPageLoading = false
                    currentBinderPage = state.data
                    pageLoadingView.visibility = View.GONE
                    renderEditorMessage(editorMessageTextView, null)

                    if (pageTitleEditText.text.toString() != state.data.title) {
                        pageTitleEditText.setText(state.data.title)
                        pageTitleEditText.setSelection(state.data.title.length)
                    }

                    if (state.data.status != com.adamjohnson.pokebinder.data.model.BinderPageStatus.DRAFT) {
                        selectedFilledSlotIndex = null
                    }

                    renderSlotViews()
                    renderEditorControls(
                        pageTitleEditText = pageTitleEditText,
                        saveButton = saveButton,
                        publishButton = publishButton
                    )
                }

                is UiState.Error -> {
                    isPageLoading = false
                    pageLoadingView.visibility = View.GONE
                    renderEditorMessage(editorMessageTextView, state.message)
                    renderSlotViews()
                    renderEditorControls(
                        pageTitleEditText = pageTitleEditText,
                        saveButton = saveButton,
                        publishButton = publishButton
                    )
                }
            }
        }

        binderEditorViewModel.canPublish.observe(viewLifecycleOwner) { canPublish ->
            currentCanPublish = canPublish
            renderEditorControls(
                pageTitleEditText = pageTitleEditText,
                saveButton = saveButton,
                publishButton = publishButton
            )
        }

        binderEditorViewModel.initializeEditor(arguments?.getString(ARG_PAGE_ID))
    }

    private fun handleSlotClick(slotIndex: Int) {
        val binderPage = currentBinderPage ?: return

        if (!isCurrentPageEditable()) {
            showShortToast("Published binders are read-only.")
            return
        }

        val cardId = binderPage.slots.getOrNull(slotIndex)?.cardId
        if (cardId.isNullOrBlank()) {
            selectedFilledSlotIndex = null
            val maxSelectable = binderEditorViewModel.getEmptySlotCount()

            if (maxSelectable <= 0) {
                showShortToast("No empty slots remaining.")
                return
            }

            AddToBinderDialogFragment.newInstance(
                startSlotIndex = slotIndex,
                maxSelectable = maxSelectable
            ).show(parentFragmentManager, AddToBinderDialogFragment.TAG)
        } else {
            selectedFilledSlotIndex = if (selectedFilledSlotIndex == slotIndex) {
                null
            } else {
                slotIndex
            }
            renderSlotViews()
        }
    }

    private fun renderSlotViews() {
        val binderPage = currentBinderPage

        slotContainerViews.forEachIndexed { index, containerView ->
            val imageView = slotImageViews[index]
            val plusTextView = slotPlusTextViews[index]
            val removeTextView = slotRemoveTextViews[index]

            val cardId = binderPage?.slots?.getOrNull(index)?.cardId
            val pokemonCard = if (cardId.isNullOrBlank()) {
                null
            } else {
                cardRepository.getPokemonCard(cardId)
            }

            if (pokemonCard == null) {
                Glide.with(this).clear(imageView)
                imageView.visibility = View.GONE
                plusTextView.visibility = View.VISIBLE
            } else {
                imageView.visibility = View.VISIBLE
                plusTextView.visibility = View.GONE

                Glide.with(this)
                    .load(pokemonCard.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(imageView)
            }

            val isSelectedFilledSlot =
                selectedFilledSlotIndex == index &&
                        pokemonCard != null &&
                        isCurrentPageEditable()

            removeTextView.visibility = if (isSelectedFilledSlot) {
                View.VISIBLE
            } else {
                View.GONE
            }

            containerView.alpha = if (isSelectedFilledSlot) 0.82f else 1f
        }
    }

    private fun renderEditorControls(
        pageTitleEditText: EditText,
        saveButton: Button,
        publishButton: Button
    ) {
        val isEditable = isCurrentPageEditable() && !isPageLoading

        pageTitleEditText.isEnabled = isEditable
        saveButton.isEnabled = isEditable
        publishButton.isEnabled = isEditable && currentCanPublish
    }

    private fun renderEditorMessage(
        editorMessageTextView: TextView,
        message: String?
    ) {
        if (message.isNullOrBlank()) {
            editorMessageTextView.text = ""
            editorMessageTextView.visibility = View.GONE
        } else {
            editorMessageTextView.text = message
            editorMessageTextView.visibility = View.VISIBLE
        }
    }

    private fun isCurrentPageEditable(): Boolean {
        return currentBinderPage?.status == com.adamjohnson.pokebinder.data.model.BinderPageStatus.DRAFT
    }

    private fun openPublishedPage(pageId: String) {
        if (findNavController().currentDestination?.id != R.id.binderEditorFragment) {
            return
        }

        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.binderEditorFragment, true)
            .build()

        findNavController().navigate(
            R.id.pageDetailFragment,
            bundleOf(PageDetailFragment.ARG_PAGE_ID to pageId),
            navOptions
        )
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