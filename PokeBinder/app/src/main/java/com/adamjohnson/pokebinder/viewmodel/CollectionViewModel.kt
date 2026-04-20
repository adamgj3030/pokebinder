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

class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val collectionRepository = CollectionRepository()
    private val cardRepository = CardRepository(
        PokemonCardLocalDataSource(application.applicationContext)
    )

    private var allCollectionCards: List<PokemonCard> = emptyList()

    private val _collectionState = MutableLiveData<UiState<List<PokemonCard>>>(UiState.Loading)
    val collectionState: LiveData<UiState<List<PokemonCard>>> = _collectionState

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    fun loadCollection() {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            _collectionState.value = UiState.Error("You must be signed in to view your collection.")
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
                }
            } else {
                allCollectionCards = emptyList()
                _collectionState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load collection.")
                )
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query

        if (_collectionState.value !is UiState.Loading) {
            publishFilteredCollection()
        }
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

    private fun publishFilteredCollection() {
        val filteredCards = filterCollectionCards(
            cards = allCollectionCards,
            query = _searchQuery.value.orEmpty()
        )
        _collectionState.postValue(UiState.Success(filteredCards))
    }

    private fun filterCollectionCards(
        cards: List<PokemonCard>,
        query: String
    ): List<PokemonCard> {
        val trimmedQuery = query.trim()

        if (trimmedQuery.isBlank()) {
            return cards
        }

        return cards.filter { card ->
            card.name.contains(trimmedQuery, ignoreCase = true) ||
                    card.setName.contains(trimmedQuery, ignoreCase = true) ||
                    card.cardId.contains(trimmedQuery, ignoreCase = true) ||
                    card.types.any { type ->
                        type.contains(trimmedQuery, ignoreCase = true)
                    }
        }
    }
}