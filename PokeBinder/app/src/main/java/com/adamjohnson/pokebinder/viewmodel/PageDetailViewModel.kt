package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.Comment
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.adamjohnson.pokebinder.data.repository.BinderPageRepository
import com.adamjohnson.pokebinder.data.repository.SocialRepository
import com.adamjohnson.pokebinder.utils.UiState

class PageDetailViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val binderPageRepository = BinderPageRepository()
    private val socialRepository = SocialRepository()
    private var loadedPageId: String? = null

    private val _pageState = MutableLiveData<UiState<BinderPage>>(UiState.Loading)
    val pageState: LiveData<UiState<BinderPage>> = _pageState

    private val _commentsState =
        MutableLiveData<UiState<List<Comment>>>(UiState.Loading)
    val commentsState: LiveData<UiState<List<Comment>>> = _commentsState

    private val _isLiked = MutableLiveData(false)
    val isLiked: LiveData<Boolean> = _isLiked

    private val _isLikeLoading = MutableLiveData(false)
    val isLikeLoading: LiveData<Boolean> = _isLikeLoading

    private val _commentSubmissionState = MutableLiveData<UiState<Comment>?>(null)
    val commentSubmissionState: LiveData<UiState<Comment>?> = _commentSubmissionState

    private val _socialMessage = MutableLiveData<String?>(null)
    val socialMessage: LiveData<String?> = _socialMessage

    fun loadPage(pageId: String) {
        val normalizedPageId = pageId.trim()

        if (normalizedPageId.isBlank()) {
            _pageState.value = UiState.Error("Invalid page.")
            _commentsState.value = UiState.Error("Invalid page.")
            _isLiked.value = false
            _isLikeLoading.value = false
            _commentSubmissionState.value = null
            _socialMessage.value = null
            return
        }

        loadedPageId = normalizedPageId
        _pageState.value = UiState.Loading
        _commentsState.value = UiState.Loading
        _isLiked.value = false
        _isLikeLoading.value = false
        _commentSubmissionState.value = null
        _socialMessage.value = null

        refreshPageContent(normalizedPageId)
    }

    fun isUserSignedIn(): Boolean {
        return authRepository.isSignedIn()
    }

    fun toggleLike() {
        val pageId = loadedPageId.orEmpty()

        if (pageId.isBlank()) {
            _socialMessage.value = "Invalid page."
            return
        }

        if (_isLikeLoading.value == true) {
            return
        }

        val userId = authRepository.getCurrentUserId()
        if (userId.isNullOrBlank()) {
            _socialMessage.value = "You must be signed in to like pages."
            return
        }

        _isLikeLoading.value = true

        socialRepository.toggleLike(pageId, userId) { isNowLiked, errorMessage ->
            _isLikeLoading.postValue(false)

            if (isNowLiked != null) {
                _isLiked.postValue(isNowLiked)
                loadPublishedPage(pageId)
            } else {
                _socialMessage.postValue(
                    errorMessage ?: "Unable to update page like."
                )
            }
        }
    }

    fun submitComment(text: String) {
        val pageId = loadedPageId.orEmpty()

        if (pageId.isBlank()) {
            _commentSubmissionState.value = UiState.Error("Invalid page.")
            return
        }

        if (_commentSubmissionState.value == UiState.Loading) {
            return
        }

        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null || currentUser.userId.isBlank()) {
            _commentSubmissionState.value =
                UiState.Error("You must be signed in to comment.")
            return
        }

        val normalizedText = text.trim()
        if (normalizedText.isBlank()) {
            _commentSubmissionState.value =
                UiState.Error("Comment text is required.")
            return
        }

        _commentSubmissionState.value = UiState.Loading

        socialRepository.addComment(
            pageId = pageId,
            userId = currentUser.userId,
            userDisplayName = currentUser.displayName,
            text = normalizedText
        ) { comment, errorMessage ->
            if (comment != null) {
                _commentSubmissionState.postValue(UiState.Success(comment))
                loadPublishedPage(pageId)
                loadComments(pageId)
            } else {
                _commentSubmissionState.postValue(
                    UiState.Error(errorMessage ?: "Unable to add comment.")
                )
            }
        }
    }

    fun clearSocialMessage() {
        _socialMessage.value = null
    }

    private fun refreshPageContent(pageId: String) {
        loadPublishedPage(pageId)
        loadComments(pageId)
        loadLikeState(pageId)
    }

    private fun loadPublishedPage(pageId: String) {
        binderPageRepository.getPublishedPage(pageId) { binderPage, errorMessage ->
            if (binderPage != null) {
                _pageState.postValue(UiState.Success(binderPage))
            } else {
                _pageState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load published page.")
                )
            }
        }
    }

    private fun loadComments(pageId: String) {
        socialRepository.getComments(pageId) { comments, errorMessage ->
            if (comments != null) {
                _commentsState.postValue(UiState.Success(comments))
            } else {
                _commentsState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load comments.")
                )
            }
        }
    }

    private fun loadLikeState(pageId: String) {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            _isLiked.value = false
            return
        }

        socialRepository.isPageLikedByUser(pageId, userId) { isLiked, _ ->
            _isLiked.postValue(isLiked == true)
        }
    }
}