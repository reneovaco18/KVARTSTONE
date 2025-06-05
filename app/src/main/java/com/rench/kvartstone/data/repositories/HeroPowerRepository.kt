package com.rench.kvartstone.data.repositories

import android.content.Context
import com.rench.kvartstone.data.AppDatabase
import com.rench.kvartstone.data.entities.HeroPowerEntity
import com.rench.kvartstone.domain.GameEngineInterface
import com.rench.kvartstone.domain.Hero
import com.rench.kvartstone.domain.HeroPower
import com.rench.kvartstone.domain.MinionCard
import com.rench.kvartstone.domain.Turn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HeroPowerRepository(private val context: Context) {
    private val heroPowerDao = AppDatabase.getDatabase(context).heroPowerDao()

    val allHeroPowers: Flow<List<HeroPower>> = heroPowerDao.getAllActivePowers().map { entities ->
        entities.map { entityToDomain(it) }
    }

    private fun entityToDomain(entity: HeroPowerEntity): HeroPower {
        val resourceId = context.resources.getIdentifier(
            entity.imageResName, "drawable", context.packageName
        )

        return HeroPower(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            cost = entity.manaCost,
            imageRes = resourceId,
            effect = createHeroPowerEffect(entity.effectType, entity.effectValue, entity.targetType)
        )
    }

    private fun createHeroPowerEffect(type: String, value: Int, targetType: String): (GameEngineInterface, Any?) -> Unit {
        return when (type) {
            "damage" -> { engine, target ->
                when (targetType) {
                    "enemy_hero" -> {
                        if (engine.currentTurn == Turn.PLAYER) {
                            engine.botHero.takeDamage(value)
                        } else {
                            engine.playerHero.takeDamage(value)
                        }
                    }
                    "any_character" -> {
                        when (target) {
                            is MinionCard -> target.takeDamage(value)
                            is Hero -> target.takeDamage(value)
                        }
                    }
                }
            }
            "heal" -> { engine, target ->
                when (target) {
                    is MinionCard -> target.heal(value)
                    is Hero -> target.heal(value)
                    null -> {
                        if (engine.currentTurn == Turn.PLAYER) {
                            engine.playerHero.heal(value)
                        } else {
                            engine.botHero.heal(value)
                        }
                    }
                }
            }
            "armor" -> { engine, _ ->
                if (engine.currentTurn == Turn.PLAYER) {
                    engine.playerHero.addArmor(value)
                } else {
                    engine.botHero.addArmor(value)
                }
            }
            "draw" -> { engine, _ ->
                repeat(value) {
                    if (engine.currentTurn == Turn.PLAYER) {
                        engine.drawCardForPlayer()
                    } else {
                        engine.drawCardForBot()
                    }
                }
            }
            else -> { _, _ -> } // No effect
        }
    }

    suspend fun getHeroPowerById(heroPowerId: Int): HeroPower {
        val entity = heroPowerDao.getHeroPowerById(heroPowerId)
        return if (entity != null) {
            entityToDomain(entity)
        } else {
            // Return a default hero power if not found
            createDefaultPlayerHeroPower()
        }
    }

    private fun createDefaultPlayerHeroPower(): HeroPower {
        return HeroPower(
            id = 1,
            name = "Fireblast",
            description = "Deal 1 damage to any character",
            cost = 2,
            imageRes = android.R.drawable.ic_menu_close_clear_cancel, // Fallback icon
            effect = { engine, target ->
                when (target) {
                    is MinionCard -> target.takeDamage(1)
                    is Hero -> target.takeDamage(1)
                    else -> {
                        // Default: damage enemy hero
                        if (engine.currentTurn == Turn.PLAYER) {
                            engine.botHero.takeDamage(1)
                        } else {
                            engine.playerHero.takeDamage(1)
                        }
                    }
                }
            }
        )
    }

    suspend fun initializeDefaultHeroPowers() {
        val defaultPowers = listOf(
            HeroPowerEntity(1, "Fireblast", "Deal 1 damage to any character", 2, "hero_power_fire", "damage", 1, "any_character", true),
            HeroPowerEntity(2, "Armor Up!", "Gain 2 Armor", 2, "ic_hero_power_warrior", "armor", 2, "self", true)
        )
        defaultPowers.forEach { heroPowerDao.insertPower(it) }
    }
}
