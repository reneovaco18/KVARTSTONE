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
import kotlinx.coroutines.flow.firstOrNull
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
    // data/repositories/DeckRepository.kt
    // random deck helper
    suspend fun getRandomDeckExcept(excludeId: Int): Deck? =
        deckDao.getAllDecksOnce()
            .mapNotNull { entityToDomain(it) }   // ← was it.toDomain()
            .filter   { it.id != excludeId }
            .randomOrNull()

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
        return deckDao.insertDeck(deck).also { id ->
            if (id > 0) Log.d("DeckRepo", "Deck inserted with ID: $id")
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
            if (deckDao.getDeckCount() > 0) return

            // Get ALL card IDs from database
            val cardIds = cardRepository.getAllCardsAsEntity()
                .firstOrNull()
                ?.map { it.id }
                ?: emptyList()

            val defaultDecks = listOf(
                DeckEntity(
                    name = "Basic Deck",
                    description = "Balanced starter deck",
                    heroClass = "neutral",
                    cardIds = JSONArray(cardIds.take(30)).toString(),
                    isCustom = false
                ),
                DeckEntity(
                    name = "Aggressive Deck",
                    description = "Fast combat deck",
                    heroClass = "warrior",
                    cardIds = JSONArray(cardIds.filter { it % 2 == 0 }.take(30)).toString(),
                    isCustom = false
                )
            )

            deckDao.insertDecks(defaultDecks)
        } catch (e: Exception) {
            Log.e("DeckRepository", "Error initializing default decks", e)
        }
    }



    // In DeckRepository.kt

    suspend fun removeCardFromAllDecks(cardId: Int) {
        withContext(Dispatchers.IO) {
            val allDecks = deckDao.getAllDecks().first() // Get a snapshot
            for (deckEntity in allDecks) {
                val cardIds = JSONArray(deckEntity.cardIds)
                val updatedIds = JSONArray()
                var changed = false
                for (i in 0 until cardIds.length()) {
                    if (cardIds.getInt(i) != cardId) {
                        updatedIds.put(cardIds.getInt(i))
                    } else {
                        changed = true
                    }
                }
                if (changed) {
                    updateDeck(deckEntity.copy(cardIds = updatedIds.toString()))
                }
            }
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
        val targetSize = 10

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