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

    private val _nameSuggestions = MutableLiveData<List<String>>(emptyList())
    val nameSuggestions: LiveData<List<String>> = _nameSuggestions

    private val _setSuggestions = MutableLiveData<List<String>>(emptyList())
    val setSuggestions: LiveData<List<String>> = _setSuggestions

    private val _typeSuggestions = MutableLiveData<List<String>>(emptyList())
    val typeSuggestions: LiveData<List<String>> = _typeSuggestions

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

    fun clearFilters() {
        _nameQuery.value = ""
        _setQuery.value = ""
        _typeQuery.value = ""
        _cardIdQuery.value = ""
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
        val filteredCards = filterSavedCards(
            cards = allSavedCards,
            nameQuery = _nameQuery.value.orEmpty(),
            setQuery = _setQuery.value.orEmpty(),
            typeQuery = _typeQuery.value.orEmpty(),
            cardIdQuery = _cardIdQuery.value.orEmpty()
        )

        _cardsState.value = UiState.Success(filteredCards)
        refreshSuggestions()
    }

    private fun filterSavedCards(
        cards: List<PokemonCard>,
        nameQuery: String,
        setQuery: String,
        typeQuery: String,
        cardIdQuery: String
    ): List<PokemonCard> {
        val normalizedNameQuery = nameQuery.trim()
        val normalizedSetQuery = setQuery.trim()
        val normalizedTypeQuery = typeQuery.trim()
        val normalizedCardIdQuery = cardIdQuery.trim()

        return cards.filter { card ->
            matchesText(card.name, normalizedNameQuery) &&
                    matchesText(card.setName, normalizedSetQuery) &&
                    matchesText(card.cardId, normalizedCardIdQuery) &&
                    matchesTypes(card.types, normalizedTypeQuery)
        }
    }

    private fun matchesText(value: String, query: String): Boolean {
        return query.isBlank() || value.contains(query, ignoreCase = true)
    }

    private fun matchesTypes(types: List<String>, query: String): Boolean {
        return query.isBlank() || types.any { type ->
            type.contains(query, ignoreCase = true)
        }
    }

    private fun refreshSuggestions() {
        _nameSuggestions.value = getSuggestions(
            query = _nameQuery.value.orEmpty(),
            values = allSavedCards.map { it.name }
        )

        _setSuggestions.value = getSuggestions(
            query = _setQuery.value.orEmpty(),
            values = allSavedCards.map { it.setName }
        )

        _typeSuggestions.value = getSuggestions(
            query = _typeQuery.value.orEmpty(),
            values = allSavedCards.flatMap { it.types }
        )
    }

    private fun clearSuggestions() {
        _nameSuggestions.value = emptyList()
        _setSuggestions.value = emptyList()
        _typeSuggestions.value = emptyList()
    }

    private fun getSuggestions(
        query: String,
        values: List<String>,
        limit: Int = 8
    ): List<String> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return emptyList()
        }

        return values
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .filter { value ->
                value.contains(trimmedQuery, ignoreCase = true)
            }
            .sortedWith(
                compareBy<String> { !it.startsWith(trimmedQuery, ignoreCase = true) }
                    .thenBy { it.lowercase() }
            )
            .take(limit)
    }
}