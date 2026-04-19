package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.repository.BinderPageRepository
import com.adamjohnson.pokebinder.utils.UiState

class PageDetailViewModel : ViewModel() {

    private val binderPageRepository = BinderPageRepository()
    private var loadedPageId: String? = null

    private val _pageState = MutableLiveData<UiState<BinderPage>>(UiState.Loading)
    val pageState: LiveData<UiState<BinderPage>> = _pageState

    fun loadPage(pageId: String) {
        val normalizedPageId = pageId.trim()

        if (normalizedPageId.isBlank()) {
            _pageState.value = UiState.Error("Invalid page.")
            return
        }

        if (loadedPageId == normalizedPageId && _pageState.value is UiState.Success<*>) {
            return
        }

        loadedPageId = normalizedPageId
        _pageState.value = UiState.Loading

        binderPageRepository.getPublishedPage(normalizedPageId) { binderPage, errorMessage ->
            if (binderPage != null) {
                _pageState.postValue(UiState.Success(binderPage))
            } else {
                _pageState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load published page.")
                )
            }
        }
    }
}