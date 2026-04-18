package com.adamjohnson.pokebinder.model

data class BinderPage(
    val pageId: String = "",
    val ownerUserId: String = "",
    val title: String = "",
    val slots: List<BinderSlot> = List(9) { index -> BinderSlot(index = index) },
    val status: BinderPageStatus = BinderPageStatus.DRAFT,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val publishedAt: Long? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0
)