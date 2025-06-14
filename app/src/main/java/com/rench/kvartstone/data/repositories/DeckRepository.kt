package com.rench.kvartstone.data.repositories
import kotlinx.coroutines.flow.first
import android.content.Context
import android.util.Log
import com.rench.kvartstone.data.AppDatabase
import com.rench.kvartstone.data.entities.DeckEntity
import com.rench.kvartstone.domain.Card
import com.rench.kvartstone.domain.Deck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONException
import com.rench.kvartstone.data.entities.CardEntity
// FIXED: Now uses actual card IDs from database instead of hardcoded values
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
class DeckRepository(private val context: Context) {
    private val deckDao = AppDatabase.getDatabase(context).deckDao()
    private val cardRepository = CardRepository(context)

    val allDecks: Flow<List<Deck>> = deckDao.getAllDecks().map { entities ->
        entities.mapNotNull { entity ->
            try {
                entityToDomain(entity)
            } catch (e: Exception) {
                Log.e("DeckRepository", "Error converting deck entity: ${e.message}")
                null
            }
        }
    }

    val customDecks: Flow<List<Deck>> = deckDao.getDecksByCustomStatus(true).map { entities ->
        entities.mapNotNull { entity ->
            try {
                entityToDomain(entity)
            } catch (e: Exception) {
                Log.e("DeckRepository", "Error converting custom deck: ${e.message}")
                null
            }
        }
    }

    fun searchDecks(query: String): Flow<List<Deck>> = deckDao.searchDecks(query).map { entities ->
        entities.mapNotNull { entity ->
            try {
                entityToDomain(entity)
            } catch (e: Exception) {
                Log.e("DeckRepository", "Error converting searched deck: ${e.message}")
                null
            }
        }
    }

    fun getDecksByHeroClass(heroClass: String): Flow<List<Deck>> =
        deckDao.getDecksByHeroClass(heroClass).map { entities ->
            entities.mapNotNull { entity ->
                try {
                    entityToDomain(entity)
                } catch (e: Exception) {
                    Log.e("DeckRepository", "Error converting deck by hero class: ${e.message}")
                    null
                }
            }
        }

    private suspend fun entityToDomain(entity: DeckEntity): Deck? {
        return try {
            val cardIds = parseCardIds(entity.cardIds)
            val cards = cardRepository.getCardsByIds(cardIds)

            if (cards.size != cardIds.size) {
                Log.w("DeckRepository", "Some cards not found for deck ${entity.name}. Expected: ${cardIds.size}, Found: ${cards.size}")
            }

            Deck(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                cards = cards,
                heroClass = entity.heroClass
            )
        } catch (e: Exception) {
            Log.e("DeckRepository", "Error converting deck entity to domain: ${e.message}")
            null
        }
    }

    private fun parseCardIds(cardIdsJson: String): List<Int> {
        return try {
            val jsonArray = JSONArray(cardIdsJson)
            (0 until jsonArray.length()).map { jsonArray.getInt(it) }
        } catch (e: JSONException) {
            Log.e("DeckRepository", "Error parsing card IDs JSON: ${e.message}")
            emptyList()
        }
    }

    private fun cardIdsToJson(cardIds: List<Int>): String {
        return try {
            JSONArray(cardIds).toString()
        } catch (e: Exception) {
            Log.e("DeckRepository", "Error converting card IDs to JSON: ${e.message}")
            "[]"
        }
    }

    suspend fun insertDeck(deck: DeckEntity): Long {
        return try {
            val result = deckDao.insertDeck(deck)
            Log.d("DeckRepository", "Inserted deck '${deck.name}' with ID: $result")
            result
        } catch (e: Exception) {
            Log.e("DeckRepository", "Error inserting deck: ${e.message}")
            -1L
        }
    }

    suspend fun insertDeckFromDomain(deck: Deck): Long {
        return try {
            val cardIds = deck.cards.map { it.id }
            val deckEntity = DeckEntity(
                id = if (deck.id > 0) deck.id else 0, // Let database auto-generate if id is 0
                name = deck.name,
                description = deck.description,
                heroClass = deck.heroClass,
                cardIds = cardIdsToJson(cardIds),
                isCustom = true,
                createdAt = System.currentTimeMillis()
            )
            insertDeck(deckEntity)
        } catch (e: Exception) {
            Log.e("DeckRepository", "Error inserting deck from domain: ${e.message}")
            -1L
        }
    }

    suspend fun updateDeck(deck: DeckEntity): Boolean {
        return try {
            deckDao.updateDeck(deck)
            Log.d("DeckRepository", "Updated deck '${deck.name}'")
            true
        } catch (e: Exception) {
            Log.e("DeckRepository", "Error updating deck: ${e.message}")
            false
        }
    }

