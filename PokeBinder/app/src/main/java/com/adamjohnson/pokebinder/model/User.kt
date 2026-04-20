package com.adamjohnson.pokebinder.data.model

data class User(
    val userId: String = "",
    val username: String = "",
    val email: String = ""
) {
    val displayName: String
        get() = username
}