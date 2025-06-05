package com.rench.kvartstone.data.repositories

import android.content.Context
import com.rench.kvartstone.domain.GameEngine
import com.rench.kvartstone.domain.HeroPower
import kotlinx.coroutines.flow.Flow

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

    private fun createHeroPowerEffect(type: String, value: Int, targetType: String): (GameEngine, Any?) -> Unit {
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

    suspend fun initializeDefaultHeroPowers() {
        val defaultPowers = listOf(
            HeroPowerEntity(1, "Fireblast", "Deal 1 damage to any character", 2, "hero_power_fire", "damage", 1, "any_character"),
            HeroPowerEntity(2, "Armor Up!", "Gain 2 Armor", 2, "ic_hero_power_warrior", "armor", 2, "self"), // Keep if you have this

        )
        defaultPowers.forEach { heroPowerDao.insertPower(it) }
    }

}