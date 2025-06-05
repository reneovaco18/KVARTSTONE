package com.rench.kvartstone.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rench.kvartstone.data.entities.HeroPowerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HeroPowerDao {
    @Query("SELECT * FROM hero_powers WHERE isActive = 1")
    fun getAllActivePowers(): Flow<List<HeroPowerEntity>>

    @Query("SELECT * FROM hero_powers WHERE id = :id")
    suspend fun getHeroPowerById(id: Int): HeroPowerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPower(heroPower: HeroPowerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPowers(heroPowers: List<HeroPowerEntity>)
}
