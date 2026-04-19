package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.adamjohnson.pokebinder.data.repository.BinderPageRepository
import com.adamjohnson.pokebinder.utils.UiState

class DraftsViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val binderPageRepository = BinderPageRepository()

    private val _draftsState =
        MutableLiveData<UiState<List<BinderPage>>>(UiState.Loading)
    val draftsState: LiveData<UiState<List<BinderPage>>> = _draftsState

    fun loadDraftPages() {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            _draftsState.value = UiState.Error("You must be signed in to view your drafts.")
            return
        }

        loadDraftPages(userId)
    }

    fun loadDraftPages(userId: String) {
        if (userId.isBlank()) {
            _draftsState.value = UiState.Error("Invalid user.")
            return
        }

        _draftsState.value = UiState.Loading

        binderPageRepository.getDraftPages(userId) { draftPages, errorMessage ->
            if (draftPages != null) {
                _draftsState.postValue(UiState.Success(draftPages))
            } else {
                _draftsState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load draft pages.")
                )
            }
        }
    }
}