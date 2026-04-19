package com.adamjohnson.pokebinder.data.repository

import com.adamjohnson.pokebinder.data.model.SavedCard
import com.google.firebase.firestore.FirebaseFirestore

class CollectionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun getSavedCards(
        userId: String,
        onResult: (List<SavedCard>?, String?) -> Unit
    ) {
        if (userId.isBlank()) {
            onResult(null, "Invalid user.")
            return
        }

        savedCardsCollection(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val savedCards = snapshot.documents.map { document ->
                    SavedCard(
                        cardId = document.id,
                        savedAt = document.getLong(FIELD_SAVED_AT) ?: 0L
                    )
                }.sortedByDescending { it.savedAt }

                onResult(savedCards, null)
            }
            .addOnFailureListener { exception ->
                onResult(
                    null,
                    exception.localizedMessage ?: "Unable to load saved cards."
                )
            }
    }

    fun saveCard(
        userId: String,
        cardId: String,
        onResult: (String?) -> Unit = {}
    ) {
        if (userId.isBlank()) {
            onResult("Invalid user.")
            return
        }

        if (cardId.isBlank()) {
            onResult("Invalid card.")
            return
        }

        val savedCardData = hashMapOf<String, Any>(
            FIELD_SAVED_AT to System.currentTimeMillis()
        )

        savedCardsCollection(userId)
            .document(cardId)
            .set(savedCardData)
            .addOnSuccessListener {
                onResult(null)
            }
            .addOnFailureListener { exception ->
                onResult(exception.localizedMessage ?: "Unable to save card.")
            }
    }

    fun removeCard(
        userId: String,
        cardId: String,
        onResult: (String?) -> Unit = {}
    ) {
        if (userId.isBlank()) {
            onResult("Invalid user.")
            return
        }

        if (cardId.isBlank()) {
            onResult("Invalid card.")
            return
        }

        savedCardsCollection(userId)
            .document(cardId)
            .delete()
            .addOnSuccessListener {
                onResult(null)
            }
            .addOnFailureListener { exception ->
                onResult(exception.localizedMessage ?: "Unable to remove saved card.")
            }
    }

    private fun savedCardsCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(SAVED_CARDS_COLLECTION)

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val SAVED_CARDS_COLLECTION = "savedCards"
        private const val FIELD_SAVED_AT = "savedAt"
    }
}