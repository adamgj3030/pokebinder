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

    private var loadGeneration = 0

    private val _currentUser = MutableLiveData(authRepository.getCurrentUser())
    val currentUser: LiveData<User?> = _currentUser

    private val _publishedPagesState =
        MutableLiveData<UiState<List<BinderPage>>>(UiState.Loading)
    val publishedPagesState: LiveData<UiState<List<BinderPage>>> = _publishedPagesState

    fun loadProfile() {
        val user = authRepository.getCurrentUser()

        if (user == null || user.userId.isBlank()) {
            nextLoadId()
            _currentUser.value = null
            _publishedPagesState.value = UiState.Success(emptyList())
            return
        }

        _currentUser.value = user
        loadPublishedPages(user.userId, nextLoadId())
    }

    fun signOut() {
        authRepository.signOut()
        nextLoadId()
        _currentUser.value = null
        _publishedPagesState.value = UiState.Success(emptyList())
    }

    private fun loadPublishedPages(userId: String, loadId: Int) {
        if (userId.isBlank()) {
            _publishedPagesState.value = UiState.Error("Invalid user.")
            return
        }

        _publishedPagesState.value = UiState.Loading

        binderPageRepository.getPublishedPagesForUser(userId) { publishedPages, errorMessage ->
            if (!isActiveLoad(loadId, userId)) {
                return@getPublishedPagesForUser
            }

            if (publishedPages != null) {
                _publishedPagesState.postValue(UiState.Success(publishedPages))
            } else {
                _publishedPagesState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load your published pages.")
                )
            }
        }
    }

    private fun nextLoadId(): Int {
        loadGeneration += 1
        return loadGeneration
    }

    private fun isActiveLoad(loadId: Int, userId: String): Boolean {
        val currentUserId = authRepository.getCurrentUserId().orEmpty()
        return loadId == loadGeneration && currentUserId == userId
    }
}