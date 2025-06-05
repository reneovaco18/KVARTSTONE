// DAO for Hero Powers
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
    @Query("SELECT * FROM hero_powers WHERE isActive = 1")
    fun getAllActivePowers(): Flow<List<HeroPowerEntity>>

    @Query("SELECT * FROM hero_powers WHERE id = :powerId")
    suspend fun getPowerById(powerId: Int): HeroPowerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPower(power: HeroPowerEntity)

    @Update
    suspend fun updatePower(power: HeroPowerEntity)

    @Delete
    suspend fun deletePower(power: HeroPowerEntity)
}