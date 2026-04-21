package com.adamjohnson.pokebinder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.adamjohnson.pokebinder.data.local.PokemonCardLocalDataSource
import com.adamjohnson.pokebinder.data.model.CardFilter
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.adamjohnson.pokebinder.data.repository.CardRepository
import com.adamjohnson.pokebinder.data.repository.CollectionRepository
import com.adamjohnson.pokebinder.utils.UiState

class AddToBinderViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val collectionRepository = CollectionRepository()
    private val cardRepository = CardRepository(
        PokemonCardLocalDataSource(application.applicationContext)
    )

    private var allSavedCards: List<PokemonCard> = emptyList()
    private val selectedCardIdsInOrder = mutableListOf<String>()
    private var maxSelectableCount = 0

    private val _cardsState = MutableLiveData<UiState<List<PokemonCard>>>(UiState.Loading)
    val cardsState: LiveData<UiState<List<PokemonCard>>> = _cardsState

    private val _selectedCardIds = MutableLiveData<List<String>>(emptyList())
    val selectedCardIds: LiveData<List<String>> = _selectedCardIds

    private val _maxSelectable = MutableLiveData(0)
    val maxSelectable: LiveData<Int> = _maxSelectable

    private val _nameQuery = MutableLiveData("")
    val nameQuery: LiveData<String> = _nameQuery

    private val _setQuery = MutableLiveData("")
    val setQuery: LiveData<String> = _setQuery

    private val _typeQuery = MutableLiveData("")
    val typeQuery: LiveData<String> = _typeQuery

    private val _cardIdQuery = MutableLiveData("")
    val cardIdQuery: LiveData<String> = _cardIdQuery

    private val _subtypeQuery = MutableLiveData("")
    val subtypeQuery: LiveData<String> = _subtypeQuery

    private val _rarityQuery = MutableLiveData("")
    val rarityQuery: LiveData<String> = _rarityQuery

    private val _fullArtOnly = MutableLiveData(false)
    val fullArtOnly: LiveData<Boolean> = _fullArtOnly

    private val _nameSuggestions = MutableLiveData<List<String>>(emptyList())
    val nameSuggestions: LiveData<List<String>> = _nameSuggestions

    private val _setSuggestions = MutableLiveData<List<String>>(emptyList())
    val setSuggestions: LiveData<List<String>> = _setSuggestions

    private val _typeSuggestions = MutableLiveData<List<String>>(emptyList())
    val typeSuggestions: LiveData<List<String>> = _typeSuggestions

    private val _subtypeSuggestions = MutableLiveData<List<String>>(emptyList())
    val subtypeSuggestions: LiveData<List<String>> = _subtypeSuggestions

    private val _raritySuggestions = MutableLiveData<List<String>>(emptyList())
    val raritySuggestions: LiveData<List<String>> = _raritySuggestions

    fun setMaxSelectable(maxSelectable: Int) {
        maxSelectableCount = maxOf(0, maxSelectable)
        _maxSelectable.value = maxSelectableCount

        while (selectedCardIdsInOrder.size > maxSelectableCount) {
            selectedCardIdsInOrder.removeLast()
        }

        _selectedCardIds.value = selectedCardIdsInOrder.toList()
    }

    fun loadSavedCards() {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            allSavedCards = emptyList()
            _cardsState.value = UiState.Error("You must be signed in to load saved cards.")
            clearSuggestions()
            return
        }

        _cardsState.value = UiState.Loading

        collectionRepository.getSavedCards(userId) { savedCards, errorMessage ->
            if (savedCards != null) {
                try {
                    allSavedCards = savedCards.mapNotNull { savedCard ->
                        cardRepository.getPokemonCard(savedCard.cardId)
                    }
                    publishFilteredCards()
                } catch (exception: Exception) {
                    allSavedCards = emptyList()
                    _cardsState.postValue(UiState.Error("Unable to load saved cards."))
                    clearSuggestions()
                }
            } else {
                allSavedCards = emptyList()
                _cardsState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load saved cards.")
                )
                clearSuggestions()
            }
        }
    }

    fun updateNameQuery(query: String) {
        _nameQuery.value = query
        publishFilteredCards()
    }

    fun updateSetQuery(query: String) {
        _setQuery.value = query
        publishFilteredCards()
    }

    fun updateTypeQuery(query: String) {
        _typeQuery.value = query
        publishFilteredCards()
    }

    fun updateCardIdQuery(query: String) {
        _cardIdQuery.value = query
        publishFilteredCards()
    }

    fun updateSubtypeQuery(query: String) {
        _subtypeQuery.value = query
        publishFilteredCards()
    }

    fun updateRarityQuery(query: String) {
        _rarityQuery.value = query
        publishFilteredCards()
    }

    fun updateFullArtOnly(enabled: Boolean) {
        _fullArtOnly.value = enabled
        publishFilteredCards()
    }

    fun clearFilters() {
        _nameQuery.value = ""
        _setQuery.value = ""
        _typeQuery.value = ""
        _cardIdQuery.value = ""
        _subtypeQuery.value = ""
        _rarityQuery.value = ""
        _fullArtOnly.value = false
        publishFilteredCards()
    }

    fun toggleCardSelection(cardId: String): String? {
        if (cardId.isBlank()) {
            return "Invalid card."
        }

        if (selectedCardIdsInOrder.contains(cardId)) {
            selectedCardIdsInOrder.remove(cardId)
            _selectedCardIds.value = selectedCardIdsInOrder.toList()
            return null
        }

        if (selectedCardIdsInOrder.size >= maxSelectableCount) {
            return "You can only add up to $maxSelectableCount cards."
        }

        selectedCardIdsInOrder.add(cardId)
        _selectedCardIds.value = selectedCardIdsInOrder.toList()
        return null
    }

    fun getSelectedCardIds(): List<String> {
        return selectedCardIdsInOrder.toList()
    }

    private fun publishFilteredCards() {
        val filteredCards = cardRepository.filterPokemonCards(
            filter = currentFilter(),
            cards = allSavedCards
        )

        _cardsState.value = UiState.Success(filteredCards)
        refreshSuggestions()
    }

    private fun currentFilter(): CardFilter {
        return CardFilter(
            nameQuery = _nameQuery.value.orEmpty(),
            setQuery = _setQuery.value.orEmpty(),
            typeQuery = _typeQuery.value.orEmpty(),
            cardIdQuery = _cardIdQuery.value.orEmpty(),
            subtypeQuery = _subtypeQuery.value.orEmpty(),
            rarityQuery = _rarityQuery.value.orEmpty(),
            fullArtOnly = _fullArtOnly.value ?: false
        )
    }

    private fun refreshSuggestions() {
        _nameSuggestions.value = cardRepository.getNameSuggestions(
            query = _nameQuery.value.orEmpty(),
            cards = allSavedCards
        )

        _setSuggestions.value = cardRepository.getSetSuggestions(
            query = _setQuery.value.orEmpty(),
            cards = allSavedCards
        )

        _typeSuggestions.value = cardRepository.getTypeSuggestions(
            query = _typeQuery.value.orEmpty(),
            cards = allSavedCards
        )

        _subtypeSuggestions.value = cardRepository.getSubtypeSuggestions(
            query = _subtypeQuery.value.orEmpty(),
            cards = allSavedCards
        )

        _raritySuggestions.value = cardRepository.getRaritySuggestions(
            query = _rarityQuery.value.orEmpty(),
            cards = allSavedCards
        )
    }

    private fun clearSuggestions() {
        _nameSuggestions.value = emptyList()
        _setSuggestions.value = emptyList()
        _typeSuggestions.value = emptyList()
        _subtypeSuggestions.value = emptyList()
        _raritySuggestions.value = emptyList()
    }
}