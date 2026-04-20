package com.adamjohnson.pokebinder.data.repository

import com.adamjohnson.pokebinder.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun createOrUpdateUser(
        user: User,
        onResult: (String?) -> Unit = {}
    ) {
        if (user.userId.isBlank()) {
            onResult("Invalid user.")
            return
        }

        val userData = hashMapOf<String, Any>(
            "userId" to user.userId
        )

        if (user.username.isNotBlank()) {
            userData["username"] = user.username
            userData["displayName"] = user.username
        }

        if (user.email.isNotBlank()) {
            userData["email"] = user.email
        }

        firestore.collection(USERS_COLLECTION)
            .document(user.userId)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                onResult(null)
            }
            .addOnFailureListener { exception ->
                onResult(exception.localizedMessage ?: "Unable to save user.")
            }
    }

    companion object {
        private const val USERS_COLLECTION = "users"
    }
}