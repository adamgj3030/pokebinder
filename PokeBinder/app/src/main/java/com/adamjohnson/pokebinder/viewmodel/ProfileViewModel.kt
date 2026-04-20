package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.User
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.adamjohnson.pokebinder.data.repository.BinderPageRepository
import com.adamjohnson.pokebinder.utils.UiState

class ProfileViewModel : ViewModel() {

    data class ProfileSummary(
        val publishedPageCount: Int = 0,
        val likesReceivedCount: Int = 0,
        val commentsReceivedCount: Int = 0
    )

    private val authRepository = AuthRepository()
    private val binderPageRepository = BinderPageRepository()

    private var loadGeneration = 0

    private val _currentUser = MutableLiveData(authRepository.getCurrentUser())
    val currentUser: LiveData<User?> = _currentUser

    private val _summaryState = MutableLiveData<UiState<ProfileSummary>>(UiState.Loading)
    val summaryState: LiveData<UiState<ProfileSummary>> = _summaryState

    fun loadProfile() {
        val user = authRepository.getCurrentUser()

        if (user == null || user.userId.isBlank()) {
            nextLoadId()
            _currentUser.value = null
            _summaryState.value = UiState.Success(ProfileSummary())
            return
        }

        _currentUser.value = user
        loadSummary(user.userId, nextLoadId())
    }

    fun signOut() {
        authRepository.signOut()
        nextLoadId()
        _currentUser.value = null
        _summaryState.value = UiState.Success(ProfileSummary())
    }

    private fun loadSummary(userId: String, loadId: Int) {
        if (userId.isBlank()) {
            _summaryState.value = UiState.Error("Invalid user.")
            return
        }

        _summaryState.value = UiState.Loading

        binderPageRepository.getPublishedPagesForUser(userId) { publishedPages, errorMessage ->
            if (!isActiveLoad(loadId, userId)) {
                return@getPublishedPagesForUser
            }

            if (publishedPages != null) {
                val summary = ProfileSummary(
                    publishedPageCount = publishedPages.size,
                    likesReceivedCount = publishedPages.sumOf { it.likeCount },
                    commentsReceivedCount = publishedPages.sumOf { it.commentCount }
                )
                _summaryState.postValue(UiState.Success(summary))
            } else {
                _summaryState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load your profile summary.")
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