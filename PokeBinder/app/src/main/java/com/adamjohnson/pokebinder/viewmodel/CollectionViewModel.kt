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

class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val collectionRepository = CollectionRepository()
    private val cardRepository = CardRepository(
        PokemonCardLocalDataSource(application.applicationContext)
    )

    private var allCollectionCards: List<PokemonCard> = emptyList()

    private val _collectionState = MutableLiveData<UiState<List<PokemonCard>>>(UiState.Loading)
    val collectionState: LiveData<UiState<List<PokemonCard>>> = _collectionState

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

    fun loadCollection() {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            _collectionState.value = UiState.Error("You must be signed in to view your collection.")
            clearSuggestions()
            return
        }

        _collectionState.value = UiState.Loading

        collectionRepository.getSavedCards(userId) { savedCards, errorMessage ->
            if (savedCards != null) {
                try {
                    allCollectionCards = savedCards.mapNotNull { savedCard ->
                        cardRepository.getPokemonCard(savedCard.cardId)
                    }
                    publishFilteredCollection()
                } catch (exception: Exception) {
                    allCollectionCards = emptyList()
                    _collectionState.postValue(UiState.Error("Unable to load collection."))
                    clearSuggestions()
                }
            } else {
                allCollectionCards = emptyList()
                _collectionState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load collection.")
                )
                clearSuggestions()
            }
        }
    }

    fun updateNameQuery(query: String) {
        _nameQuery.value = query
        publishFilteredCollectionIfReady()
    }

    fun updateSetQuery(query: String) {
        _setQuery.value = query
        publishFilteredCollectionIfReady()
    }

    fun updateTypeQuery(query: String) {
        _typeQuery.value = query
        publishFilteredCollectionIfReady()
    }

    fun updateCardIdQuery(query: String) {
        _cardIdQuery.value = query
        publishFilteredCollectionIfReady()
    }

    fun updateSubtypeQuery(query: String) {
        _subtypeQuery.value = query
        publishFilteredCollectionIfReady()
    }

    fun updateRarityQuery(query: String) {
        _rarityQuery.value = query
        publishFilteredCollectionIfReady()
    }

    fun updateFullArtOnly(enabled: Boolean) {
        _fullArtOnly.value = enabled
        publishFilteredCollectionIfReady()
    }

    fun clearFilters() {
        _nameQuery.value = ""
        _setQuery.value = ""
        _typeQuery.value = ""
        _cardIdQuery.value = ""
        _subtypeQuery.value = ""
        _rarityQuery.value = ""
        _fullArtOnly.value = false
        publishFilteredCollectionIfReady()
    }

    fun removeCard(
        cardId: String,
        onResult: (String?) -> Unit = {}
    ) {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            val errorMessage = "You must be signed in to update your collection."
            _collectionState.value = UiState.Error(errorMessage)
            onResult(errorMessage)
            return
        }

        if (cardId.isBlank()) {
            val errorMessage = "Invalid card."
            _collectionState.value = UiState.Error(errorMessage)
            onResult(errorMessage)
            return
        }

        collectionRepository.removeCard(userId, cardId) { errorMessage ->
            if (errorMessage == null) {
                allCollectionCards = allCollectionCards.filterNot { card ->
                    card.cardId == cardId
                }
                publishFilteredCollection()
            } else {
                _collectionState.postValue(UiState.Error(errorMessage))
            }

            onResult(errorMessage)
        }
    }

    private fun publishFilteredCollectionIfReady() {
        if (_collectionState.value !is UiState.Loading) {
            publishFilteredCollection()
        }
    }

    private fun publishFilteredCollection() {
        val filteredCards = cardRepository.filterPokemonCards(
            filter = currentFilter(),
            cards = allCollectionCards
        )
        _collectionState.postValue(UiState.Success(filteredCards))
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
            cards = allCollectionCards
        )
        _setSuggestions.value = cardRepository.getSetSuggestions(
            query = _setQuery.value.orEmpty(),
            cards = allCollectionCards
        )
        _typeSuggestions.value = cardRepository.getTypeSuggestions(
            query = _typeQuery.value.orEmpty(),
            cards = allCollectionCards
        )
        _subtypeSuggestions.value = cardRepository.getSubtypeSuggestions(
            query = _subtypeQuery.value.orEmpty(),
            cards = allCollectionCards
        )
        _raritySuggestions.value = cardRepository.getRaritySuggestions(
            query = _rarityQuery.value.orEmpty(),
            cards = allCollectionCards
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