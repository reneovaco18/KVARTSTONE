package com.rench.kvartstone.data.repositories

import android.content.Context
import android.util.Log
import com.rench.kvartstone.R
import com.rench.kvartstone.data.AppDatabase
import com.rench.kvartstone.data.entities.CardEntity
import com.rench.kvartstone.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CardRepository(private val context: Context) {
    private val cardDao = AppDatabase.getDatabase(context).cardDao()

    val allCards: Flow<List<Card>> = cardDao.getAllCards().map { entities ->
        entities.mapNotNull { entity ->
            try {
                entityToCard(entity)
            } catch (e: Exception) {
                Log.e("CardRepository", "Error converting entity to card: ${e.message}")
                null
            }
        }
    }

    fun getAllCardsAsEntity(): Flow<List<CardEntity>> = cardDao.getAllCards()

    fun getCustomCards(): Flow<List<Card>> = cardDao.getCardsByCustomStatus(true).map { entities ->
        entities.mapNotNull { entity ->
            try {
                entityToCard(entity)
            } catch (e: Exception) {
                Log.e("CardRepository", "Error converting custom card: ${e.message}")
                null
            }
        }
    }
    suspend fun insert(card: CardEntity): Long = insertCard(card)

    suspend fun insertAll(cards: List<CardEntity>) {
        cardDao.insertCards(cards)
    }
    fun searchCards(query: String): Flow<List<Card>> = cardDao.searchCards(query).map { entities ->
        entities.mapNotNull { entity ->
            try {
                entityToCard(entity)
            } catch (e: Exception) {
                Log.e("CardRepository", "Error converting searched card: ${e.message}")
                null
            }
        }
    }

    suspend fun getCardById(cardId: Int): Card? {
        return try {
            val entity = cardDao.getCardById(cardId) ?: return null
            entityToCard(entity)
        } catch (e: Exception) {
            Log.e("CardRepository", "Error getting card by ID: ${e.message}")
            null
        }
    }


    suspend fun getCardsByIds(idList: List<Int>): List<Card> {
        if (idList.isEmpty()) return emptyList()


        val entitiesById: Map<Int, CardEntity> =
            cardDao.getCardsByIds(idList.distinct())
                .associateBy { it.id }


        val result = mutableListOf<Card>()
        idList.forEach { id ->
            entitiesById[id]?.let { result += entityToCard(it) }
        }
        return result
    }

    suspend fun insertCard(cardEntity: CardEntity): Long {
        return try {
            val result = cardDao.insertCard(cardEntity)
            Log.d("CardRepository", "Inserted card '${cardEntity.name}' with ID: $result")
            result
        } catch (e: Exception) {
            Log.e("CardRepository", "Error inserting card: ${e.message}")
            -1L
        }
    }

    suspend fun updateCard(cardEntity: CardEntity): Boolean {
        return try {
            cardDao.updateCard(cardEntity)
            Log.d("CardRepository", "Updated card '${cardEntity.name}'")
            true
        } catch (e: Exception) {
            Log.e("CardRepository", "Error updating card: ${e.message}")
            false
        }
    }

    suspend fun deleteCard(cardEntity: CardEntity): Boolean {
        return try {
            cardDao.deleteCard(cardEntity)
            Log.d("CardRepository", "Deleted card '${cardEntity.name}'")
            true
        } catch (e: Exception) {
            Log.e("CardRepository", "Error deleting card: ${e.message}")
            false
        }
    }

    suspend fun getCardCount(): Int {
        return try {
            cardDao.getCardCount()
        } catch (e: Exception) {
            Log.e("CardRepository", "Error getting card count: ${e.message}")
            0
        }
    }

    fun entityToCard(entity: CardEntity): Card {
        return when (entity.type.lowercase()) {
            "minion" -> MinionCard(
                id         = entity.id,
                name       = entity.name,
                manaCost   = entity.manaCost,
                imageResName = entity.imageResName.ifBlank { "ic_card_minion_generic" },
                imageUri   = entity.imageUri,
                attack     = entity.attack ?: 0,
                maxHealth  = entity.health ?: 1
            )
            "spell"  -> SpellCard(
                id         = entity.id,
                name       = entity.name,
                manaCost   = entity.manaCost,
                imageResName = entity.imageResName.ifBlank { "ic_card_spell_generic" },
                imageUri   = entity.imageUri,
                effect     = createSpellEffect(entity.effect),
                targetingType = determineTargetingType(entity.effect),
                description   = entity.description
            )
            else -> throw IllegalArgumentException("Unknown card type ${entity.type}")
        }
    }
    private fun createSpellEffect(raw: String?): (GameEngineInterface, List<Any>) -> Unit {


        SpellEffect.fromString(raw)?.let { spec ->
            return when (spec.type) {
                "damage" -> damageFn(spec.value)
                "heal"   -> healHeroFn(spec.value)
                else     -> { _, _ -> }
            }
        }


        raw?.lowercase()?.let { txt ->
            Regex("""deal\s+(\d+)\s+damage""").find(txt)
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?.let { return damageFn(it) }

            Regex("""restore\s+(\d+)\s+health""").find(txt)
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?.let { return healHeroFn(it) }
        }

        return { _, _ -> }
    }
    private fun damageFn(dmg: Int): (GameEngineInterface, List<Any>) -> Unit =
        { _, targets ->
            targets.forEach {
                when (it) {
                    is MinionCard -> it.takeDamage(dmg)
                    is Hero       -> it.takeDamage(dmg)
                }
            }
        }

    private fun healHeroFn(hp: Int): (GameEngineInterface, List<Any>) -> Unit =
        { engine, _ -> engine.playerHero.heal(hp) }
    private fun determineTargetingType(effectString: String?): TargetingType {
        return when (effectString?.lowercase()) {
            "deal 2 damage", "deal 1 damage" -> TargetingType.SINGLE_CHARACTER
            else -> TargetingType.NO_TARGET
        }
    }




    suspend fun initializeDefaultCards() {
        try {

            val existingCount = cardDao.getCardCount()
            if (existingCount > 0) {
                Log.d("CardRepository", "Cards already exist ($existingCount), skipping initialization")
                return
            }


            val defaultCards = listOf(
                CardEntity(
                    id = 0,
                    name = "Vanilla Minion",
                    description = "A basic minion with no special abilities",
                    type = "minion",
                    manaCost = 1,
                    attack = 1,
                    health = 2,
                    effect = null,
                    imageResName = "ic_card_vanilla",
                    isCustom = false
                ),
                CardEntity(
                    id = 0,
                    name = "Strong Attacker",
                    description = "High attack, low health minion",
                    type = "minion",
                    manaCost = 3,
                    attack = 4,
                    health = 2,
                    effect = null,
                    imageResName = "ic_card_attacker",
                    isCustom = false
                ),
                CardEntity(
                    id = 0,
                    name = "Tough Defender",
                    description = "Low attack, high health minion",
                    type = "minion",
                    manaCost = 3,
                    attack = 2,
                    health = 5,
                    effect = null,
                    imageResName = "ic_card_defender",
                    isCustom = false
                ),
                CardEntity(
                    id = 0,
                    name = "Lightning Bolt",
                    description = "Deal 2 damage to any target",
                    type = "spell",
                    manaCost = 2,
                    attack = null,
                    health = null,
                    effect = "Deal 2 damage",
                    imageResName = "ic_card_spell_damage",
                    isCustom = false
                ),
                CardEntity(
                    id = 0,
                    name = "Fireball",
                    description = "Deal 1 damage to any target",
                    type = "spell",
                    manaCost = 1,
                    attack = null,
                    health = null,
                    effect = "Deal 1 damage",
                    imageResName = "ic_card_spell_lightning",
                    isCustom = false
                )
            )


            val insertedIds = mutableListOf<Long>()
            for (card in defaultCards) {
                val insertedId = cardDao.insertCard(card)
                insertedIds.add(insertedId)
                Log.d("CardRepository", "Inserted card '${card.name}' with auto-generated ID: $insertedId")
            }

            Log.d("CardRepository", "Successfully initialized ${defaultCards.size} default cards with IDs: $insertedIds")


            val finalCount = cardDao.getCardCount()
            Log.d("CardRepository", "Final card count after initialization: $finalCount")

        } catch (e: Exception) {
            Log.e("CardRepository", "Error initializing default cards: ${e.message}", e)
            throw e
        }
    }
}