package com.adamjohnson.pokebinder.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.adamjohnson.pokebinder.data.local.PokemonCardLocalDataSource
import com.adamjohnson.pokebinder.data.model.PokemonCard
import com.adamjohnson.pokebinder.data.repository.CardRepository
import com.adamjohnson.pokebinder.utils.UiState

class CardSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val cardRepository = CardRepository(
        PokemonCardLocalDataSource(application.applicationContext)
    )

    private val _cardsState = MutableLiveData<UiState<List<PokemonCard>>>(UiState.Loading)
    val cardsState: LiveData<UiState<List<PokemonCard>>> = _cardsState

    init {
        loadCards()
    }

    fun loadCards() {
        _cardsState.value = UiState.Success(cardRepository.getPokemonCards())
    }
}