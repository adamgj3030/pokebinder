package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.BinderPageStatus
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.adamjohnson.pokebinder.data.repository.BinderPageRepository
import com.adamjohnson.pokebinder.utils.UiState

class DraftsViewModel : ViewModel() {

    enum class BinderFilter {
        ALL,
        DRAFTS,
        PUBLISHED
    }

    data class BindersScreenState(
        val totalBindersCount: Int = 0,
        val binders: List<BinderPage> = emptyList(),
        val searchQuery: String = "",
        val filter: BinderFilter = BinderFilter.ALL
    )

    private val authRepository = AuthRepository()
    private val binderPageRepository = BinderPageRepository()

    private var loadGeneration = 0
    private var allBinderPages: List<BinderPage> = emptyList()
    private var currentSearchQuery = ""
    private var currentFilter = BinderFilter.ALL

    private val _bindersState =
        MutableLiveData<UiState<BindersScreenState>>(UiState.Loading)
    val bindersState: LiveData<UiState<BindersScreenState>> = _bindersState

    fun loadBinderPages() {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            nextLoadId()
            allBinderPages = emptyList()
            _bindersState.value = UiState.Error("You must be signed in to view your binders.")
            return
        }

        loadBinderPages(userId, nextLoadId())
    }

    fun updateSearchQuery(query: String) {
        currentSearchQuery = query.trim()

        if (_bindersState.value != UiState.Loading) {
            publishCurrentState()
        }
    }

    fun updateFilter(filter: BinderFilter) {
        currentFilter = filter

        if (_bindersState.value != UiState.Loading) {
            publishCurrentState()
        }
    }

    fun deleteBinderPage(
        binderPage: BinderPage,
        onResult: (String?) -> Unit = {}
    ) {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            onResult("You must be signed in to delete a binder.")
            return
        }

        if (binderPage.pageId.isBlank()) {
            onResult("Invalid binder.")
            return
        }

        if (binderPage.ownerUserId.isNotBlank() && binderPage.ownerUserId != userId) {
            onResult("Invalid user.")
            return
        }

        val onDeleteFinished = fun(errorMessage: String?) {
            if (errorMessage == null) {
                allBinderPages = allBinderPages.filterNot { page ->
                    page.pageId == binderPage.pageId
                }
                publishCurrentState()
            }

            onResult(errorMessage)
        }

        if (binderPage.status == BinderPageStatus.PUBLISHED) {
            binderPageRepository.deletePublishedPage(
                userId = userId,
                pageId = binderPage.pageId,
                onResult = onDeleteFinished
            )
        } else {
            binderPageRepository.deleteDraftPage(
                userId = userId,
                pageId = binderPage.pageId,
                onResult = onDeleteFinished
            )
        }
    }

    private fun loadBinderPages(userId: String, loadId: Int) {
        if (userId.isBlank()) {
            _bindersState.value = UiState.Error("Invalid user.")
            return
        }

        _bindersState.value = UiState.Loading

        binderPageRepository.getDraftPages(userId) { draftPages, draftErrorMessage ->
            if (!isActiveLoad(loadId, userId)) {
                return@getDraftPages
            }

            if (draftPages == null) {
                _bindersState.postValue(
                    UiState.Error(draftErrorMessage ?: "Unable to load draft binders.")
                )
                return@getDraftPages
            }

            binderPageRepository.getPublishedPagesForUser(userId) { publishedPages, publishedErrorMessage ->
                if (!isActiveLoad(loadId, userId)) {
                    return@getPublishedPagesForUser
                }

                if (publishedPages == null) {
                    _bindersState.postValue(
                        UiState.Error(publishedErrorMessage ?: "Unable to load published binders.")
                    )
                    return@getPublishedPagesForUser
                }

                allBinderPages = sortBinderPages(draftPages + publishedPages)
                publishCurrentState()
            }
        }
    }

    private fun publishCurrentState() {
        val filteredBinders = allBinderPages.filter { binderPage ->
            matchesFilter(binderPage) && matchesSearchQuery(binderPage)
        }

        _bindersState.value = UiState.Success(
            BindersScreenState(
                totalBindersCount = allBinderPages.size,
                binders = filteredBinders,
                searchQuery = currentSearchQuery,
                filter = currentFilter
            )
        )
    }

    private fun matchesFilter(binderPage: BinderPage): Boolean {
        return when (currentFilter) {
            BinderFilter.ALL -> true
            BinderFilter.DRAFTS -> binderPage.status == BinderPageStatus.DRAFT
            BinderFilter.PUBLISHED -> binderPage.status == BinderPageStatus.PUBLISHED
        }
    }

    private fun matchesSearchQuery(binderPage: BinderPage): Boolean {
        return currentSearchQuery.isBlank() ||
                binderPage.title.contains(currentSearchQuery, ignoreCase = true)
    }

    private fun sortBinderPages(binderPages: List<BinderPage>): List<BinderPage> {
        return binderPages.sortedWith(
            compareByDescending<BinderPage> { getSortTimestamp(it) }
                .thenByDescending { it.updatedAt }
        )
    }

    private fun getSortTimestamp(binderPage: BinderPage): Long {
        return if (binderPage.status == BinderPageStatus.PUBLISHED) {
            binderPage.publishedAt ?: binderPage.updatedAt
        } else {
            binderPage.updatedAt
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