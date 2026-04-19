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

    fun signIn(
        email: String,
        password: String,
        onResult: (User?, String?) -> Unit
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = getCurrentUser()
                    if (user != null) {
                        onResult(user, null)
                    } else {
                        onResult(null, "Sign in succeeded, but no user was returned.")
                    }
                } else {
                    onResult(null, task.exception?.localizedMessage ?: "Unable to sign in.")
                }
            }
    }

    fun createAccount(
        email: String,
        password: String,
        onResult: (User?, String?) -> Unit
    ) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = getCurrentUser()
                    if (user != null) {
                        onResult(user, null)
                    } else {
                        onResult(null, "Account created, but no user was returned.")
                    }
                } else {
                    onResult(null, task.exception?.localizedMessage ?: "Unable to create account.")
                }
            }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}