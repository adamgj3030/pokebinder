package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.User
import com.adamjohnson.pokebinder.data.repository.AuthRepository
import com.adamjohnson.pokebinder.data.repository.UserRepository
import com.adamjohnson.pokebinder.utils.UiState

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    private val _authState = MutableLiveData<UiState<User?>>(UiState.Loading)
    val authState: LiveData<UiState<User?>> = _authState

    init {
        refreshAuthState()
    }

    fun refreshAuthState() {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            bootstrapUser(user)
        }
        _authState.value = UiState.Success(user)
    }

    fun isSignedIn(): Boolean {
        return authRepository.isSignedIn()
    }

    fun getCurrentUser(): User? {
        return authRepository.getCurrentUser()
    }

    fun signIn(email: String, password: String) {
        val trimmedEmail = email.trim()
        val validationMessage = validateSignIn(trimmedEmail, password)

        if (validationMessage != null) {
            _authState.value = UiState.Error(validationMessage)
            return
        }

        _authState.value = UiState.Loading

        authRepository.signIn(trimmedEmail, password) { user, errorMessage ->
            if (user != null) {
                bootstrapUser(user)
                _authState.postValue(UiState.Success(user))
            } else {
                _authState.postValue(
                    UiState.Error(errorMessage ?: "Unable to sign in.")
                )
            }
        }
    }

    fun createAccount(email: String, password: String, username: String) {
        val trimmedEmail = email.trim()
        val trimmedUsername = username.trim()
        val validationMessage = validateCreateAccount(trimmedUsername, trimmedEmail, password)

        if (validationMessage != null) {
            _authState.value = UiState.Error(validationMessage)
            return
        }

        _authState.value = UiState.Loading

        authRepository.createAccount(trimmedEmail, password, trimmedUsername) { user, errorMessage ->
            if (user != null) {
                bootstrapUser(user)
                _authState.postValue(UiState.Success(user))
            } else {
                _authState.postValue(
                    UiState.Error(errorMessage ?: "Unable to create account.")
                )
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        refreshAuthState()
    }

    private fun bootstrapUser(user: User) {
        userRepository.createOrUpdateUser(user)
    }

    private fun validateSignIn(email: String, password: String): String? {
        if (email.isBlank()) {
            return "Email is required."
        }

        if (password.isBlank()) {
            return "Password is required."
        }

        return null
    }

    private fun validateCreateAccount(
        username: String,
        email: String,
        password: String
    ): String? {
        if (username.isBlank()) {
            return "Username is required."
        }

        return validateSignIn(email, password)
    }
}