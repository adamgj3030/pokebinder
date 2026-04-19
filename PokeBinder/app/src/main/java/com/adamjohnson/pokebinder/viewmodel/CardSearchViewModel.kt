package com.adamjohnson.pokebinder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.adamjohnson.pokebinder.data.local.PokemonCardLocalDataSource
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.adamjohnson.pokebinder.data.repository.CardRepository
import com.adamjohnson.pokebinder.data.repository.CollectionRepository
import com.adamjohnson.pokebinder.utils.UiState

class CardSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val collectionRepository = CollectionRepository()
    private val cardRepository = CardRepository(
        PokemonCardLocalDataSource(application.applicationContext)
    )

    private val _cardsState = MutableLiveData<UiState<List<PokemonCard>>>(UiState.Loading)
    val cardsState: LiveData<UiState<List<PokemonCard>>> = _cardsState

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _savedCardIds = MutableLiveData<Set<String>>(emptySet())
    val savedCardIds: LiveData<Set<String>> = _savedCardIds

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

    fun loadSavedCardIds() {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            _savedCardIds.value = emptySet()
            return
        }

        collectionRepository.getSavedCards(userId) { savedCards, _ ->
            if (savedCards != null) {
                _savedCardIds.postValue(savedCards.map { it.cardId }.toSet())
            } else {
                _savedCardIds.postValue(emptySet())
            }
        }
    }

    fun saveCard(
        cardId: String,
        onResult: (String?) -> Unit = {}
    ) {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            onResult("You must be signed in to save cards.")
            return
        }

        if (cardId.isBlank()) {
            onResult("Invalid card.")
            return
        }

        if (_savedCardIds.value.orEmpty().contains(cardId)) {
            onResult(null)
            return
        }

        collectionRepository.saveCard(userId, cardId) { errorMessage ->
            if (errorMessage == null) {
                val updatedSavedCardIds = _savedCardIds.value.orEmpty() + cardId
                _savedCardIds.postValue(updatedSavedCardIds)
            }

            onResult(errorMessage)
        }
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