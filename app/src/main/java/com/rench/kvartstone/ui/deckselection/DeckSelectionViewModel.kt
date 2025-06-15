package com.rench.kvartstone.ui.deckselection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rench.kvartstone.data.dao.DeckDao
import com.rench.kvartstone.data.repositories.DeckRepository
import com.rench.kvartstone.domain.Deck
import kotlinx.coroutines.launch

class DeckSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val deckRepository = DeckRepository(application)

    private val _decks = MutableLiveData<List<Deck>>()
    val decks: LiveData<List<Deck>> = _decks

    private val _selectedDeck = MutableLiveData<Deck?>()
    val selectedDeck: LiveData<Deck?> = _selectedDeck

    fun loadAvailableDecks() {
        viewModelScope.launch {
            try {
                deckRepository.allDecks.collect { deckList ->
                    _decks.value = deckList
                }
            } catch (e: Exception) {

                _decks.value = createDefaultDecks()
            }
        }
    }

    fun selectDeck(deck: Deck) {
        _selectedDeck.value = deck
    }
    fun deleteDeck(deck: Deck) = viewModelScope.launch {
        deckRepository.deleteDeckById(deck.id)
    }

    private fun createDefaultDecks(): List<Deck> {
        return listOf(
            Deck(
                id = 1,
                name = "Basic Deck",
                description = "A balanced starter deck",
                cards = emptyList(),
                heroClass = "neutral"
            ),
            Deck(
                id = 2,
                name = "Aggressive Deck",
                description = "Fast and aggressive",
                cards = emptyList(),
                heroClass = "warrior"
            )
        )
    }
}
