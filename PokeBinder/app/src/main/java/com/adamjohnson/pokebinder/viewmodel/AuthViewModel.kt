package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.User
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.adamjohnson.pokebinder.utils.UiState

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _authState = MutableLiveData<UiState<User?>>(UiState.Loading)
    val authState: LiveData<UiState<User?>> = _authState

    init {
        refreshAuthState()
    }

    fun refreshAuthState() {
        _authState.value = UiState.Success(authRepository.getCurrentUser())
    }

    fun isSignedIn(): Boolean {
        return authRepository.isSignedIn()
    }

    fun signOut() {
        authRepository.signOut()
        refreshAuthState()
    }
}