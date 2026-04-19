package com.adamjohnson.pokebinder.ui.detail

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.local.PokemonCardLocalDataSource
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.repository.CardRepository
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.PageDetailViewModel
import java.text.DateFormat
import java.util.Date

class PageDetailFragment : Fragment(R.layout.fragment_page_detail) {

    private val pageDetailViewModel: PageDetailViewModel by viewModels()

    private val cardRepository by lazy {
        CardRepository(PokemonCardLocalDataSource(requireContext().applicationContext))
    }

    private lateinit var slotViews: List<TextView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val contentScrollView: View = view.findViewById(R.id.contentScrollView)
        val pageTitleTextView: TextView = view.findViewById(R.id.pageTitleTextView)
        val pageStatusTextView: TextView = view.findViewById(R.id.pageStatusTextView)
        val filledSlotsTextView: TextView = view.findViewById(R.id.filledSlotsTextView)
        val publishedAtTextView: TextView = view.findViewById(R.id.publishedAtTextView)
        val pageMessageTextView: TextView = view.findViewById(R.id.pageMessageTextView)
        val loadingView: View = view.findViewById(R.id.loadingView)

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

        pageDetailViewModel.pageState.observe(viewLifecycleOwner) { state ->
            when (state) {
                UiState.Loading -> {
                    renderLoading(
                        contentScrollView = contentScrollView,
                        pageMessageTextView = pageMessageTextView,
                        loadingView = loadingView
                    )
                }

                is UiState.Success -> {
                    renderSuccess(
                        binderPage = state.data,
                        contentScrollView = contentScrollView,
                        pageTitleTextView = pageTitleTextView,
                        pageStatusTextView = pageStatusTextView,
                        filledSlotsTextView = filledSlotsTextView,
                        publishedAtTextView = publishedAtTextView,
                        pageMessageTextView = pageMessageTextView,
                        loadingView = loadingView
                    )
                }

                is UiState.Error -> {
                    renderError(
                        message = state.message,
                        contentScrollView = contentScrollView,
                        pageMessageTextView = pageMessageTextView,
                        loadingView = loadingView
                    )
                }
            }
        }

        val pageId = arguments?.getString(ARG_PAGE_ID).orEmpty()
        pageDetailViewModel.loadPage(pageId)
    }

    private fun renderLoading(
        contentScrollView: View,
        pageMessageTextView: TextView,
        loadingView: View
    ) {
        contentScrollView.visibility = View.GONE
        pageMessageTextView.visibility = View.GONE
        loadingView.visibility = View.VISIBLE
    }

    private fun renderSuccess(
        binderPage: BinderPage,
        contentScrollView: View,
        pageTitleTextView: TextView,
        pageStatusTextView: TextView,
        filledSlotsTextView: TextView,
        publishedAtTextView: TextView,
        pageMessageTextView: TextView,
        loadingView: View
    ) {
        loadingView.visibility = View.GONE
        contentScrollView.visibility = View.VISIBLE
        pageMessageTextView.visibility = View.GONE

        pageTitleTextView.text = binderPage.title.ifBlank {
            getString(R.string.page_detail_untitled)
        }

        pageStatusTextView.text = getString(R.string.page_detail_status_published)
        filledSlotsTextView.text = getString(
            R.string.page_detail_filled_slots,
            getFilledSlotCount(binderPage)
        )

        val publishedAt = binderPage.publishedAt ?: 0L
        if (publishedAt > 0L) {
            val formattedPublishedAt = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT
            ).format(Date(publishedAt))

            publishedAtTextView.text = getString(
                R.string.page_detail_published_at,
                formattedPublishedAt
            )
            publishedAtTextView.visibility = View.VISIBLE
        } else {
            publishedAtTextView.text = ""
            publishedAtTextView.visibility = View.GONE
        }

        renderSlotViews(binderPage)
    }

    private fun renderError(
        message: String,
        contentScrollView: View,
        pageMessageTextView: TextView,
        loadingView: View
    ) {
        loadingView.visibility = View.GONE
        contentScrollView.visibility = View.GONE
        pageMessageTextView.text = message
        pageMessageTextView.visibility = View.VISIBLE
        renderSlotViews(null)
    }

    private fun renderSlotViews(binderPage: BinderPage?) {
        slotViews.forEachIndexed { index, slotView ->
            val cardId = binderPage?.slots?.getOrNull(index)?.cardId
            val cardText = getSlotCardDisplayText(cardId)

            slotView.text = getString(R.string.page_detail_slot_label, index + 1, cardText)
            slotView.setBackgroundResource(android.R.color.darker_gray)
        }
    }

    private fun getSlotCardDisplayText(cardId: String?): String {
        if (cardId.isNullOrBlank()) {
            return getString(R.string.page_detail_slot_empty)
        }

        val pokemonCard = cardRepository.getPokemonCard(cardId)
        return if (pokemonCard != null) {
            getString(
                R.string.page_detail_slot_card_info,
                pokemonCard.name,
                pokemonCard.setName
            )
        } else {
            cardId
        }
    }

    private fun getFilledSlotCount(binderPage: BinderPage): Int {
        return binderPage.slots.count { slot ->
            !slot.cardId.isNullOrBlank()
        }
    }

    companion object {
        const val ARG_PAGE_ID = "pageId"
    }
}