    suspend fun deleteDeck(deck: DeckEntity): Boolean {
        return try {
            deckDao.deleteDeck(deck)
            Log.d("DeckRepository", "Deleted deck '${deck.name}'")
            true
        } catch (e: Exception) {
            Log.e("DeckRepository", "Error deleting deck: ${e.message}")
            false
        }
    }

    suspend fun deleteDeckById(deckId: Int): Boolean {
        return try {
            deckDao.deleteDeckById(deckId)
            Log.d("DeckRepository", "Deleted deck with ID: $deckId")
            true
        } catch (e: Exception) {
            Log.e("DeckRepository", "Error deleting deck by ID: ${e.message}")
            false
        }
    }

    suspend fun getDeckById(deckId: Int): Deck? {
        return try {
            val entity = deckDao.getDeckById(deckId) ?: return null
            entityToDomain(entity)
        } catch (e: Exception) {
            Log.e("DeckRepository", "Error getting deck by ID: ${e.message}")
            null
        }
    }

    suspend fun getDeckCount(): Int {
        return try {
            deckDao.getDeckCount()
        } catch (e: Exception) {
            Log.e("DeckRepository", "Error getting deck count: ${e.message}")
            0
        }
    }

    suspend fun getCustomDeckCount(): Int {
        return try {
            deckDao.getCustomDeckCount()
        } catch (e: Exception) {
            Log.e("DeckRepository", "Error getting custom deck count: ${e.message}")
            0
        }
    }



    suspend fun initializeDefaultDecks() {
        try {
            if (deckDao.getDeckCount() > 0) {
                Log.d("DeckRepository", "Decks already exist, skipping initialization.")
                return
            }

            // CORRECTED: Collect the list from the Flow before using it
            val cardEntities: List<CardEntity> = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(context).cardDao().getAllCards().first()
            }

            // CORRECTED: This check now correctly operates on a List
            if (cardEntities.isNullOrEmpty()) {
                Log.w("DeckRepository", "No cards available for deck creation.")
                return
            }

            // CORRECTED: This now correctly maps over a List, not a Flow
            val availableCardIds = cardEntities.map { it.id }

            // These function calls now receive the correct List<Int> type
            val basicDeckCardIds = createBalancedDeck(availableCardIds, "basic")
            val aggressiveDeckCardIds = createBalancedDeck(availableCardIds, "aggressive")

            val defaultDecks = listOf(
                DeckEntity(
                    id = 0,
                    name = "Basic Deck",
                    description = "A balanced starter deck with various card types",
                    heroClass = "neutral",
                    cardIds = cardIdsToJson(basicDeckCardIds),
                    isCustom = false,
                    createdAt = System.currentTimeMillis()
                ),
                DeckEntity(
                    id = 0,
                    name = "Aggressive Deck",
                    description = "Fast and aggressive deck focused on quick victories",
                    heroClass = "warrior",
                    cardIds = cardIdsToJson(aggressiveDeckCardIds),
                    isCustom = false,
                    createdAt = System.currentTimeMillis()
                )
            )

            for (deck in defaultDecks) {
                insertDeck(deck)
            }

            Log.d("DeckRepository", "Default decks initialized.")
        } catch (e: Exception) {
            Log.e("DeckRepository", "Error during deck initialization: ${e.message}", e)
        }
    }




    /**
     * Create a balanced 30-card deck from available card IDs
     */
    private fun createBalancedDeck(availableCardIds: List<Int>, deckType: String): List<Int> {
        if (availableCardIds.isEmpty()) {
            Log.w("DeckRepository", "No cards available for deck creation")
            return emptyList()
        }

        val deckCards = mutableListOf<Int>()
        val targetSize = 30

        when (deckType) {
            "basic" -> {
                // Create a balanced deck with 2 copies of each card, cycling through available cards
                var cardIndex = 0
                repeat(targetSize) {
                    deckCards.add(availableCardIds[cardIndex % availableCardIds.size])
                    // Add each card twice before moving to next
                    if ((it + 1) % 2 == 0) {
                        cardIndex++
                    }
                }
            }
            "aggressive" -> {
                // Create an aggressive deck favoring lower cost cards (assuming lower IDs = lower cost)
                var cardIndex = 0
                repeat(targetSize) {
                    // Favor earlier cards more heavily for aggressive deck
                    val weightedIndex = if (cardIndex < availableCardIds.size / 2) {
                        cardIndex % (availableCardIds.size / 2)
                    } else {
                        cardIndex % availableCardIds.size
                    }
                    deckCards.add(availableCardIds[weightedIndex])
                    cardIndex++
                }
            }
            else -> {
                // Default: evenly distribute available cards
                repeat(targetSize) {
                    deckCards.add(availableCardIds[it % availableCardIds.size])
                }
            }
        }

        Log.d("DeckRepository", "Created $deckType deck with ${deckCards.size} cards using card IDs: ${availableCardIds.take(5)}...")
        return deckCards
    }
}