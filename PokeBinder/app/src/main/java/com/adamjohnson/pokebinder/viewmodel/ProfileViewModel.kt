package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.User
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.adamjohnson.pokebinder.data.repository.UserRepository
import com.adamjohnson.pokebinder.utils.UiState

class ProfileViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _profileState = MutableLiveData<UiState<User?>>(UiState.Loading)
    val profileState: LiveData<UiState<User?>> = _profileState

    init {
        loadProfile()
    }

    fun loadProfile() {
        val currentUserId = authRepository.getCurrentUserId()

        val user = if (currentUserId == null) {
            null
        } else {
            userRepository.getUser(currentUserId) ?: authRepository.getCurrentUser()
        }

        _profileState.value = UiState.Success(user)
    }
}