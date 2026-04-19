package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.User
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.adamjohnson.pokebinder.utils.UiState

class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _profileState = MutableLiveData<UiState<User?>>(UiState.Loading)
    val profileState: LiveData<UiState<User?>> = _profileState

    init {
        loadProfile()
    }

    fun loadProfile() {
        _profileState.value = UiState.Success(authRepository.getCurrentUser())
    }
}