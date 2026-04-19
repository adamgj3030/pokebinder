package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.User
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.adamjohnson.pokebinder.data.repository.BinderPageRepository
import com.adamjohnson.pokebinder.utils.UiState

class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val binderPageRepository = BinderPageRepository()

    private val _currentUser = MutableLiveData(authRepository.getCurrentUser())
    val currentUser: LiveData<User?> = _currentUser

    private val _publishedPagesState =
        MutableLiveData<UiState<List<BinderPage>>>(UiState.Loading)
    val publishedPagesState: LiveData<UiState<List<BinderPage>>> = _publishedPagesState

    init {
        loadProfile()
    }

    fun loadProfile() {
        val user = authRepository.getCurrentUser()
        _currentUser.value = user

        if (user == null) {
            _publishedPagesState.value = UiState.Success(emptyList())
            return
        }

        loadPublishedPages(user.userId)
    }

    fun signOut() {
        authRepository.signOut()
        _currentUser.value = null
        _publishedPagesState.value = UiState.Success(emptyList())
    }

    private fun loadPublishedPages(userId: String) {
        _publishedPagesState.value = UiState.Loading

        binderPageRepository.getPublishedPagesForUser(userId) { publishedPages, errorMessage ->
            if (publishedPages != null) {
                _publishedPagesState.postValue(UiState.Success(publishedPages))
            } else {
                _publishedPagesState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load your published pages.")
                )
            }
        }
    }
}