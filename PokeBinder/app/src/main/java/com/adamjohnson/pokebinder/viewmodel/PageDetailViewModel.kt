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
import com.google.firebase.firestore.ListenerRegistration

class PageDetailViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val binderPageRepository = BinderPageRepository()
    private val socialRepository = SocialRepository()

    private var loadedPageId: String? = null

    private var pageListener: ListenerRegistration? = null
    private var commentsListener: ListenerRegistration? = null
    private var likeStatusListener: ListenerRegistration? = null

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

    private val _userSignedInState = MutableLiveData(authRepository.isSignedIn())
    val userSignedInState: LiveData<Boolean> = _userSignedInState

    fun setPage(pageId: String) {
        refreshAuthState()
        stopListening()

        val normalizedPageId = pageId.trim()
        loadedPageId = normalizedPageId.ifBlank { null }
        _isLikeLoading.value = false
        _commentSubmissionState.value = null
        _socialMessage.value = null

        if (normalizedPageId.isBlank()) {
            _pageState.value = UiState.Error("Invalid page.")
            _commentsState.value = UiState.Error("Invalid page.")
            _isLiked.value = false
            return
        }

        _pageState.value = UiState.Loading
        _commentsState.value = UiState.Loading
        _isLiked.value = false
    }

    fun startListening() {
        refreshAuthState()

        val pageId = loadedPageId.orEmpty()
        if (pageId.isBlank()) {
            return
        }

        stopListening()

        pageListener = binderPageRepository.listenToPublishedPage(pageId) { binderPage, errorMessage ->
            if (binderPage != null) {
                _pageState.postValue(UiState.Success(binderPage))
            } else {
                _pageState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load published page.")
                )
            }
        }

        commentsListener = socialRepository.listenToComments(pageId) { comments, errorMessage ->
            if (comments != null) {
                _commentsState.postValue(UiState.Success(comments))
            } else {
                _commentsState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load comments.")
                )
            }
        }

        startLikeStatusListener(pageId)
    }

    fun stopListening() {
        pageListener?.remove()
        pageListener = null

        commentsListener?.remove()
        commentsListener = null

        likeStatusListener?.remove()
        likeStatusListener = null
    }

    fun toggleLike() {
        refreshAuthState()

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
            } else {
                _socialMessage.postValue(
                    errorMessage ?: "Unable to update page like."
                )
            }
        }
    }

    fun submitComment(text: String) {
        refreshAuthState()

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
            } else {
                _commentSubmissionState.postValue(
                    UiState.Error(errorMessage ?: "Unable to add comment.")
                )
            }
        }
    }

    fun clearCommentSubmissionState() {
        if (_commentSubmissionState.value == UiState.Loading) {
            return
        }

        _commentSubmissionState.value = null
    }

    fun clearSocialMessage() {
        _socialMessage.value = null
    }

    private fun startLikeStatusListener(pageId: String) {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            _isLiked.postValue(false)
            return
        }

        likeStatusListener = socialRepository.listenToPageLikeStatus(
            pageId = pageId,
            userId = userId
        ) { isLiked, _ ->
            _isLiked.postValue(isLiked == true)
        }
    }

    private fun refreshAuthState() {
        _userSignedInState.value = authRepository.isSignedIn()
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}