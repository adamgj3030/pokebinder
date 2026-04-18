package com.adamjohnson.pokebinder.model

data class Comment(
    val commentId: String = "",
    val pageId: String = "",
    val userId: String = "",
    val userDisplayName: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)