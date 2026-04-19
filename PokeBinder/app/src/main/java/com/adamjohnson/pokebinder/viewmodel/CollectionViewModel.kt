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

    private val _collectionState = MutableLiveData<UiState<List<PokemonCard>>>(UiState.Loading)
    val collectionState: LiveData<UiState<List<PokemonCard>>> = _collectionState

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
                    val cards = savedCards.mapNotNull { savedCard ->
                        cardRepository.getPokemonCard(savedCard.cardId)
                    }
                    _collectionState.postValue(UiState.Success(cards))
                } catch (exception: Exception) {
                    _collectionState.postValue(UiState.Error("Unable to load collection."))
                }
            } else {
                _collectionState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load collection.")
                )
            }
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
                loadCollection()
            } else {
                _collectionState.postValue(UiState.Error(errorMessage))
            }

            onResult(errorMessage)
        }
    }
}