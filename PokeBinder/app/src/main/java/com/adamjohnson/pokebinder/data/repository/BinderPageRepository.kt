package com.adamjohnson.pokebinder.data.repository

import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.BinderPageStatus
import com.adamjohnson.pokebinder.data.model.BinderSlot
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class BinderPageRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val authRepository = AuthRepository()

    fun saveDraftPage(
        userId: String,
        binderPage: BinderPage,
        onResult: (BinderPage?, String?) -> Unit = { _, _ -> }
    ) {
        if (userId.isBlank()) {
            onResult(null, "Invalid user.")
            return
        }

        val document = if (binderPage.pageId.isBlank()) {
            draftPagesCollection(userId).document()
        } else {
            draftPagesCollection(userId).document(binderPage.pageId)
        }

        val now = System.currentTimeMillis()
        val createdAt = if (binderPage.createdAt > 0L) {
            binderPage.createdAt
        } else {
            now
        }
        val creatorUsername = binderPage.creatorUsername.trim().ifBlank {
            authRepository.getCurrentUser()?.username.orEmpty().trim()
        }

        val savedDraftPage = binderPage.copy(
            pageId = document.id,
            ownerUserId = userId,
            creatorUsername = creatorUsername,
            title = binderPage.title.trim(),
            slots = normalizeSlots(binderPage.slots),
            status = BinderPageStatus.DRAFT,
            createdAt = createdAt,
            updatedAt = now,
            publishedAt = null,
            likeCount = 0,
            commentCount = 0
        )

        document.set(draftPageToMap(savedDraftPage), SetOptions.merge())
            .addOnSuccessListener {
                onResult(savedDraftPage, null)
            }
            .addOnFailureListener { exception ->
                onResult(
                    null,
                    exception.localizedMessage ?: "Unable to save draft page."
                )
            }
    }

    fun publishPage(
        userId: String,
        binderPage: BinderPage,
        onResult: (BinderPage?, String?) -> Unit = { _, _ -> }
    ) {
        if (userId.isBlank()) {
            onResult(null, "Invalid user.")
            return
        }

        if (binderPage.status != BinderPageStatus.DRAFT) {
            onResult(null, "Only draft pages can be published.")
            return
        }

        if (binderPage.ownerUserId.isNotBlank() && binderPage.ownerUserId != userId) {
            onResult(null, "Invalid user.")
            return
        }

        if (!hasAllSlotsFilled(binderPage)) {
            onResult(null, "All 9 slots must be filled before publishing.")
            return
        }

        val publishedDocument = if (binderPage.pageId.isBlank()) {
            publishedPagesCollection().document()
        } else {
            publishedPagesCollection().document(binderPage.pageId)
        }

        val now = System.currentTimeMillis()
        val createdAt = if (binderPage.createdAt > 0L) {
            binderPage.createdAt
        } else {
            now
        }
        val creatorUsername = binderPage.creatorUsername.trim().ifBlank {
            authRepository.getCurrentUser()?.username.orEmpty().trim()
        }

        val publishedBinderPage = binderPage.copy(
            pageId = publishedDocument.id,
            ownerUserId = userId,
            creatorUsername = creatorUsername,
            title = binderPage.title.trim(),
            slots = normalizeSlots(binderPage.slots),
            status = BinderPageStatus.PUBLISHED,
            createdAt = createdAt,
            updatedAt = now,
            publishedAt = now,
            likeCount = maxOf(binderPage.likeCount, 0),
            commentCount = maxOf(binderPage.commentCount, 0)
        )

        firestore.runTransaction { transaction ->
            transaction.set(
                publishedDocument,
                publishedPageToMap(publishedBinderPage)
            )

            if (binderPage.pageId.isNotBlank()) {
                val draftDocument = draftPagesCollection(userId).document(binderPage.pageId)
                transaction.delete(draftDocument)
            }
        }.addOnSuccessListener {
            onResult(publishedBinderPage, null)
        }.addOnFailureListener { exception ->
            onResult(
                null,
                exception.localizedMessage ?: "Unable to publish binder page."
            )
        }
    }

    fun getDraftPages(
        userId: String,
        onResult: (List<BinderPage>?, String?) -> Unit
    ) {
        if (userId.isBlank()) {
            onResult(null, "Invalid user.")
            return
        }

        draftPagesCollection(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val draftPages = snapshot.documents.map { document ->
                    documentToDraftPage(userId, document)
                }.sortedByDescending { it.updatedAt }

                onResult(draftPages, null)
            }
            .addOnFailureListener { exception ->
                onResult(
                    null,
                    exception.localizedMessage ?: "Unable to load draft pages."
                )
            }
    }

    fun getDraftPage(
        userId: String,
        pageId: String,
        onResult: (BinderPage?, String?) -> Unit
    ) {
        if (userId.isBlank()) {
            onResult(null, "Invalid user.")
            return
        }

        if (pageId.isBlank()) {
            onResult(null, "Invalid page.")
            return
        }

        draftPagesCollection(userId)
            .document(pageId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onResult(documentToDraftPage(userId, document), null)
                } else {
                    onResult(null, "Draft page not found.")
                }
            }
            .addOnFailureListener { exception ->
                onResult(
                    null,
                    exception.localizedMessage ?: "Unable to load draft page."
                )
            }
    }

    fun getPublishedPage(
        pageId: String,
        onResult: (BinderPage?, String?) -> Unit
    ) {
        if (pageId.isBlank()) {
            onResult(null, "Invalid page.")
            return
        }

        publishedPagesCollection()
            .document(pageId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onResult(documentToPublishedPage(document), null)
                } else {
                    onResult(null, "Published page not found.")
                }
            }
            .addOnFailureListener { exception ->
                onResult(
                    null,
                    exception.localizedMessage ?: "Unable to load published page."
                )
            }
    }

    fun listenToPublishedPage(
        pageId: String,
        onResult: (BinderPage?, String?) -> Unit
    ): ListenerRegistration? {
        if (pageId.isBlank()) {
            onResult(null, "Invalid page.")
            return null
        }

        return publishedPagesCollection()
            .document(pageId)
            .addSnapshotListener { document, exception ->
                if (exception != null) {
                    onResult(
                        null,
                        exception.localizedMessage ?: "Unable to load published page."
                    )
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    onResult(documentToPublishedPage(document), null)
                } else {
                    onResult(null, "Published page not found.")
                }
            }
    }

    fun getPublishedPages(
        onResult: (List<BinderPage>?, String?) -> Unit
    ) {
        publishedPagesCollection()
            .get()
            .addOnSuccessListener { snapshot ->
                val publishedPages = sortPublishedPages(
                    snapshot.documents.map { document ->
                        documentToPublishedPage(document)
                    }
                )

                onResult(publishedPages, null)
            }
            .addOnFailureListener { exception ->
                onResult(
                    null,
                    exception.localizedMessage ?: "Unable to load published pages."
                )
            }
    }

    fun listenToPublishedPages(
        onResult: (List<BinderPage>?, String?) -> Unit
    ): ListenerRegistration {
        return publishedPagesCollection()
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    onResult(
                        null,
                        exception.localizedMessage ?: "Unable to load published pages."
                    )
                    return@addSnapshotListener
                }

                val publishedPages = sortPublishedPages(
                    snapshot?.documents.orEmpty().map { document ->
                        documentToPublishedPage(document)
                    }
                )

                onResult(publishedPages, null)
            }
    }

    fun getPublishedPagesByIds(
        pageIds: List<String>,
        onResult: (List<BinderPage>?, String?) -> Unit
    ) {
        val normalizedPageIds = pageIds.map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()

        if (normalizedPageIds.isEmpty()) {
            onResult(emptyList(), null)
            return
        }

        getPublishedPages { publishedPages, errorMessage ->
            if (publishedPages != null) {
                onResult(
                    publishedPages.filter { normalizedPageIds.contains(it.pageId) },
                    null
                )
            } else {
                onResult(
                    null,
                    errorMessage ?: "Unable to load published pages."
                )
            }
        }
    }

    fun getPublishedPagesForUser(
        userId: String,
        onResult: (List<BinderPage>?, String?) -> Unit
    ) {
        if (userId.isBlank()) {
            onResult(null, "Invalid user.")
            return
        }

        publishedPagesCollection()
            .whereEqualTo(FIELD_OWNER_USER_ID, userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val publishedPages = sortPublishedPages(
                    snapshot.documents.map { document ->
                        documentToPublishedPage(document)
                    }
                )

                onResult(publishedPages, null)
            }
            .addOnFailureListener { exception ->
                onResult(
                    null,
                    exception.localizedMessage ?: "Unable to load published pages."
                )
            }
    }

    fun deleteDraftPage(
        userId: String,
        pageId: String,
        onResult: (String?) -> Unit = {}
    ) {
        if (userId.isBlank()) {
            onResult("Invalid user.")
            return
        }

        if (pageId.isBlank()) {
            onResult("Invalid page.")
            return
        }

        draftPagesCollection(userId)
            .document(pageId)
            .delete()
            .addOnSuccessListener {
                onResult(null)
            }
            .addOnFailureListener { exception ->
                onResult(
                    exception.localizedMessage ?: "Unable to delete draft page."
                )
            }
    }

    fun deletePublishedPage(
        userId: String,
        pageId: String,
        onResult: (String?) -> Unit = {}
    ) {
        if (userId.isBlank()) {
            onResult("Invalid user.")
            return
        }

        if (pageId.isBlank()) {
            onResult("Invalid page.")
            return
        }

        val pageDocument = publishedPagesCollection().document(pageId)

        pageDocument.get()
            .addOnSuccessListener { pageSnapshot ->
                if (!pageSnapshot.exists()) {
                    onResult("Published page not found.")
                    return@addOnSuccessListener
                }

                val ownerUserId = pageSnapshot.getString(FIELD_OWNER_USER_ID).orEmpty()
                if (ownerUserId != userId) {
                    onResult("Invalid user.")
                    return@addOnSuccessListener
                }

                deletePublishedPageWithChildren(pageDocument, onResult)
            }
            .addOnFailureListener { exception ->
                onResult(
                    exception.localizedMessage ?: "Unable to delete published page."
                )
            }
    }

    private fun draftPagesCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DRAFT_PAGES_COLLECTION)

    private fun publishedPagesCollection() =
        firestore.collection(PUBLISHED_PAGES_COLLECTION)

    private fun likesCollection(pageId: String) =
        publishedPagesCollection()
            .document(pageId)
            .collection(LIKES_COLLECTION)

    private fun commentsCollection(pageId: String) =
        publishedPagesCollection()
            .document(pageId)
            .collection(COMMENTS_COLLECTION)

    private fun deletePublishedPageWithChildren(
        pageDocument: DocumentReference,
        onResult: (String?) -> Unit
    ) {
        val pageId = pageDocument.id

        likesCollection(pageId)
            .get()
            .addOnSuccessListener { likesSnapshot ->
                commentsCollection(pageId)
                    .get()
                    .addOnSuccessListener { commentsSnapshot ->
                        val batch = firestore.batch()

                        likesSnapshot.documents.forEach { document ->
                            batch.delete(document.reference)
                        }

                        commentsSnapshot.documents.forEach { document ->
                            batch.delete(document.reference)
                        }

                        batch.delete(pageDocument)

                        batch.commit()
                            .addOnSuccessListener {
                                onResult(null)
                            }
                            .addOnFailureListener { exception ->
                                onResult(
                                    exception.localizedMessage
                                        ?: "Unable to delete published page."
                                )
                            }
                    }
                    .addOnFailureListener { exception ->
                        onResult(
                            exception.localizedMessage
                                ?: "Unable to delete published page comments."
                        )
                    }
            }
            .addOnFailureListener { exception ->
                onResult(
                    exception.localizedMessage ?: "Unable to delete published page likes."
                )
            }
    }

    private fun draftPageToMap(binderPage: BinderPage): Map<String, Any> {
        val pageData = hashMapOf<String, Any>(
            FIELD_TITLE to binderPage.title,
            FIELD_SLOTS to slotsToMap(binderPage.slots),
            FIELD_CREATED_AT to binderPage.createdAt,
            FIELD_UPDATED_AT to binderPage.updatedAt
        )

        if (binderPage.creatorUsername.isNotBlank()) {
            pageData[FIELD_CREATOR_USERNAME] = binderPage.creatorUsername
        }

        return pageData
    }

    private fun publishedPageToMap(binderPage: BinderPage): Map<String, Any> {
        return hashMapOf(
            FIELD_OWNER_USER_ID to binderPage.ownerUserId,
            FIELD_CREATOR_USERNAME to binderPage.creatorUsername,
            FIELD_TITLE to binderPage.title,
            FIELD_SLOTS to slotsToMap(binderPage.slots),
            FIELD_STATUS to BinderPageStatus.PUBLISHED.name,
            FIELD_CREATED_AT to binderPage.createdAt,
            FIELD_UPDATED_AT to binderPage.updatedAt,
            FIELD_PUBLISHED_AT to (binderPage.publishedAt ?: binderPage.updatedAt),
            FIELD_LIKE_COUNT to maxOf(binderPage.likeCount, 0),
            FIELD_COMMENT_COUNT to maxOf(binderPage.commentCount, 0)
        )
    }

    private fun slotsToMap(slots: List<BinderSlot>): List<Map<String, Any>> {
        return normalizeSlots(slots).map { slot ->
            val slotData = hashMapOf<String, Any>(
                FIELD_SLOT_INDEX to slot.index
            )

            if (!slot.cardId.isNullOrBlank()) {
                slotData[FIELD_SLOT_CARD_ID] = slot.cardId
            }

            slotData
        }
    }

    private fun documentToDraftPage(
        userId: String,
        document: DocumentSnapshot
    ): BinderPage {
        return BinderPage(
            pageId = document.id,
            ownerUserId = userId,
            creatorUsername = document.getString(FIELD_CREATOR_USERNAME).orEmpty(),
            title = document.getString(FIELD_TITLE).orEmpty(),
            slots = parseSlots(document),
            status = BinderPageStatus.DRAFT,
            createdAt = document.getLong(FIELD_CREATED_AT) ?: 0L,
            updatedAt = document.getLong(FIELD_UPDATED_AT) ?: 0L,
            publishedAt = null,
            likeCount = 0,
            commentCount = 0
        )
    }

    private fun documentToPublishedPage(document: DocumentSnapshot): BinderPage {
        val updatedAt = document.getLong(FIELD_UPDATED_AT) ?: 0L

        return BinderPage(
            pageId = document.id,
            ownerUserId = document.getString(FIELD_OWNER_USER_ID).orEmpty(),
            creatorUsername = document.getString(FIELD_CREATOR_USERNAME).orEmpty(),
            title = document.getString(FIELD_TITLE).orEmpty(),
            slots = parseSlots(document),
            status = BinderPageStatus.PUBLISHED,
            createdAt = document.getLong(FIELD_CREATED_AT) ?: 0L,
            updatedAt = updatedAt,
            publishedAt = document.getLong(FIELD_PUBLISHED_AT) ?: updatedAt,
            likeCount = (document.getLong(FIELD_LIKE_COUNT) ?: 0L).toInt(),
            commentCount = (document.getLong(FIELD_COMMENT_COUNT) ?: 0L).toInt()
        )
    }

    private fun parseSlots(document: DocumentSnapshot): List<BinderSlot> {
        val rawSlots = document.get(FIELD_SLOTS) as? List<*> ?: emptyList<Any?>()

        val loadedSlots = rawSlots.mapIndexedNotNull { position, rawSlot ->
            val slotMap = rawSlot as? Map<*, *> ?: return@mapIndexedNotNull null

            val index = (slotMap[FIELD_SLOT_INDEX] as? Number)?.toInt() ?: position
            val cardId = slotMap[FIELD_SLOT_CARD_ID] as? String

            BinderSlot(
                index = index,
                cardId = cardId
            )
        }

        return normalizeSlots(loadedSlots)
    }

    private fun normalizeSlots(slots: List<BinderSlot>): List<BinderSlot> {
        val slotsByIndex = slots.associateBy { it.index }

        return List(SLOT_COUNT) { index ->
            BinderSlot(
                index = index,
                cardId = slotsByIndex[index]?.cardId
            )
        }
    }

    private fun sortPublishedPages(publishedPages: List<BinderPage>): List<BinderPage> {
        return publishedPages.sortedWith(
            compareByDescending<BinderPage> { it.publishedAt ?: 0L }
                .thenByDescending { it.updatedAt }
        )
    }

    private fun hasAllSlotsFilled(binderPage: BinderPage): Boolean {
        val normalizedSlots = normalizeSlots(binderPage.slots)

        return normalizedSlots.all { slot ->
            !slot.cardId.isNullOrBlank()
        }
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val DRAFT_PAGES_COLLECTION = "binderDraftPages"
        private const val PUBLISHED_PAGES_COLLECTION = "binderPublishedPages"
        private const val LIKES_COLLECTION = "likes"
        private const val COMMENTS_COLLECTION = "comments"

        private const val FIELD_OWNER_USER_ID = "ownerUserId"
        private const val FIELD_CREATOR_USERNAME = "creatorUsername"
        private const val FIELD_TITLE = "title"
        private const val FIELD_SLOTS = "slots"
        private const val FIELD_STATUS = "status"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"
        private const val FIELD_PUBLISHED_AT = "publishedAt"
        private const val FIELD_LIKE_COUNT = "likeCount"
        private const val FIELD_COMMENT_COUNT = "commentCount"

        private const val FIELD_SLOT_INDEX = "index"
        private const val FIELD_SLOT_CARD_ID = "cardId"

        private const val SLOT_COUNT = 9
    }
}