package com.adamjohnson.pokebinder.data.repository

import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.data.model.BinderPageStatus
import com.adamjohnson.pokebinder.data.model.BinderSlot
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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
                    onResult(documentToBinderPage(userId, document), null)
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
                    documentToBinderPage(userId, document)
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

    fun getPublishedPages(): List<BinderPage> {
        return emptyList()
    }

    private fun draftPagesCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DRAFT_PAGES_COLLECTION)

    private fun draftPageToMap(binderPage: BinderPage): Map<String, Any> {
        val slotsData = normalizeSlots(binderPage.slots).map { slot ->
            val slotData = hashMapOf<String, Any>(
                FIELD_SLOT_INDEX to slot.index
            )

            if (!slot.cardId.isNullOrBlank()) {
                slotData[FIELD_SLOT_CARD_ID] = slot.cardId
            }

            slotData
        }

        return hashMapOf(
            FIELD_TITLE to binderPage.title,
            FIELD_SLOTS to slotsData,
            FIELD_CREATED_AT to binderPage.createdAt,
            FIELD_UPDATED_AT to binderPage.updatedAt
        )
    }

    private fun documentToBinderPage(
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
            updatedAt = document.getLong(FIELD_UPDATED_AT) ?: 0L
        )
    }

    private fun parseSlots(document: DocumentSnapshot): List<BinderSlot> {
        val rawSlots = document.get(FIELD_SLOTS) as? List<*> ?: emptyList()

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

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val DRAFT_PAGES_COLLECTION = "binderDraftPages"

        private const val FIELD_TITLE = "title"
        private const val FIELD_SLOTS = "slots"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"

        private const val FIELD_SLOT_INDEX = "index"
        private const val FIELD_SLOT_CARD_ID = "cardId"

        private const val SLOT_COUNT = 9
    }
}