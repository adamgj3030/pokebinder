package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.repository.BinderPageRepository
import com.adamjohnson.pokebinder.utils.UiState

class DraftsViewModel : ViewModel() {

    private val binderPageRepository = BinderPageRepository()

    private val _draftsState =
        MutableLiveData<UiState<List<BinderPage>>>(UiState.Success(emptyList()))
    val draftsState: LiveData<UiState<List<BinderPage>>> = _draftsState

    fun loadDraftPages(userId: String) {
        _draftsState.value = UiState.Loading
        _draftsState.value = UiState.Success(binderPageRepository.getDraftPages(userId))
    }
}