package com.rench.kvartstone.data.repositories

import android.content.Context
import com.rench.kvartstone.R
import com.rench.kvartstone.data.AppDatabase
import com.rench.kvartstone.data.entities.CardEntity
import com.rench.kvartstone.domain.Card
import com.rench.kvartstone.domain.MinionCard
import com.rench.kvartstone.domain.SpellCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CardRepository(private val context: Context) {
    private val cardDao = AppDatabase.getDatabase(context).cardDao()

    val allCards: Flow<List<Card>> = cardDao.getAllCards().map { entities ->
        entities.map { entity -> entityToCard(entity) }
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
                imageRes = resourceId,
                attack = entity.attack ?: 0,
                health = entity.health ?: 1
            )
            "spell" -> SpellCard(
                id = entity.id,
                name = entity.name,
                manaCost = entity.manaCost,
                imageRes = resourceId,
                effect = { gameEngine, targets ->
                    // Parse effect from string and apply
                    // This is a simplified version
                    if (targets.isNotEmpty() && targets[0] is MinionCard) {
                        (targets[0] as MinionCard).takeDamage(1)
                    }
                }
            )
            else -> throw IllegalArgumentException("Unknown card type: ${entity.type}")
        }
    }

    suspend fun initializeDefaultCards() {
        val defaultCards = listOf(
            CardEntity(1, "Vanilla Minion", "minion", 1, 1, 2, null, "ic_card_vanilla"),
            CardEntity(2, "Strong Attacker", "minion", 3, 4, 2, null, "ic_card_attacker"),
            CardEntity(3, "Tough Defender", "minion", 3, 2, 5, null, "ic_card_defender"),
            CardEntity(4, "Deal Damage", "spell", 2, null, null, "Deal 2 damage", "ic_card_spell_damage")
        )
        cardDao.insertCards(defaultCards)
    }
}