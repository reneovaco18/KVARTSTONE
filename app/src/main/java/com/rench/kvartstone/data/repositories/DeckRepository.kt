package com.rench.kvartstone.data.repositories

import android.content.Context
import com.rench.kvartstone.data.AppDatabase
import com.rench.kvartstone.data.entities.DeckEntity
import com.rench.kvartstone.domain.Card
import com.rench.kvartstone.domain.Deck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

class DeckRepository(private val context: Context) {
    private val deckDao = AppDatabase.getDatabase(context).deckDao()
    private val cardRepository = CardRepository(context)

    val allDecks: Flow<List<Deck>> = deckDao.getAllDecks().map { entities ->
        entities.map { entityToDomain(it) }
    }

    private suspend fun entityToDomain(entity: DeckEntity): Deck {
        val cardIds = parseCardIds(entity.cardIds)

        val cards = cardIds.mapNotNull { cardId ->
            cardRepository.getCardById(cardId)
        }

        return Deck(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            cards = cards,
            heroClass = entity.heroClass
        )
    }

    private fun parseCardIds(cardIdsJson: String): List<Int> {
        return try {
            val jsonArray = JSONArray(cardIdsJson)
            (0 until jsonArray.length()).map { jsonArray.getInt(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun cardIdsToJson(cardIds: List<Int>): String {
        val jsonArray = JSONArray()
        cardIds.forEach { jsonArray.put(it) }
        return jsonArray.toString()
    }

    suspend fun getDeckById(deckId: Int): Deck {
        val entity = deckDao.getDeckById(deckId)
        return entityToDomain(entity)
    }

    suspend fun initializeDefaultDecks() {
        val defaultCardIds = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
        val cardIdsJson = cardIdsToJson(defaultCardIds)

        val defaultDecks = listOf(
            DeckEntity(1, "Basic Deck", "A balanced starter deck", "neutral", cardIdsJson, false, System.currentTimeMillis()),
            DeckEntity(2, "Aggressive Deck", "Fast and aggressive", "warrior", cardIdsJson, false, System.currentTimeMillis())
        )

        defaultDecks.forEach { deckDao.insertDeck(it) }
    }
}
