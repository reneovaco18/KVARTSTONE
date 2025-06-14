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
                // Use the existing method that returns CardEntity directly
                cardRepository.getAllCardsAsEntity().collect { cardEntities ->
                    originalCards = cardEntities
                    refreshCurrentView()
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
                // Actually persist to database via repository
                val insertedId = cardRepository.insertCard(card.copy(
                    isCustom = true,
                    createdAt = System.currentTimeMillis()
                ))

                if (insertedId > 0) {
                    _message.value = "Card '${card.name}' created successfully"
                    // Data will automatically update through Flow observation
                } else {
                    _message.value = "Failed to create card"
                }
            } catch (e: Exception) {
                _message.value = "Failed to create card: ${e.message}"
            }
        }
    }



    fun updateCard(card: CardEntity) {
        viewModelScope.launch {
            try {
                val success = cardRepository.updateCard(card)
                if (success) {
                    _message.value = "Card '${card.name}' updated successfully"
                    loadAllCards()
                }
            } catch (e: Exception) {
                _message.value = "Failed to update card: ${e.message}"
            }
        }
    }


    fun deleteCard(card: CardEntity) {
        viewModelScope.launch {
            try {
                val success = cardRepository.deleteCard(card)
                if (success) {
                    _message.value = "Card '${card.name}' deleted successfully"
                    loadAllCards()
                }
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
