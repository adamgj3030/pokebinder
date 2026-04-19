package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.repository.BinderPageRepository
import com.adamjohnson.pokebinder.utils.UiState

class FeedViewModel : ViewModel() {

    private val binderPageRepository = BinderPageRepository()

    private val _feedState = MutableLiveData<UiState<List<BinderPage>>>(UiState.Loading)
    val feedState: LiveData<UiState<List<BinderPage>>> = _feedState

    init {
        loadFeedPages()
    }

    fun loadFeedPages() {
        _feedState.value = UiState.Loading

        binderPageRepository.getPublishedPages { publishedPages, errorMessage ->
            if (publishedPages != null) {
                _feedState.postValue(UiState.Success(publishedPages))
            } else {
                _feedState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load published pages.")
                )
            }
        }
    }
}