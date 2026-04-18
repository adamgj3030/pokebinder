package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.SavedCard
import com.adamjohnson.pokebinder.data.repository.CollectionRepository
import com.adamjohnson.pokebinder.utils.UiState

class CollectionViewModel : ViewModel() {

    private val collectionRepository = CollectionRepository()

    private val _savedCardsState =
        MutableLiveData<UiState<List<SavedCard>>>(UiState.Success(emptyList()))
    val savedCardsState: LiveData<UiState<List<SavedCard>>> = _savedCardsState

    fun loadSavedCards(userId: String) {
        _savedCardsState.value = UiState.Loading
        _savedCardsState.value = UiState.Success(collectionRepository.getSavedCards(userId))
    }
}