package com.rench.kvartstone.ui.cardmanagement

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rench.kvartstone.data.entities.CardEntity
import com.rench.kvartstone.data.repositories.CardRepository
import kotlinx.coroutines.launch

class CardManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val cardRepository = CardRepository(application)

    private val _cards = MutableLiveData<List<CardEntity>>()
    val cards: LiveData<List<CardEntity>> = _cards

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

    // Keep original list for filtering
    private var originalCards = listOf<CardEntity>()
    private var currentFilter = FilterType.ALL

    enum class FilterType {
        ALL, MINIONS, SPELLS, CUSTOM, RARITY
    }

    init {
        loadAllCards()
    }

    private fun loadAllCards() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                cardRepository.allCards.collect { domainCards ->
                    val entities = domainCards.map { card ->
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
                    originalCards = entities
                    _cards.value = entities
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _message.value = "Failed to load cards: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun searchCards(query: String) {
        if (query.isEmpty()) {
            showAllCards()
        } else {
            _cards.value = originalCards.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
        }
    }

    fun showAllCards() {
        currentFilter = FilterType.ALL
        _cards.value = originalCards
    }

    fun filterByType(type: String) {
        currentFilter = when (type) {
            "minion" -> FilterType.MINIONS
            "spell" -> FilterType.SPELLS
            else -> FilterType.ALL
        }
        _cards.value = originalCards.filter { it.type == type }
    }

    fun filterByCustomStatus(isCustom: Boolean) {
        currentFilter = FilterType.CUSTOM
        _cards.value = originalCards.filter { it.isCustom == isCustom }
    }

    fun filterByRarity(rarity: String) {
        currentFilter = FilterType.RARITY
        _cards.value = originalCards.filter { it.rarity == rarity }
    }

    fun createCard(card: CardEntity) {
        viewModelScope.launch {
            try {
                // Actually save to database
                val newCard = card.copy(
                    id = (originalCards.maxOfOrNull { it.id } ?: 0) + 1,
                    isCustom = true,
                    createdAt = System.currentTimeMillis()
                )

                // Add to original list
                originalCards = originalCards + newCard

                // Update displayed list
                refreshCurrentView()

                _message.value = "Card '${newCard.name}' created successfully"
            } catch (e: Exception) {
                _message.value = "Failed to create card: ${e.message}"
            }
        }
    }

    fun updateCard(card: CardEntity) {
        viewModelScope.launch {
            try {
                // Update in original list
                originalCards = originalCards.map {
                    if (it.id == card.id) card else it
                }

                // Update displayed list
                refreshCurrentView()

                _message.value = "Card '${card.name}' updated successfully"
            } catch (e: Exception) {
                _message.value = "Failed to update card: ${e.message}"
            }
        }
    }

    fun deleteCard(card: CardEntity) {
        viewModelScope.launch {
            try {
                // Remove from original list
                originalCards = originalCards.filter { it.id != card.id }

                // Update displayed list
                refreshCurrentView()

                _message.value = "Card '${card.name}' deleted successfully"
            } catch (e: Exception) {
                _message.value = "Failed to delete card: ${e.message}"
            }
        }
    }

    private fun refreshCurrentView() {
        // Reapply current filter
        when (currentFilter) {
            FilterType.ALL -> showAllCards()
            FilterType.MINIONS -> filterByType("minion")
            FilterType.SPELLS -> filterByType("spell")
            FilterType.CUSTOM -> filterByCustomStatus(true)
            FilterType.RARITY -> _cards.value = originalCards // Keep current rarity filter
        }
    }

    fun setSelectedImageUri(uri: Uri) {
        _selectedImageUri.value = uri
    }
}
