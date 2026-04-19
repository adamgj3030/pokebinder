package com.adamjohnson.pokebinder.ui.cardsearch

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.CardSearchViewModel

class CardSearchFragment : Fragment(R.layout.fragment_card_search) {

    private val cardSearchViewModel: CardSearchViewModel by viewModels()

    private lateinit var cardSearchAdapter: CardSearchAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchEditText: EditText = view.findViewById(R.id.searchEditText)
        val resultCountTextView: TextView = view.findViewById(R.id.resultCountTextView)
        val cardResultsRecyclerView: RecyclerView = view.findViewById(R.id.cardResultsRecyclerView)
        val loadingView: View = view.findViewById(R.id.loadingView)
        val emptyStateView: View = view.findViewById(R.id.emptyStateView)
        val emptyTitleTextView: TextView = view.findViewById(R.id.emptyTitleTextView)
        val emptyMessageTextView: TextView = view.findViewById(R.id.emptyMessageTextView)

        cardSearchAdapter = CardSearchAdapter()

        cardResultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        cardResultsRecyclerView.adapter = cardSearchAdapter

        searchEditText.doAfterTextChanged { editable ->
            val query = editable?.toString().orEmpty()
            if (cardSearchViewModel.searchQuery.value != query) {
                cardSearchViewModel.updateSearchQuery(query)
            }
        }

        cardSearchViewModel.searchQuery.observe(viewLifecycleOwner) { query ->
            if (searchEditText.text.toString() != query) {
                searchEditText.setText(query)
                searchEditText.setSelection(query.length)
            }
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
        cardSearchAdapter.submitList(cards)
        resultCountTextView.text = getString(R.string.card_search_results_label, cards.size)
        loadingView.visibility = View.GONE

        if (cards.isEmpty()) {
            cardResultsRecyclerView.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
            emptyTitleTextView.text = getString(R.string.card_search_empty_title)
            emptyMessageTextView.text = getString(R.string.card_search_empty_message)
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
        cardSearchAdapter.submitList(emptyList())
        resultCountTextView.text = getString(R.string.card_search_results_default)
        cardResultsRecyclerView.visibility = View.GONE
        loadingView.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
        emptyTitleTextView.text = getString(R.string.card_search_error_title)
        emptyMessageTextView.text = message
    }
}