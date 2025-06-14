package com.rench.kvartstone.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rench.kvartstone.data.entities.HeroPowerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HeroPowerDao {
    @Query("SELECT * FROM hero_powers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActivePowers(): Flow<List<HeroPowerEntity>>

    @Query("SELECT * FROM hero_powers ORDER BY name ASC")
    fun getAllPowers(): Flow<List<HeroPowerEntity>>

    @Query("SELECT * FROM hero_powers WHERE id = :id")
    suspend fun getHeroPowerById(id: Int): HeroPowerEntity?

    @Query("SELECT * FROM hero_powers WHERE effectType = :effectType AND isActive = 1")
    fun getPowersByEffectType(effectType: String): Flow<List<HeroPowerEntity>>

    @Query("SELECT * FROM hero_powers WHERE manaCost = :cost AND isActive = 1")
    fun getPowersByCost(cost: Int): Flow<List<HeroPowerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPower(heroPower: HeroPowerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPowers(heroPowers: List<HeroPowerEntity>)

    @Update
    suspend fun updatePower(heroPower: HeroPowerEntity)

    @Delete
    suspend fun deletePower(heroPower: HeroPowerEntity)

    @Query("DELETE FROM hero_powers WHERE id = :id")
    suspend fun deletePowerById(id: Int)

    @Query("UPDATE hero_powers SET isActive = :isActive WHERE id = :id")
    suspend fun updatePowerActiveStatus(id: Int, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM hero_powers WHERE isActive = 1")
    suspend fun getActivePowerCount(): Int
}
