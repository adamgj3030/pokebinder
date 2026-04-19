package com.adamjohnson.pokebinder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.adamjohnson.pokebinder.data.local.PokemonCardLocalDataSource
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.BinderPageStatus
import com.adamjohnson.pokebinder.data.model.BinderSlot
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.adamjohnson.pokebinder.data.repository.BinderPageRepository
import com.adamjohnson.pokebinder.data.repository.CardRepository
import com.adamjohnson.pokebinder.data.repository.CollectionRepository
import com.adamjohnson.pokebinder.utils.UiState

class BinderEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val binderPageRepository = BinderPageRepository()
    private val collectionRepository = CollectionRepository()
    private val cardRepository = CardRepository(
        PokemonCardLocalDataSource(application.applicationContext)
    )

    private var currentBinderPage = BinderPage()
    private var hasInitialized = false
    private var initializedPageId: String? = null

    private val _binderPageState = MutableLiveData<UiState<BinderPage>>(UiState.Loading)
    val binderPageState: LiveData<UiState<BinderPage>> = _binderPageState

    private val _availableCardsState =
        MutableLiveData<UiState<List<PokemonCard>>>(UiState.Loading)
    val availableCardsState: LiveData<UiState<List<PokemonCard>>> = _availableCardsState

    private val _selectedSlotIndex = MutableLiveData<Int?>(null)
    val selectedSlotIndex: LiveData<Int?> = _selectedSlotIndex

    private val _canPublish = MutableLiveData(false)
    val canPublish: LiveData<Boolean> = _canPublish

    fun initializeEditor(pageId: String?) {
        val normalizedPageId = pageId?.trim()?.takeIf { it.isNotBlank() }

        if (hasInitialized && initializedPageId == normalizedPageId) {
            return
        }

        hasInitialized = true
        initializedPageId = normalizedPageId

        loadAvailableCards()

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
            currentBinderPage = BinderPage()
            _selectedSlotIndex.value = null
            updateCanPublish()

            val errorMessage = "You must be signed in to create a binder draft."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        val newBinderPage = BinderPage(
            ownerUserId = userId,
            title = title.trim()
        )

        initializedPageId = null
        currentBinderPage = newBinderPage
        _selectedSlotIndex.value = null
        updateCanPublish()
        _binderPageState.value = UiState.Success(newBinderPage)

        onResult(newBinderPage, null)
    }

    fun loadEditorPage(
        pageId: String,
        onResult: (BinderPage?, String?) -> Unit = { _, _ -> }
    ) {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            currentBinderPage = BinderPage()
            _selectedSlotIndex.value = null
            updateCanPublish()

            val errorMessage = "You must be signed in to view this binder page."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        if (pageId.isBlank()) {
            currentBinderPage = BinderPage()
            _selectedSlotIndex.value = null
            updateCanPublish()

            val errorMessage = "Invalid page."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        _selectedSlotIndex.value = null
        _binderPageState.value = UiState.Loading

        binderPageRepository.getEditorPage(userId, pageId) { binderPage, errorMessage ->
            if (binderPage != null) {
                currentBinderPage = binderPage
                initializedPageId = binderPage.pageId.takeIf { it.isNotBlank() }
                updateCanPublish()
                _binderPageState.postValue(UiState.Success(binderPage))
            } else {
                currentBinderPage = BinderPage()
                _selectedSlotIndex.postValue(null)
                updateCanPublish()
                _binderPageState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load binder page.")
                )
            }

            onResult(binderPage, errorMessage)
        }
    }

    fun updateTitle(title: String) {
        if (!isCurrentPageEditable()) {
            return
        }

        updateCurrentBinderPage(
            currentBinderPage.copy(title = title)
        )
    }

    fun selectSlot(slotIndex: Int) {
        if (!isCurrentPageEditable()) {
            return
        }

        if (slotIndex !in 0 until SLOT_COUNT) {
            return
        }

        _selectedSlotIndex.value = slotIndex
    }

    fun assignCardToSelectedSlot(cardId: String) {
        if (!isCurrentPageEditable()) {
            return
        }

        val trimmedCardId = cardId.trim()

        if (trimmedCardId.isBlank()) {
            return
        }

        val availableCards = when (val state = _availableCardsState.value) {
            is UiState.Success -> state.data
            else -> emptyList()
        }

        if (availableCards.none { it.cardId == trimmedCardId }) {
            return
        }

        updateSelectedSlotCard(trimmedCardId)
    }

    fun clearSelectedSlot() {
        if (!isCurrentPageEditable()) {
            return
        }

        updateSelectedSlotCard(null)
    }

    fun saveCurrentDraft(
        onResult: (BinderPage?, String?) -> Unit = { _, _ -> }
    ) {
        if (isRequestInProgress()) {
            return
        }

        if (!isCurrentPageEditable()) {
            val errorMessage = "Published pages are read-only."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            val errorMessage = "You must be signed in to save a binder draft."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        if (currentBinderPage.ownerUserId.isNotBlank() && currentBinderPage.ownerUserId != userId) {
            val errorMessage = "Invalid user."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        _binderPageState.value = UiState.Loading

        binderPageRepository.saveDraftPage(userId, currentBinderPage) { savedBinderPage, errorMessage ->
            if (savedBinderPage != null) {
                currentBinderPage = savedBinderPage
                initializedPageId = savedBinderPage.pageId.takeIf { it.isNotBlank() }
                updateCanPublish()
                _binderPageState.postValue(UiState.Success(savedBinderPage))
            } else {
                _binderPageState.postValue(
                    UiState.Error(errorMessage ?: "Unable to save draft page.")
                )
            }

            onResult(savedBinderPage, errorMessage)
        }
    }

    fun canPublishCurrentPage(): Boolean {
        return calculateCanPublish(currentBinderPage)
    }

    fun publishCurrentPage(
        onResult: (BinderPage?, String?) -> Unit = { _, _ -> }
    ) {
        if (isRequestInProgress()) {
            return
        }

        if (!isCurrentPageEditable()) {
            val errorMessage = "Published pages are read-only."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            val errorMessage = "You must be signed in to publish a binder page."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        if (currentBinderPage.ownerUserId.isNotBlank() && currentBinderPage.ownerUserId != userId) {
            val errorMessage = "Invalid user."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        if (!canPublishCurrentPage()) {
            val errorMessage = "All 9 slots must be filled before publishing."
            _binderPageState.value = UiState.Error(errorMessage)
            onResult(null, errorMessage)
            return
        }

        _binderPageState.value = UiState.Loading

        binderPageRepository.publishPage(userId, currentBinderPage) { publishedBinderPage, errorMessage ->
            if (publishedBinderPage != null) {
                currentBinderPage = publishedBinderPage
                initializedPageId = publishedBinderPage.pageId.takeIf { it.isNotBlank() }
                _selectedSlotIndex.postValue(null)
                updateCanPublish()
                _binderPageState.postValue(UiState.Success(publishedBinderPage))
            } else {
                _binderPageState.postValue(
                    UiState.Error(errorMessage ?: "Unable to publish binder page.")
                )
            }

            onResult(publishedBinderPage, errorMessage)
        }
    }

    fun loadAvailableCards() {
        val userId = authRepository.getCurrentUserId()

        if (userId.isNullOrBlank()) {
            _availableCardsState.value =
                UiState.Error("You must be signed in to load editor cards.")
            return
        }

        _availableCardsState.value = UiState.Loading

        collectionRepository.getSavedCards(userId) { savedCards, errorMessage ->
            if (savedCards != null) {
                try {
                    val cards = savedCards.mapNotNull { savedCard ->
                        cardRepository.getPokemonCard(savedCard.cardId)
                    }
                    _availableCardsState.postValue(UiState.Success(cards))
                } catch (exception: Exception) {
                    _availableCardsState.postValue(
                        UiState.Error("Unable to load editor cards.")
                    )
                }
            } else {
                _availableCardsState.postValue(
                    UiState.Error(errorMessage ?: "Unable to load editor cards.")
                )
            }
        }
    }

    private fun updateSelectedSlotCard(cardId: String?) {
        val selectedIndex = _selectedSlotIndex.value

        if (selectedIndex == null || selectedIndex !in 0 until SLOT_COUNT) {
            return
        }

        val updatedSlots = currentBinderPage.slots.map { slot ->
            if (slot.index == selectedIndex) {
                BinderSlot(
                    index = slot.index,
                    cardId = cardId
                )
            } else {
                slot
            }
        }

        updateCurrentBinderPage(
            currentBinderPage.copy(slots = updatedSlots)
        )
    }

    private fun updateCurrentBinderPage(updatedBinderPage: BinderPage) {
        currentBinderPage = updatedBinderPage
        updateCanPublish()
        _binderPageState.value = UiState.Success(updatedBinderPage)
    }

    private fun updateCanPublish() {
        _canPublish.value = calculateCanPublish(currentBinderPage)
    }

    private fun calculateCanPublish(binderPage: BinderPage): Boolean {
        return binderPage.status == BinderPageStatus.DRAFT &&
                binderPage.slots.size == SLOT_COUNT &&
                binderPage.slots.all { slot ->
                    !slot.cardId.isNullOrBlank()
                }
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