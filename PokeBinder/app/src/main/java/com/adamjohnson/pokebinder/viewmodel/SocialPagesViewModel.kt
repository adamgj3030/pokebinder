package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.adamjohnson.pokebinder.data.repository.BinderPageRepository
import com.adamjohnson.pokebinder.data.repository.SocialRepository
import com.adamjohnson.pokebinder.utils.UiState

class SocialPagesViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val binderPageRepository = BinderPageRepository()
    private val socialRepository = SocialRepository()

    private var loadGeneration = 0
    private var currentMode = MODE_LIKES

    private val _pagesState = MutableLiveData<UiState<List<BinderPage>>>(UiState.Loading)
    val pagesState: LiveData<UiState<List<BinderPage>>> = _pagesState

    fun loadPages(mode: String) {
        val normalizedMode = normalizeMode(mode)
        currentMode = normalizedMode

        val userId = authRepository.getCurrentUserId()
        if (userId.isNullOrBlank()) {
            nextLoadId()
            _pagesState.value = UiState.Error("You must be signed in to view this list.")
            return
        }

        val loadId = nextLoadId()
        _pagesState.value = UiState.Loading

        val onPageIdsLoaded = fun(pageIds: List<String>?, errorMessage: String?) {
            if (!isActiveLoad(loadId, userId, normalizedMode)) {
                return
            }

            if (pageIds == null) {
                _pagesState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load pages.")
                )
                return
            }

            if (pageIds.isEmpty()) {
                _pagesState.postValue(UiState.Success(emptyList()))
                return
            }

            binderPageRepository.getPublishedPagesByIds(pageIds) { publishedPages, pagesError ->
                if (!isActiveLoad(loadId, userId, normalizedMode)) {
                    return@getPublishedPagesByIds
                }

                if (publishedPages != null) {
                    _pagesState.postValue(UiState.Success(publishedPages))
                } else {
                    _pagesState.postValue(
                        UiState.Error(pagesError ?: "Unable to load pages.")
                    )
                }
            }
        }

        if (normalizedMode == MODE_COMMENTS) {
            socialRepository.getCommentedPageIdsForUser(userId, onPageIdsLoaded)
        } else {
            socialRepository.getLikedPageIdsForUser(userId, onPageIdsLoaded)
        }
    }

    private fun nextLoadId(): Int {
        loadGeneration += 1
        return loadGeneration
    }

    private fun isActiveLoad(loadId: Int, userId: String, mode: String): Boolean {
        val currentUserId = authRepository.getCurrentUserId().orEmpty()
        return loadId == loadGeneration && currentUserId == userId && currentMode == mode
    }

    private fun normalizeMode(mode: String): String {
        return if (mode == MODE_COMMENTS) {
            MODE_COMMENTS
        } else {
            MODE_LIKES
        }
    }

    companion object {
        const val MODE_LIKES = "likes"
        const val MODE_COMMENTS = "comments"
    }
}
