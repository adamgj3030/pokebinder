package com.adamjohnson.pokebinder.data.repository

import com.adamjohnson.pokebinder.data.model.Comment
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query

class SocialRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun getComments(
        pageId: String,
        onResult: (List<Comment>?, String?) -> Unit
    ) {
        if (pageId.isBlank()) {
            onResult(null, "Invalid page.")
            return
        }

        validatePublishedPage(pageId) { validationMessage ->
            if (validationMessage != null) {
                onResult(null, validationMessage)
                return@validatePublishedPage
            }

            commentsCollection(pageId)
                .orderBy(FIELD_CREATED_AT, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener { snapshot ->
                    val comments = snapshot.documents.map { document ->
                        documentToComment(pageId, document)
                    }

                    onResult(comments, null)
                }
                .addOnFailureListener { exception ->
                    onResult(
                        null,
                        exception.localizedMessage ?: "Unable to load comments."
                    )
                }
        }
    }

    fun addComment(
        pageId: String,
        userId: String,
        userDisplayName: String,
        text: String,
        onResult: (Comment?, String?) -> Unit = { _, _ -> }
    ) {
        if (pageId.isBlank()) {
            onResult(null, "Invalid page.")
            return
        }

        if (userId.isBlank()) {
            onResult(null, "Invalid user.")
            return
        }

        val normalizedText = text.trim()
        if (normalizedText.isBlank()) {
            onResult(null, "Comment text is required.")
            return
        }

        val normalizedDisplayName = userDisplayName.trim().ifBlank {
            DEFAULT_DISPLAY_NAME
        }

        firestore.runTransaction<Comment> { transaction ->
            val pageDocument = publishedPageDocument(pageId)
            val pageSnapshot = transaction.get(pageDocument)

            if (!pageSnapshot.exists()) {
                throw FirebaseFirestoreException(
                    "Published page not found.",
                    FirebaseFirestoreException.Code.ABORTED
                )
            }

            val commentDocument = commentsCollection(pageId).document()
            val now = System.currentTimeMillis()

            val comment = Comment(
                commentId = commentDocument.id,
                pageId = pageId,
                userId = userId,
                userDisplayName = normalizedDisplayName,
                text = normalizedText,
                createdAt = now
            )

            val currentCommentCount =
                (pageSnapshot.getLong(FIELD_COMMENT_COUNT) ?: 0L).toInt()
            val updatedCommentCount = currentCommentCount + 1

            transaction.set(commentDocument, commentToMap(comment))
            transaction.update(pageDocument, FIELD_COMMENT_COUNT, updatedCommentCount)

            comment
        }.addOnSuccessListener { comment ->
            onResult(comment, null)
        }.addOnFailureListener { exception ->
            onResult(
                null,
                exception.localizedMessage ?: "Unable to add comment."
            )
        }
    }

    fun isPageLikedByUser(
        pageId: String,
        userId: String,
        onResult: (Boolean?, String?) -> Unit
    ) {
        if (pageId.isBlank()) {
            onResult(null, "Invalid page.")
            return
        }

        if (userId.isBlank()) {
            onResult(null, "Invalid user.")
            return
        }

        validatePublishedPage(pageId) { validationMessage ->
            if (validationMessage != null) {
                onResult(null, validationMessage)
                return@validatePublishedPage
            }

            likesCollection(pageId)
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    onResult(document.exists(), null)
                }
                .addOnFailureListener { exception ->
                    onResult(
                        null,
                        exception.localizedMessage ?: "Unable to check page like status."
                    )
                }
        }
    }

    fun toggleLike(
        pageId: String,
        userId: String,
        onResult: (Boolean?, String?) -> Unit = { _, _ -> }
    ) {
        if (pageId.isBlank()) {
            onResult(null, "Invalid page.")
            return
        }

        if (userId.isBlank()) {
            onResult(null, "Invalid user.")
            return
        }

        firestore.runTransaction<Boolean> { transaction ->
            val pageDocument = publishedPageDocument(pageId)
            val pageSnapshot = transaction.get(pageDocument)

            if (!pageSnapshot.exists()) {
                throw FirebaseFirestoreException(
                    "Published page not found.",
                    FirebaseFirestoreException.Code.ABORTED
                )
            }

            val likeDocument = likesCollection(pageId).document(userId)
            val likeSnapshot = transaction.get(likeDocument)

            val currentLikeCount =
                (pageSnapshot.getLong(FIELD_LIKE_COUNT) ?: 0L).toInt()

            if (likeSnapshot.exists()) {
                val updatedLikeCount = maxOf(currentLikeCount - 1, 0)
                transaction.delete(likeDocument)
                transaction.update(pageDocument, FIELD_LIKE_COUNT, updatedLikeCount)
                false
            } else {
                val likeData = hashMapOf<String, Any>(
                    FIELD_USER_ID to userId,
                    FIELD_CREATED_AT to System.currentTimeMillis()
                )

                val updatedLikeCount = currentLikeCount + 1
                transaction.set(likeDocument, likeData)
                transaction.update(pageDocument, FIELD_LIKE_COUNT, updatedLikeCount)
                true
            }
        }.addOnSuccessListener { isNowLiked ->
            onResult(isNowLiked, null)
        }.addOnFailureListener { exception ->
            onResult(
                null,
                exception.localizedMessage ?: "Unable to update page like."
            )
        }
    }

    private fun validatePublishedPage(
        pageId: String,
        onResult: (String?) -> Unit
    ) {
        publishedPageDocument(pageId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onResult(null)
                } else {
                    onResult("Published page not found.")
                }
            }
            .addOnFailureListener { exception ->
                onResult(
                    exception.localizedMessage ?: "Unable to load published page."
                )
            }
    }

    private fun commentToMap(comment: Comment): Map<String, Any> {
        return hashMapOf(
            FIELD_PAGE_ID to comment.pageId,
            FIELD_USER_ID to comment.userId,
            FIELD_USER_DISPLAY_NAME to comment.userDisplayName,
            FIELD_TEXT to comment.text,
            FIELD_CREATED_AT to comment.createdAt
        )
    }

    private fun documentToComment(
        pageId: String,
        document: DocumentSnapshot
    ): Comment {
        return Comment(
            commentId = document.id,
            pageId = pageId,
            userId = document.getString(FIELD_USER_ID).orEmpty(),
            userDisplayName = document.getString(FIELD_USER_DISPLAY_NAME).orEmpty(),
            text = document.getString(FIELD_TEXT).orEmpty(),
            createdAt = document.getLong(FIELD_CREATED_AT) ?: 0L
        )
    }

    private fun publishedPageDocument(pageId: String) =
        firestore.collection(PUBLISHED_PAGES_COLLECTION)
            .document(pageId)

    private fun likesCollection(pageId: String) =
        publishedPageDocument(pageId)
            .collection(LIKES_COLLECTION)

    private fun commentsCollection(pageId: String) =
        publishedPageDocument(pageId)
            .collection(COMMENTS_COLLECTION)

    companion object {
        private const val PUBLISHED_PAGES_COLLECTION = "binderPublishedPages"
        private const val LIKES_COLLECTION = "likes"
        private const val COMMENTS_COLLECTION = "comments"

        private const val FIELD_PAGE_ID = "pageId"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_USER_DISPLAY_NAME = "userDisplayName"
        private const val FIELD_TEXT = "text"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_LIKE_COUNT = "likeCount"
        private const val FIELD_COMMENT_COUNT = "commentCount"

        private const val DEFAULT_DISPLAY_NAME = "User"
    }
}