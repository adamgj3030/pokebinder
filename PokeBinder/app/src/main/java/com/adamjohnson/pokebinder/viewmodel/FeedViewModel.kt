package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.repository.BinderPageRepository
import com.adamjohnson.pokebinder.utils.UiState
import com.google.firebase.firestore.ListenerRegistration

class FeedViewModel : ViewModel() {

    private val binderPageRepository = BinderPageRepository()

    private var feedListener: ListenerRegistration? = null

    private val _feedState = MutableLiveData<UiState<List<BinderPage>>>(UiState.Loading)
    val feedState: LiveData<UiState<List<BinderPage>>> = _feedState

    fun startListening() {
        stopListening()

        if (_feedState.value !is UiState.Success) {
            _feedState.value = UiState.Loading
        }

        feedListener = binderPageRepository.listenToPublishedPages { publishedPages, errorMessage ->
            if (publishedPages != null) {
                _feedState.postValue(UiState.Success(publishedPages))
            } else {
                _feedState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load published pages.")
                )
            }
        }
    }

    fun stopListening() {
        feedListener?.remove()
        feedListener = null
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}