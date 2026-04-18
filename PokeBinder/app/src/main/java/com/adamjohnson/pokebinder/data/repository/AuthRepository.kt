package com.adamjohnson.pokebinder.data.repository

import com.adamjohnson.pokebinder.data.model.User
import com.google.firebase.auth.FirebaseAuth

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun isSignedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null

        return User(
            userId = firebaseUser.uid,
            displayName = firebaseUser.displayName.orEmpty()
        )
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}