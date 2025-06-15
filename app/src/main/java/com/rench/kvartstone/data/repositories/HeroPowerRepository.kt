package com.rench.kvartstone.data.repositories

import android.content.Context
import android.util.Log
import com.rench.kvartstone.data.AppDatabase
import com.rench.kvartstone.data.entities.HeroPowerEntity
import com.rench.kvartstone.domain.GameEngineInterface
import com.rench.kvartstone.domain.Hero
import com.rench.kvartstone.domain.HeroPower
import com.rench.kvartstone.domain.HeroPowerFactory
import com.rench.kvartstone.domain.MinionCard
import com.rench.kvartstone.domain.Turn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

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


            imageResName = entity.imageResName,

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
        description = "Deal 1 damage to a character",
        cost        = 2,
        imageResName = "ic_hero_power_fire",
        effect      = { engine, target ->
            val real = when (target) {
                is List<*> -> target.firstOrNull()
                null       -> if (engine.currentTurn == Turn.PLAYER)
                    engine.botHero else engine.playerHero
                else       -> target
            }
            when (real) {
                is MinionCard -> real.takeDamage(1, engine)
                is Hero       -> real.takeDamage(1)
            }
        }
    )
    private fun createDefaultBotHeroPower(): HeroPower = HeroPower(
        id          = 2,
        name        = "Lesser Heal",
        description = "Restore 2 Health to your hero",
        cost        = 2,
        imageResName = "ic_hero_power_priest",
        effect      = { engine, _ ->
            if (engine.currentTurn == Turn.PLAYER)
                engine.botHero.heal(2)
            else
                engine.playerHero.heal(2)
        }
    )
    suspend fun refreshHeroPowers() {
        try {

            val existingPowers = heroPowerDao.getAllPowers().first()
            existingPowers.forEach { power ->
                heroPowerDao.deletePowerById(power.id)
            }


            val allHeroPowers = HeroPowerFactory.getAllHeroPowers()
            val entities = allHeroPowers.map { power ->
                HeroPowerEntity(
                    id = power.id,
                    name = power.name,
                    description = power.description,
                    manaCost = power.cost,
                    imageResName = power.imageResName,
                    effectType = determineEffectType(power.id),
                    effectValue = determineEffectValue(power.id),
                    targetType = determineTargetType(power.id),
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )
            }

            heroPowerDao.insertPowers(entities)
            Log.d("HeroPowerRepository", "Successfully refreshed ${entities.size} hero powers")

        } catch (e: Exception) {
            Log.e("HeroPowerRepository", "Error refreshing hero powers: ${e.message}")
        }
    }


    private fun determineEffectType(id: Int) = when (id) {
        1 -> "damage"
        2 -> "heal"
        3 -> "armor"
        else -> "unknown"
    }

    private fun determineEffectValue(id: Int) = when (id) {
        1 -> 1
        2 -> 2
        3 -> 2
        else -> 0
    }

    private fun determineTargetType(id: Int) = when (id) {
        1 -> "any_character"
        2 -> "self"
        3 -> "self"
        else -> "none"
    }


    suspend fun initializeDefaultHeroPowers() {
        try {
            val count = heroPowerDao.getActivePowerCount()
            if (count == 0) {
                refreshHeroPowers()
            }
        } catch (e: Exception) {
            Log.e("HeroPowerRepository", "Error checking/initializing hero powers: ${e.message}")
        }
    }


}
