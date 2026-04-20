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
    private var refreshGeneration = 0

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

    fun loadPage(pageId: String) {
        refreshAuthState()

        val normalizedPageId = pageId.trim()
        loadedPageId = normalizedPageId.ifBlank { null }
        _isLikeLoading.value = false
        _commentSubmissionState.value = null
        _socialMessage.value = null

        if (normalizedPageId.isBlank()) {
            nextRefreshId()
            _pageState.value = UiState.Error("Invalid page.")
            _commentsState.value = UiState.Error("Invalid page.")
            _isLiked.value = false
            return
        }

        _pageState.value = UiState.Loading
        _commentsState.value = UiState.Loading
        _isLiked.value = false

        refreshPageContent(normalizedPageId, nextRefreshId())
    }

    fun refreshCurrentPage() {
        refreshAuthState()

        val pageId = loadedPageId.orEmpty()
        if (pageId.isBlank()) {
            return
        }

        refreshPageContent(pageId, nextRefreshId())
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
                updateCurrentPageLikeCount(isNowLiked)
                refreshCurrentPage()
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
                appendCommentToCurrentList(comment)
                updateCurrentPageCommentCount()
                _commentSubmissionState.postValue(UiState.Success(comment))
                refreshCurrentPage()
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

    private fun refreshPageContent(pageId: String, refreshId: Int) {
        loadPublishedPage(pageId, refreshId)
        loadComments(pageId, refreshId)
        loadLikeState(pageId, refreshId)
    }

    private fun loadPublishedPage(pageId: String, refreshId: Int) {
        binderPageRepository.getPublishedPage(pageId) { binderPage, errorMessage ->
            if (!isActiveRefresh(refreshId)) {
                return@getPublishedPage
            }

            if (binderPage != null) {
                _pageState.postValue(UiState.Success(binderPage))
            } else {
                _pageState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load published page.")
                )
            }
        }
    }

    private fun loadComments(pageId: String, refreshId: Int) {
        socialRepository.getComments(pageId) { comments, errorMessage ->
            if (!isActiveRefresh(refreshId)) {
                return@getComments
            }

            if (comments != null) {
                _commentsState.postValue(UiState.Success(comments))
            } else {
                _commentsState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load comments.")
                )
            }
        }
    }

    private fun loadLikeState(pageId: String, refreshId: Int) {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            if (isActiveRefresh(refreshId)) {
                _isLiked.postValue(false)
            }
            return
        }

        socialRepository.isPageLikedByUser(pageId, userId) { isLiked, _ ->
            if (!isActiveRefresh(refreshId)) {
                return@isPageLikedByUser
            }

            _isLiked.postValue(isLiked == true)
        }
    }

    private fun updateCurrentPageLikeCount(isNowLiked: Boolean) {
        val currentPage = (_pageState.value as? UiState.Success)?.data ?: return

        val updatedLikeCount = if (isNowLiked) {
            currentPage.likeCount + 1
        } else {
            maxOf(currentPage.likeCount - 1, 0)
        }

        _pageState.postValue(
            UiState.Success(
                currentPage.copy(likeCount = updatedLikeCount)
            )
        )
    }

    private fun updateCurrentPageCommentCount() {
        val currentPage = (_pageState.value as? UiState.Success)?.data ?: return

        _pageState.postValue(
            UiState.Success(
                currentPage.copy(commentCount = currentPage.commentCount + 1)
            )
        )
    }

    private fun appendCommentToCurrentList(comment: Comment) {
        val currentComments = (_commentsState.value as? UiState.Success)?.data ?: return

        if (currentComments.any { it.commentId == comment.commentId }) {
            return
        }

        _commentsState.postValue(UiState.Success(currentComments + comment))
    }

    private fun refreshAuthState() {
        _userSignedInState.value = authRepository.isSignedIn()
    }

    private fun nextRefreshId(): Int {
        refreshGeneration += 1
        return refreshGeneration
    }

    private fun isActiveRefresh(refreshId: Int): Boolean {
        return refreshId == refreshGeneration
    }
}