package com.rench.kvartstone.data.repositories

import android.content.Context
import android.util.Log
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
        entities.mapNotNull { entity ->
            try {
                entityToDomain(entity)
            } catch (e: Exception) {
                Log.e("HeroPowerRepository", "Error converting hero power entity: ${e.message}")
                null
            }
        }
    }
    // Add this function to expose the count from the DAO
    suspend fun getActivePowerCount(): Int {
        return heroPowerDao.getActivePowerCount()
    }

    val allPowers: Flow<List<HeroPower>> = heroPowerDao.getAllPowers().map { entities ->
        entities.mapNotNull { entity ->
            try {
                entityToDomain(entity)
            } catch (e: Exception) {
                Log.e("HeroPowerRepository", "Error converting hero power: ${e.message}")
                null
            }
        }
    }

    fun getPowersByEffectType(effectType: String): Flow<List<HeroPower>> =
        heroPowerDao.getPowersByEffectType(effectType).map { entities ->
            entities.mapNotNull { entity ->
                try {
                    entityToDomain(entity)
                } catch (e: Exception) {
                    Log.e("HeroPowerRepository", "Error converting power by effect type: ${e.message}")
                    null
                }
            }
        }

    fun getPowersByCost(cost: Int): Flow<List<HeroPower>> =
        heroPowerDao.getPowersByCost(cost).map { entities ->
            entities.mapNotNull { entity ->
                try {
                    entityToDomain(entity)
                } catch (e: Exception) {
                    Log.e("HeroPowerRepository", "Error converting power by cost: ${e.message}")
                    null
                }
            }
        }

    private fun entityToDomain(entity: HeroPowerEntity): HeroPower {
        return HeroPower(
            id          = entity.id,
            name        = entity.name,
            description = entity.description,
            cost        = entity.manaCost,

            // ① use the **string** that is already stored in the DB
            imageResName = entity.imageResName,               // <- changed

            effect      = createHeroPowerEffect(
                entity.effectType,
                entity.effectValue,
                entity.targetType
            )
        )
    }


    private fun createHeroPowerEffect(type: String, value: Int, targetType: String): (GameEngineInterface, Any?) -> Unit {
        return when (type.lowercase()) {
            "damage" -> { engine, target ->
                when (targetType.lowercase()) {
                    "enemy_hero" -> {
                        try {
                            if (engine.currentTurn == Turn.PLAYER) {
                                engine.botHero.takeDamage(value)
                            } else {
                                engine.playerHero.takeDamage(value)
                            }
                        } catch (e: Exception) {
                            Log.e("HeroPowerRepository", "Error dealing damage to enemy hero: ${e.message}")
                        }
                    }
                    "any_character" -> {
                        try {
                            when (target) {
                                is MinionCard -> target.takeDamage(value)
                                is Hero -> target.takeDamage(value)
                                else -> {
                                    // Default to enemy hero if no target specified
                                    if (engine.currentTurn == Turn.PLAYER) {
                                        engine.botHero.takeDamage(value)
                                    } else {
                                        engine.playerHero.takeDamage(value)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("HeroPowerRepository", "Error dealing damage to character: ${e.message}")
                        }
                    }
                }
            }
            "heal" -> { engine, target ->
                try {
                    when (target) {
                        is MinionCard -> target.heal(value)
                        is Hero -> target.heal(value)
                        null -> {
                            // Heal own hero
                            if (engine.currentTurn == Turn.PLAYER) {
                                engine.playerHero.heal(value)
                            } else {
                                engine.botHero.heal(value)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HeroPowerRepository", "Error healing: ${e.message}")
                }
            }
            "armor" -> { engine, _ ->
                try {
                    if (engine.currentTurn == Turn.PLAYER) {
                        engine.playerHero.addArmor(value)
                    } else {
                        engine.botHero.addArmor(value)
                    }
                } catch (e: Exception) {
                    Log.e("HeroPowerRepository", "Error adding armor: ${e.message}")
                }
            }
            "draw" -> { engine, _ ->
                try {
                    repeat(value) {
                        if (engine.currentTurn == Turn.PLAYER) {
                            engine.drawCardForPlayer()
                        } else {
                            engine.drawCardForBot()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("HeroPowerRepository", "Error drawing cards: ${e.message}")
                }
            }
            else -> { _, _ ->
                Log.w("HeroPowerRepository", "Unknown hero power effect type: $type")
            }
        }
    }

    suspend fun getHeroPowerById(heroPowerId: Int): HeroPower {
        return try {
            val entity = heroPowerDao.getHeroPowerById(heroPowerId)
            if (entity != null) {
                entityToDomain(entity)
            } else {
                Log.w("HeroPowerRepository", "Hero power not found, returning default")
                createDefaultPlayerHeroPower()
            }
        } catch (e: Exception) {
            Log.e("HeroPowerRepository", "Error getting hero power by ID: ${e.message}")
            createDefaultPlayerHeroPower()
        }
    }

    suspend fun insertHeroPower(heroPowerEntity: HeroPowerEntity): Long {
        return try {
            heroPowerDao.insertPower(heroPowerEntity)
        } catch (e: Exception) {
            Log.e("HeroPowerRepository", "Error inserting hero power: ${e.message}")
            -1L
        }
    }

    suspend fun updateHeroPower(heroPowerEntity: HeroPowerEntity): Boolean {
        return try {
            heroPowerDao.updatePower(heroPowerEntity)
            true
        } catch (e: Exception) {
            Log.e("HeroPowerRepository", "Error updating hero power: ${e.message}")
            false
        }
    }

    suspend fun deleteHeroPower(heroPowerEntity: HeroPowerEntity): Boolean {
        return try {
            heroPowerDao.deletePower(heroPowerEntity)
            true
        } catch (e: Exception) {
            Log.e("HeroPowerRepository", "Error deleting hero power: ${e.message}")
            false
        }
    }

    suspend fun updatePowerActiveStatus(id: Int, isActive: Boolean): Boolean {
        return try {
            heroPowerDao.updatePowerActiveStatus(id, isActive)
            true
        } catch (e: Exception) {
            Log.e("HeroPowerRepository", "Error updating power active status: ${e.message}")
            false
        }
    }

    private fun createDefaultPlayerHeroPower(): HeroPower = HeroPower(
        id          = 1,
        name        = "Fireblast",
        description = "Deal 1 damage to any character",
        cost        = 2,

        // ② pass a drawable **name**, not an int
        imageResName = "ic_menu_close_clear_cancel",          // <- changed

        effect       = { engine, target ->
            when (target) {
                is MinionCard -> target.takeDamage(1)
                is Hero       -> target.takeDamage(1)
                else -> if (engine.currentTurn == Turn.PLAYER)
                    engine.botHero.takeDamage(1)
                else engine.playerHero.takeDamage(1)
            }
        }
    )

    suspend fun initializeDefaultHeroPowers() {
        try {
            if (heroPowerDao.getHeroPowerById(1) != null) {
                Log.d("HeroPowerRepository", "Default hero powers already exist, skipping initialization")
                return
            }

            val defaultPowers = listOf(
                HeroPowerEntity(
                    id = 1,
                    name = "Fireblast",
                    description = "Deal 1 damage to any character",
                    manaCost = 2,
                    imageResName = "hero_power_fire",
                    effectType = "damage",
                    effectValue = 1,
                    targetType = "any_character",
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                ),
                HeroPowerEntity(
                    id = 2,
                    name = "Armor Up!",
                    description = "Gain 2 Armor",
                    manaCost = 2,
                    imageResName = "ic_hero_power_warrior",
                    effectType = "armor",
                    effectValue = 2,
                    targetType = "self",
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                ),
                HeroPowerEntity(
                    id = 3,
                    name = "Lesser Heal",
                    description = "Restore 2 Health to any character",
                    manaCost = 2,
                    imageResName = "ic_hero_power_priest",
                    effectType = "heal",
                    effectValue = 2,
                    targetType = "any_character",
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
            )

            defaultPowers.forEach { heroPowerDao.insertPower(it) }
            Log.d("HeroPowerRepository", "Successfully initialized ${defaultPowers.size} default hero powers")
        } catch (e: Exception) {
            Log.e("HeroPowerRepository", "Error initializing default hero powers: ${e.message}")
        }
    }
}
