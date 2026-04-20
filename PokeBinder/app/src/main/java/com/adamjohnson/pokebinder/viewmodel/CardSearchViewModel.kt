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

    private val _nameQuery = MutableLiveData("")
    val nameQuery: LiveData<String> = _nameQuery

    private val _setQuery = MutableLiveData("")
    val setQuery: LiveData<String> = _setQuery

    private val _typeQuery = MutableLiveData("")
    val typeQuery: LiveData<String> = _typeQuery

    private val _cardIdQuery = MutableLiveData("")
    val cardIdQuery: LiveData<String> = _cardIdQuery

    private val _savedCardIds = MutableLiveData<Set<String>>(emptySet())
    val savedCardIds: LiveData<Set<String>> = _savedCardIds

    private val _nameSuggestions = MutableLiveData<List<String>>(emptyList())
    val nameSuggestions: LiveData<List<String>> = _nameSuggestions

    private val _setSuggestions = MutableLiveData<List<String>>(emptyList())
    val setSuggestions: LiveData<List<String>> = _setSuggestions

    private val _typeSuggestions = MutableLiveData<List<String>>(emptyList())
    val typeSuggestions: LiveData<List<String>> = _typeSuggestions

    init {
        loadCards()
    }

    fun loadCards() {
        _cardsState.value = UiState.Loading
        runSearch()
    }

    fun updateNameQuery(query: String) {
        _nameQuery.value = query
        runSearch()
    }

    fun updateSetQuery(query: String) {
        _setQuery.value = query
        runSearch()
    }

    fun updateTypeQuery(query: String) {
        _typeQuery.value = query
        runSearch()
    }

    fun updateCardIdQuery(query: String) {
        _cardIdQuery.value = query
        runSearch()
    }

    fun clearFilters() {
        _nameQuery.value = ""
        _setQuery.value = ""
        _typeQuery.value = ""
        _cardIdQuery.value = ""
        runSearch()
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

    fun toggleSavedCard(
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

        val currentSavedCardIds = _savedCardIds.value.orEmpty()
        if (currentSavedCardIds.contains(cardId)) {
            collectionRepository.removeCard(userId, cardId) { errorMessage ->
                if (errorMessage == null) {
                    _savedCardIds.postValue(currentSavedCardIds - cardId)
                }

                onResult(errorMessage)
            }
        } else {
            collectionRepository.saveCard(userId, cardId) { errorMessage ->
                if (errorMessage == null) {
                    _savedCardIds.postValue(currentSavedCardIds + cardId)
                }

                onResult(errorMessage)
            }
        }
    }

    private fun runSearch() {
        try {
            val cards = cardRepository.filterPokemonCards(
                nameQuery = _nameQuery.value.orEmpty(),
                setQuery = _setQuery.value.orEmpty(),
                typeQuery = _typeQuery.value.orEmpty(),
                cardIdQuery = _cardIdQuery.value.orEmpty()
            )

            _cardsState.value = UiState.Success(cards)
            refreshSuggestions()
        } catch (exception: Exception) {
            _cardsState.value = UiState.Error("Unable to load cards.")
            clearSuggestions()
        }
    }

    private fun refreshSuggestions() {
        _nameSuggestions.value = cardRepository.getNameSuggestions(_nameQuery.value.orEmpty())
        _setSuggestions.value = cardRepository.getSetSuggestions(_setQuery.value.orEmpty())
        _typeSuggestions.value = cardRepository.getTypeSuggestions(_typeQuery.value.orEmpty())
    }

    private fun clearSuggestions() {
        _nameSuggestions.value = emptyList()
        _setSuggestions.value = emptyList()
        _typeSuggestions.value = emptyList()
    }
}