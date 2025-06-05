package com.rench.kvartstone.data.repositories

import android.content.Context
import com.rench.kvartstone.R
import com.rench.kvartstone.data.AppDatabase
import com.rench.kvartstone.data.entities.CardEntity
import com.rench.kvartstone.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CardRepository(private val context: Context) {
    private val cardDao = AppDatabase.getDatabase(context).cardDao()

    val allCards: Flow<List<Card>> = cardDao.getAllCards().map { entities ->
        entities.map { entity -> entityToCard(entity) }
    }

    suspend fun getCardById(cardId: Int): Card? {
        val entity = cardDao.getCardById(cardId) ?: return null
        return entityToCard(entity)
    }

    private fun entityToCard(entity: CardEntity): Card {
        val resourceId = context.resources.getIdentifier(
            entity.imageResName, "drawable", context.packageName
        )

        return when (entity.type) {
            "minion" -> MinionCard(
                id = entity.id,
                name = entity.name,
                manaCost = entity.manaCost,
                imageRes = if (resourceId != 0) resourceId else R.drawable.ic_card_minion_generic,
                attack = entity.attack ?: 0,
                maxHealth = entity.health ?: 1
            )
            "spell" -> SpellCard(
                id = entity.id,
                name = entity.name,
                manaCost = entity.manaCost,
                imageRes = if (resourceId != 0) resourceId else R.drawable.ic_card_spell_generic,
                effect = { gameEngine, targets ->
                    // Updated to work with GameEngineInterface
                    if (targets.isNotEmpty() && targets[0] is MinionCard) {
                        (targets[0] as MinionCard).takeDamage(1)
                    }
                },
                targetingType = TargetingType.SINGLE_CHARACTER,
                description = entity.description
            )
            else -> throw IllegalArgumentException("Unknown card type: ${entity.type}")
        }
    }

    suspend fun initializeDefaultCards() {
        val defaultCards = listOf(
            CardEntity(
                id = 1,
                name = "Vanilla Minion",
                description = "A basic minion with no special abilities",
                type = "minion",
                manaCost = 1,
                attack = 1,
                health = 2,
                effect = null,
                imageResName = "ic_card_vanilla"
            ),
            CardEntity(
                id = 2,
                name = "Strong Attacker",
                description = "High attack, low health minion",
                type = "minion",
                manaCost = 3,
                attack = 4,
                health = 2,
                effect = null,
                imageResName = "ic_card_attacker"
            ),
            CardEntity(
                id = 3,
                name = "Tough Defender",
                description = "Low attack, high health minion",
                type = "minion",
                manaCost = 3,
                attack = 2,
                health = 5,
                effect = null,
                imageResName = "ic_card_defender"
            ),
            CardEntity(
                id = 4,
                name = "Deal Damage",
                description = "Deal 2 damage to any target",
                type = "spell",
                manaCost = 2,
                attack = null,
                health = null,
                effect = "Deal 2 damage",
                imageResName = "ic_card_spell_damage"
            )
        )
        cardDao.insertCards(defaultCards)
    }
}
