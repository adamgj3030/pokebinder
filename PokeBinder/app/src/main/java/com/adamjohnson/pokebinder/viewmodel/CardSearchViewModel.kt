package com.adamjohnson.pokebinder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.adamjohnson.pokebinder.data.local.PokemonCardLocalDataSource
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.adamjohnson.pokebinder.data.repository.CardRepository
import com.adamjohnson.pokebinder.utils.UiState

class CardSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val cardRepository = CardRepository(
        PokemonCardLocalDataSource(application.applicationContext)
    )

    private val _cardsState = MutableLiveData<UiState<List<PokemonCard>>>(UiState.Loading)
    val cardsState: LiveData<UiState<List<PokemonCard>>> = _cardsState

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    init {
        loadCards()
    }

    fun loadCards() {
        _cardsState.value = UiState.Loading
        runSearch(_searchQuery.value.orEmpty())
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        runSearch(query)
    }

    fun clearSearch() {
        updateSearchQuery("")
    }

    private fun runSearch(query: String) {
        try {
            val cards = cardRepository.searchPokemonCards(query)
            _cardsState.value = UiState.Success(cards)
        } catch (exception: Exception) {
            _cardsState.value = UiState.Error("Unable to load cards.")
        }
    }
}