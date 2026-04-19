package com.adamjohnson.pokebinder.data.repository

import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.BinderPageStatus
import com.adamjohnson.pokebinder.data.model.BinderSlot
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions

class BinderPageRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

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

        val savedDraftPage = binderPage.copy(
            pageId = document.id,
            ownerUserId = userId,
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

        val document = if (binderPage.pageId.isBlank()) {
            publishedPagesCollection().document()
        } else {
            publishedPagesCollection().document(binderPage.pageId)
        }

        val now = System.currentTimeMillis()

        firestore.runTransaction<BinderPage> { transaction ->
            val existingPublishedSnapshot = transaction.get(document)
            val existingPublishedPage = if (existingPublishedSnapshot.exists()) {
                documentToPublishedPage(existingPublishedSnapshot)
            } else {
                null
            }

            if (
                existingPublishedPage != null &&
                existingPublishedPage.ownerUserId.isNotBlank() &&
                existingPublishedPage.ownerUserId != userId
            ) {
                throw FirebaseFirestoreException(
                    "You cannot publish over another user's page.",
                    FirebaseFirestoreException.Code.PERMISSION_DENIED
                )
            }

            val createdAt =
                existingPublishedPage?.createdAt?.takeIf { it > 0L }
                    ?: binderPage.createdAt.takeIf { it > 0L }
                    ?: now

            val publishedAt =
                existingPublishedPage?.publishedAt?.takeIf { it > 0L }
                    ?: binderPage.publishedAt?.takeIf { it > 0L }
                    ?: now

            val likeCount = existingPublishedPage?.likeCount ?: 0
            val commentCount = existingPublishedPage?.commentCount ?: 0

            val publishedPage = binderPage.copy(
                pageId = document.id,
                ownerUserId = userId,
                title = binderPage.title.trim(),
                slots = normalizeSlots(binderPage.slots),
                status = BinderPageStatus.PUBLISHED,
                createdAt = createdAt,
                updatedAt = now,
                publishedAt = publishedAt,
                likeCount = likeCount,
                commentCount = commentCount
            )

            transaction.set(document, publishedPageToMap(publishedPage), SetOptions.merge())

            if (binderPage.pageId.isNotBlank()) {
                transaction.delete(draftPagesCollection(userId).document(binderPage.pageId))
            }

            publishedPage
        }.addOnSuccessListener { publishedPage ->
            onResult(publishedPage, null)
        }.addOnFailureListener { exception ->
            onResult(
                null,
                exception.localizedMessage ?: "Unable to publish binder page."
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

    fun getEditorPage(
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
            .addOnSuccessListener { draftDocument ->
                if (draftDocument.exists()) {
                    onResult(documentToDraftPage(userId, draftDocument), null)
                } else {
                    publishedPagesCollection()
                        .document(pageId)
                        .get()
                        .addOnSuccessListener { publishedDocument ->
                            if (!publishedDocument.exists()) {
                                onResult(null, "Page not found.")
                                return@addOnSuccessListener
                            }

                            val publishedPage = documentToPublishedPage(publishedDocument)
                            if (
                                publishedPage.ownerUserId.isNotBlank() &&
                                publishedPage.ownerUserId != userId
                            ) {
                                onResult(
                                    null,
                                    "You cannot open another user's published page in the editor."
                                )
                            } else {
                                onResult(publishedPage, null)
                            }
                        }
                        .addOnFailureListener { exception ->
                            onResult(
                                null,
                                exception.localizedMessage ?: "Unable to load binder page."
                            )
                        }
                }
            }
            .addOnFailureListener { exception ->
                onResult(
                    null,
                    exception.localizedMessage ?: "Unable to load binder page."
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

    private fun draftPagesCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DRAFT_PAGES_COLLECTION)

    private fun publishedPagesCollection() =
        firestore.collection(PUBLISHED_PAGES_COLLECTION)

    private fun draftPageToMap(binderPage: BinderPage): Map<String, Any> {
        return hashMapOf(
            FIELD_TITLE to binderPage.title,
            FIELD_SLOTS to slotsToMap(binderPage.slots),
            FIELD_CREATED_AT to binderPage.createdAt,
            FIELD_UPDATED_AT to binderPage.updatedAt
        )
    }

    private fun publishedPageToMap(binderPage: BinderPage): Map<String, Any> {
        return hashMapOf(
            FIELD_OWNER_USER_ID to binderPage.ownerUserId,
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

        private const val FIELD_OWNER_USER_ID = "ownerUserId"
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