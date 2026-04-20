package com.adamjohnson.pokebinder.data.repository

import com.adamjohnson.pokebinder.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

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
            username = firebaseUser.displayName.orEmpty().trim(),
            email = firebaseUser.email.orEmpty().trim()
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
        username: String,
        onResult: (User?, String?) -> Unit
    ) {
        val trimmedUsername = username.trim()

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onResult(null, task.exception?.localizedMessage ?: "Unable to create account.")
                    return@addOnCompleteListener
                }

                val firebaseUser = firebaseAuth.currentUser
                if (firebaseUser == null) {
                    onResult(null, "Account created, but no user was returned.")
                    return@addOnCompleteListener
                }

                val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmedUsername)
                    .build()

                firebaseUser.updateProfile(profileUpdate)
                    .addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            val user = getCurrentUser()
                            if (user != null) {
                                onResult(user, null)
                            } else {
                                onResult(null, "Account created, but no user was returned.")
                            }
                        } else {
                            onResult(
                                null,
                                profileTask.exception?.localizedMessage
                                    ?: "Account created, but the username could not be saved."
                            )
                        }
                    }
            }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}