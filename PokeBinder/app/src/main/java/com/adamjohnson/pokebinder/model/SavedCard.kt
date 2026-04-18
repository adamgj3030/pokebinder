package com.adamjohnson.pokebinder.data.model

data class SavedCard(
    val userId: String = "",
    val cardId: String = "",
    val savedAt: Long = 0L
)