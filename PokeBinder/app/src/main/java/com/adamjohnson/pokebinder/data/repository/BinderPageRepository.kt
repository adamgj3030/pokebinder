package com.adamjohnson.pokebinder.data.repository

import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.BinderPageStatus
import com.adamjohnson.pokebinder.data.model.BinderSlot
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
        val createdAt = if (binderPage.createdAt > 0L) {
            binderPage.createdAt
        } else {
            now
        }

        val publishedPage = binderPage.copy(
            pageId = document.id,
            ownerUserId = userId,
            title = binderPage.title.trim(),
            slots = normalizeSlots(binderPage.slots),
            status = BinderPageStatus.PUBLISHED,
            createdAt = createdAt,
            updatedAt = now,
            publishedAt = now,
            likeCount = binderPage.likeCount,
            commentCount = binderPage.commentCount
        )

        val batch = firestore.batch()
        batch.set(document, publishedPageToMap(publishedPage), SetOptions.merge())

        if (binderPage.pageId.isNotBlank()) {
            batch.delete(draftPagesCollection(userId).document(binderPage.pageId))
        }

        batch.commit()
            .addOnSuccessListener {
                onResult(publishedPage, null)
            }
            .addOnFailureListener { exception ->
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
            .orderBy(FIELD_PUBLISHED_AT, Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val publishedPages = snapshot.documents.map { document ->
                    documentToPublishedPage(document)
                }

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
            FIELD_STATUS to binderPage.status.name,
            FIELD_CREATED_AT to binderPage.createdAt,
            FIELD_UPDATED_AT to binderPage.updatedAt,
            FIELD_PUBLISHED_AT to (binderPage.publishedAt ?: binderPage.updatedAt),
            FIELD_LIKE_COUNT to binderPage.likeCount,
            FIELD_COMMENT_COUNT to binderPage.commentCount
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
        return BinderPage(
            pageId = document.id,
            ownerUserId = document.getString(FIELD_OWNER_USER_ID).orEmpty(),
            title = document.getString(FIELD_TITLE).orEmpty(),
            slots = parseSlots(document),
            status = parseStatus(
                document.getString(FIELD_STATUS),
                BinderPageStatus.PUBLISHED
            ),
            createdAt = document.getLong(FIELD_CREATED_AT) ?: 0L,
            updatedAt = document.getLong(FIELD_UPDATED_AT) ?: 0L,
            publishedAt = document.getLong(FIELD_PUBLISHED_AT),
            likeCount = (document.getLong(FIELD_LIKE_COUNT) ?: 0L).toInt(),
            commentCount = (document.getLong(FIELD_COMMENT_COUNT) ?: 0L).toInt()
        )
    }

    private fun parseStatus(
        rawStatus: String?,
        defaultStatus: BinderPageStatus
    ): BinderPageStatus {
        return try {
            if (rawStatus.isNullOrBlank()) {
                defaultStatus
            } else {
                BinderPageStatus.valueOf(rawStatus)
            }
        } catch (exception: IllegalArgumentException) {
            defaultStatus
        }
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