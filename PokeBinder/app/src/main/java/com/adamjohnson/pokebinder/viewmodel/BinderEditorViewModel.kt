package com.adamjohnson.pokebinder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.BinderPageStatus
import com.adamjohnson.pokebinder.data.model.BinderSlot
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.adamjohnson.pokebinder.data.repository.BinderPageRepository
import com.adamjohnson.pokebinder.utils.UiState

class BinderEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val binderPageRepository = BinderPageRepository()

    private var currentBinderPage = emptyDraftPage()
    private var hasInitialized = false
    private var initializedPageId: String? = null

    private val _binderPageState = MutableLiveData<UiState<BinderPage>>(UiState.Loading)
    val binderPageState: LiveData<UiState<BinderPage>> = _binderPageState

    private val _canPublish = MutableLiveData(false)
    val canPublish: LiveData<Boolean> = _canPublish

    fun initializeEditor(pageId: String?) {
        val normalizedPageId = pageId?.trim()?.takeIf { it.isNotBlank() }

        if (hasInitialized && initializedPageId == normalizedPageId) {
            return
        }

        hasInitialized = true
        initializedPageId = normalizedPageId

        if (normalizedPageId == null) {
            startNewDraft()
        } else {
            loadEditorPage(normalizedPageId)
        }
    }

    fun startNewDraft(
        title: String = "",
        onResult: (BinderPage?, String?) -> Unit = { _, _ -> }
    ) {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            currentBinderPage = emptyDraftPage()
            updateCanPublish()

            val errorMessage = "You must be signed in to create a binder draft."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        val newBinderPage = emptyDraftPage(
            userId = userId,
            title = title
        )

        currentBinderPage = newBinderPage
        updateCanPublish()
        _binderPageState.value = UiState.Success(newBinderPage)
        onResult(newBinderPage, null)
    }

    fun updateTitle(title: String) {
        if (!isCurrentPageEditable() || isRequestInProgress()) {
            return
        }

        updateCurrentBinderPage(
            currentBinderPage.copy(title = title)
        )
    }

    fun saveCurrentDraft(
        onResult: (BinderPage?, String?) -> Unit = { _, _ -> }
    ) {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            val errorMessage = "You must be signed in to save a binder draft."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        if (!isCurrentPageEditable()) {
            val errorMessage = "Published binders are read-only."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        if (isRequestInProgress()) {
            return
        }

        _binderPageState.value = UiState.Loading

        val binderPageToSave = normalizeBinderPageForPersistence(currentBinderPage)

        binderPageRepository.saveDraftPage(userId, binderPageToSave) { savedBinderPage, errorMessage ->
            if (savedBinderPage != null) {
                currentBinderPage = normalizeBinderPageForEditing(savedBinderPage)
                initializedPageId = currentBinderPage.pageId.takeIf { it.isNotBlank() }
                updateCanPublish()
                _binderPageState.postValue(UiState.Success(currentBinderPage))
            } else {
                _binderPageState.postValue(
                    UiState.Error(errorMessage ?: "Unable to save draft binder.")
                )
            }

            onResult(savedBinderPage, errorMessage)
        }
    }

    fun publishCurrentPage(
        onResult: (BinderPage?, String?) -> Unit = { _, _ -> }
    ) {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            val errorMessage = "You must be signed in to publish a binder."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        if (!isCurrentPageEditable()) {
            val errorMessage = "Published binders are read-only."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        if (isRequestInProgress()) {
            return
        }

        val validationMessage = getPublishValidationMessage()
        if (validationMessage != null) {
            _binderPageState.value = UiState.Error(validationMessage)
            onResult(null, validationMessage)
            return
        }

        _binderPageState.value = UiState.Loading

        val binderPageToPublish = normalizeBinderPageForPersistence(currentBinderPage)

        binderPageRepository.publishPage(userId, binderPageToPublish) { publishedBinderPage, errorMessage ->
            if (publishedBinderPage != null) {
                currentBinderPage = normalizeBinderPageForEditing(publishedBinderPage)
                initializedPageId = currentBinderPage.pageId.takeIf { it.isNotBlank() }
                updateCanPublish()
                _binderPageState.postValue(UiState.Success(currentBinderPage))
            } else {
                _binderPageState.postValue(
                    UiState.Error(errorMessage ?: "Unable to publish binder.")
                )
            }

            onResult(publishedBinderPage, errorMessage)
        }
    }

    fun getEmptySlotCount(): Int {
        return normalizeSlots(currentBinderPage.slots).count { slot ->
            slot.cardId.isNullOrBlank()
        }
    }

    fun removeCardFromSlot(slotIndex: Int) {
        if (!isCurrentPageEditable() || isRequestInProgress()) {
            return
        }

        val normalizedIndex = normalizeSlotIndex(slotIndex)
        val updatedSlots = normalizeSlots(currentBinderPage.slots).mapIndexed { index, slot ->
            if (index == normalizedIndex) {
                slot.copy(cardId = null)
            } else {
                slot
            }
        }

        updateCurrentBinderPage(
            currentBinderPage.copy(slots = updatedSlots)
        )
    }

    fun addCardsFromSlot(startSlotIndex: Int, cardIds: List<String>) {
        if (!isCurrentPageEditable() || isRequestInProgress()) {
            return
        }

        val normalizedCardIds = cardIds.map { it.trim() }.filter { it.isNotBlank() }
        if (normalizedCardIds.isEmpty()) {
            return
        }

        val updatedSlots = normalizeSlots(currentBinderPage.slots).toMutableList()
        var slotPointer = normalizeSlotIndex(startSlotIndex)

        normalizedCardIds.forEach { cardId ->
            var checkedSlots = 0

            while (
                checkedSlots < SLOT_COUNT &&
                !updatedSlots[slotPointer].cardId.isNullOrBlank()
            ) {
                slotPointer = (slotPointer + 1) % SLOT_COUNT
                checkedSlots += 1
            }

            if (checkedSlots == SLOT_COUNT) {
                return@forEach
            }

            updatedSlots[slotPointer] = updatedSlots[slotPointer].copy(cardId = cardId)
            slotPointer = (slotPointer + 1) % SLOT_COUNT
        }

        updateCurrentBinderPage(
            currentBinderPage.copy(slots = updatedSlots)
        )
    }

    private fun loadEditorPage(pageId: String) {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            val errorMessage = "You must be signed in to load a binder draft."
            currentBinderPage = emptyDraftPage()
            updateCanPublish()
            _binderPageState.value = UiState.Error(errorMessage)
            return
        }

        _binderPageState.value = UiState.Loading

        binderPageRepository.getDraftPage(userId, pageId) { binderPage, errorMessage ->
            if (binderPage != null) {
                currentBinderPage = normalizeBinderPageForEditing(binderPage)
                updateCanPublish()
                _binderPageState.postValue(UiState.Success(currentBinderPage))
            } else {
                currentBinderPage = emptyDraftPage(userId = userId)
                updateCanPublish()
                _binderPageState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load draft binder.")
                )
            }
        }
    }

    private fun updateCurrentBinderPage(updatedBinderPage: BinderPage) {
        currentBinderPage = normalizeBinderPageForEditing(updatedBinderPage)
        updateCanPublish()
        _binderPageState.value = UiState.Success(currentBinderPage)
    }

    private fun updateCanPublish() {
        _canPublish.value = canPublishCurrentPage()
    }

    private fun canPublishCurrentPage(): Boolean {
        return getPublishValidationMessage() == null
    }

    private fun getPublishValidationMessage(): String? {
        if (currentBinderPage.status != BinderPageStatus.DRAFT) {
            return "Published binders are read-only."
        }

        if (currentBinderPage.title.trim().isBlank()) {
            return "A binder title is required before publishing."
        }

        val normalizedSlots = normalizeSlots(currentBinderPage.slots)
        if (normalizedSlots.size != SLOT_COUNT) {
            return "All 9 slots must be filled before publishing."
        }

        if (normalizedSlots.any { it.cardId.isNullOrBlank() }) {
            return "All 9 slots must be filled before publishing."
        }

        return null
    }

    private fun normalizeBinderPageForEditing(binderPage: BinderPage): BinderPage {
        return binderPage.copy(
            title = binderPage.title,
            slots = normalizeSlots(binderPage.slots)
        )
    }

    private fun normalizeBinderPageForPersistence(binderPage: BinderPage): BinderPage {
        return binderPage.copy(
            title = binderPage.title.trim(),
            slots = normalizeSlots(binderPage.slots)
        )
    }

    private fun normalizeSlots(slots: List<BinderSlot>): List<BinderSlot> {
        val slotsByIndex = slots.associateBy { it.index }

        return List(SLOT_COUNT) { index ->
            BinderSlot(
                index = index,
                cardId = slotsByIndex[index]?.cardId
            )
        }
    }

    private fun normalizeSlotIndex(slotIndex: Int): Int {
        return when {
            slotIndex < 0 -> 0
            slotIndex >= SLOT_COUNT -> SLOT_COUNT - 1
            else -> slotIndex
        }
    }

    private fun emptyDraftPage(
        userId: String = "",
        title: String = ""
    ): BinderPage {
        return BinderPage(
            ownerUserId = userId,
            title = title,
            status = BinderPageStatus.DRAFT,
            slots = List(SLOT_COUNT) { index ->
                BinderSlot(
                    index = index,
                    cardId = null
                )
            }
        )
    }

    private fun isCurrentPageEditable(): Boolean {
        return currentBinderPage.status == BinderPageStatus.DRAFT
    }

    private fun isRequestInProgress(): Boolean {
        return _binderPageState.value == UiState.Loading
    }

    companion object {
        private const val SLOT_COUNT = 9
    }
}