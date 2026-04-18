package com.adamjohnson.pokebinder.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adamjohnson.pokebinder.data.model.BinderPage
import com.adamjohnson.pokebinder.utils.UiState

class BinderEditorViewModel : ViewModel() {

    private val _binderPageState = MutableLiveData<UiState<BinderPage>>(
        UiState.Success(BinderPage())
    )
    val binderPageState: LiveData<UiState<BinderPage>> = _binderPageState
}