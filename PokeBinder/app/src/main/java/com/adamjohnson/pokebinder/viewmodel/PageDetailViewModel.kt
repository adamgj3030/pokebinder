package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.Comment
import com.adamjohnson.pokebinder.data.repository.SocialRepository
import com.adamjohnson.pokebinder.utils.UiState

class PageDetailViewModel : ViewModel() {

    private val socialRepository = SocialRepository()

    private val _pageState = MutableLiveData<UiState<BinderPage?>>(UiState.Success(null))
    val pageState: LiveData<UiState<BinderPage?>> = _pageState

    private val _commentsState =
        MutableLiveData<UiState<List<Comment>>>(UiState.Success(emptyList()))
    val commentsState: LiveData<UiState<List<Comment>>> = _commentsState

    fun setPage(page: BinderPage?) {
        _pageState.value = UiState.Success(page)
    }

    fun loadComments(pageId: String) {
        _commentsState.value = UiState.Loading
        _commentsState.value = UiState.Success(socialRepository.getComments(pageId))
    }
}