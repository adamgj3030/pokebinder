package com.adamjohnson.pokebinder.ui.collection

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adamjohnson.pokebinder.R
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.adamjohnson.pokebinder.utils.UiState
import com.adamjohnson.pokebinder.viewmodel.CollectionViewModel

class CollectionFragment : Fragment(R.layout.fragment_collection) {

    private val collectionViewModel: CollectionViewModel by viewModels()

    private lateinit var collectionAdapter: CollectionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val addCardsButton: Button = view.findViewById(R.id.addCardsButton)
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

        collectionAdapter = CollectionAdapter { card ->
            collectionViewModel.removeCard(card.cardId) { errorMessage ->
                if (errorMessage != null && isAdded) {
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }

        collectionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        collectionRecyclerView.adapter = collectionAdapter

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
    }

    override fun onResume() {
        super.onResume()
        collectionViewModel.loadCollection()
    }

    private fun openCardSearch() {
        if (findNavController().currentDestination?.id == R.id.collectionFragment) {
            findNavController().navigate(R.id.cardSearchFragment)
        }
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
        collectionAdapter.submitList(cards)
        resultCountTextView.text = getString(R.string.collection_results_label, cards.size)
        loadingView.visibility = View.GONE

        if (cards.isEmpty()) {
            collectionRecyclerView.visibility = View.GONE
            emptyStateView.visibility = View.VISIBLE
            emptyTitleTextView.text = getString(R.string.collection_empty_title)
            emptyMessageTextView.text = getString(R.string.collection_empty_message)
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
        collectionAdapter.submitList(emptyList())
        resultCountTextView.text = getString(R.string.collection_results_default)
        collectionRecyclerView.visibility = View.GONE
        loadingView.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
        emptyTitleTextView.text = getString(R.string.collection_error_title)
        emptyMessageTextView.text = message
    }
}