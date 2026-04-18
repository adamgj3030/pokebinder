package com.adamjohnson.pokebinder.data.repository

import com.adamjohnson.pokebinder.data.model.BinderPage

class BinderPageRepository {

    fun getDraftPages(userId: String): List<BinderPage> {
        return emptyList()
    }

    fun getPublishedPages(): List<BinderPage> {
        return emptyList()
    }
}