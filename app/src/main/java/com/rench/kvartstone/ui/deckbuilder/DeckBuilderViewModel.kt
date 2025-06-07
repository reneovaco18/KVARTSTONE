package com.rench.kvartstone.ui.deckbuilder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rench.kvartstone.data.entities.CardEntity
import com.rench.kvartstone.data.entities.DeckEntity
import com.rench.kvartstone.data.repositories.CardRepository
import com.rench.kvartstone.data.repositories.DeckRepository
import kotlinx.coroutines.launch
import org.json.JSONArray

data class DeckCompositionItem(
    val card: CardEntity,
    val count: Int
)

class DeckBuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val cardRepository = CardRepository(application)
    private val deckRepository = DeckRepository(application)

    // LiveData for UI updates
    private val _availableCards = MutableLiveData<List<CardEntity>>(emptyList())
    val availableCards: LiveData<List<CardEntity>> = _availableCards

    private val _deckComposition = MutableLiveData<List<DeckCompositionItem>>(emptyList())
    val deckComposition: LiveData<List<DeckCompositionItem>> = _deckComposition

    private val _deckCount = MutableLiveData(0)
    val deckCount: LiveData<Int> = _deckCount

    private val _deckName = MutableLiveData("New Deck")
    val deckName: LiveData<String> = _deckName

    private val _message = MutableLiveData("")
    val message: LiveData<String> = _message

    private val _deckSaved = MutableLiveData(false)
    val deckSaved: LiveData<Boolean> = _deckSaved

    // Internal state management
    private var allCards = listOf<CardEntity>()
    private var filteredCards = listOf<CardEntity>()
    private val currentDeckCards = mutableMapOf<Int, Int>() // cardId to count mapping
    private var currentDeckId: Int? = null

    companion object {
        private const val MAX_DECK_SIZE = 30
        private const val MAX_CARD_COPIES = 2
    }

    init {
        loadAllCards()
    }

    private fun loadAllCards() {
        viewModelScope.launch {
            try {
                cardRepository.allCards.collect { domainCards ->
                    // Convert domain cards to entities for UI display
                    allCards = domainCards.map { card ->
                        CardEntity(
                            id = card.id,
                            name = card.name,
                            description = when (card) {
                                is com.rench.kvartstone.domain.MinionCard -> "A ${card.attack}/${card.maxHealth} minion"
                                is com.rench.kvartstone.domain.SpellCard -> card.description
                                else -> "Unknown card type"
                            },
                            type = when (card) {
                                is com.rench.kvartstone.domain.MinionCard -> "minion"
                                is com.rench.kvartstone.domain.SpellCard -> "spell"
                                else -> "unknown"
                            },
                            manaCost = card.manaCost,
                            attack = if (card is com.rench.kvartstone.domain.MinionCard) card.attack else null,
                            health = if (card is com.rench.kvartstone.domain.MinionCard) card.maxHealth else null,
                            effect = null,
                            imageResName = "ic_card_generic",
                            rarity = "common",
                            isCustom = false,
                            createdAt = System.currentTimeMillis()
                        )
                    }
                    filteredCards = allCards
                    _availableCards.value = filteredCards
                }
            } catch (e: Exception) {
                _message.value = "Failed to load cards: ${e.message}"
            }
        }
    }

    fun addCardToDeck(card: CardEntity) {
        val currentCount = currentDeckCards[card.id] ?: 0
        val totalCards = currentDeckCards.values.sum()

        when {
            totalCards >= MAX_DECK_SIZE -> {
                _message.value = "Deck is full! (Maximum $MAX_DECK_SIZE cards)"
            }
            currentCount >= MAX_CARD_COPIES -> {
                _message.value = "Maximum $MAX_CARD_COPIES copies of ${card.name} allowed"
            }
            else -> {
                currentDeckCards[card.id] = currentCount + 1
                updateDeckComposition()
                _message.value = "${card.name} added to deck"
            }
        }
    }

    fun removeCardFromDeck(cardId: Int) {
        val currentCount = currentDeckCards[cardId] ?: 0
        if (currentCount > 1) {
            currentDeckCards[cardId] = currentCount - 1
        } else {
            currentDeckCards.remove(cardId)
        }
        updateDeckComposition()

        val card = allCards.find { it.id == cardId }
        _message.value = "${card?.name ?: "Card"} removed from deck"
    }

    fun getCardCountInDeck(cardId: Int): Int {
        return currentDeckCards[cardId] ?: 0
    }
    private fun updateDeckComposition() {
        val composition = currentDeckCards.mapNotNull { (cardId, count) ->
            val card = allCards.find { it.id == cardId }
            if (card != null) {
                DeckCompositionItem(card, count)
            } else null
        }.sortedBy { it.card.manaCost }

        _deckComposition.value = composition
        _deckCount.value = currentDeckCards.values.sum()
    }

    fun searchCards(query: String) {
        filteredCards = if (query.isEmpty()) {
            allCards
        } else {
            allCards.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
        }
        _availableCards.value = filteredCards
    }

    fun filterByType(type: String) {
        filteredCards = allCards.filter { it.type == type }
        _availableCards.value = filteredCards
    }

    fun filterByManaCost(minCost: Int, maxCost: Int) {
        filteredCards = allCards.filter { it.manaCost in minCost..maxCost }
        _availableCards.value = filteredCards
    }

    fun clearFilters() {
        filteredCards = allCards
        _availableCards.value = filteredCards
    }

    fun setDeckName(name: String) {
        _deckName.value = name
    }

    fun clearDeck() {
        currentDeckCards.clear()
        updateDeckComposition()
        _message.value = "Deck cleared"
    }

    fun saveDeck() {
        val name = _deckName.value ?: "New Deck"
        val totalCards = currentDeckCards.values.sum()

        if (totalCards != MAX_DECK_SIZE) {
            _message.value = "Deck must have exactly $MAX_DECK_SIZE cards"
            return
        }

        viewModelScope.launch {
            try {
                val cardIds = mutableListOf<Int>()
                currentDeckCards.forEach { (cardId, count) ->
                    repeat(count) { cardIds.add(cardId) }
                }

                val deckEntity = DeckEntity(
                    id = currentDeckId ?: 0, // 0 for new deck, will be auto-generated
                    name = name,
                    description = "Custom deck with ${currentDeckCards.size} unique cards",
                    heroClass = "neutral",
                    cardIds = JSONArray(cardIds).toString(),
                    isCustom = true,
                    createdAt = System.currentTimeMillis()
                )

                deckRepository.insertDeck(deckEntity)
                _message.value = "Deck '$name' saved successfully!"
                _deckSaved.value = true

            } catch (e: Exception) {
                _message.value = "Failed to save deck: ${e.message}"
            }
        }
    }

    fun loadExistingDeck(deckId: Int) {
        currentDeckId = deckId
        viewModelScope.launch {
            try {
                val deck = deckRepository.getDeckById(deckId)
                _deckName.value = deck.name

                // FIX: The error was here. We no longer need to parse a string of IDs.
                // We now iterate over the `deck.cards` list that the repository already prepared.
                currentDeckCards.clear()

                deck.cards.forEach { card ->
                    // FIX: Get the card's ID directly from the card object.
                    val cardId = card.id
                    currentDeckCards[cardId] = (currentDeckCards[cardId] ?: 0) + 1
                }

                updateDeckComposition()
                _message.value = "Loaded deck '${deck.name}'"

            } catch (e: Exception) {
                _message.value = "Failed to load deck: ${e.message}"
            }
        }
    }
}
