// CardDao.kt
package com.rench.kvartstone.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rench.kvartstone.data.entities.CardEntity
import kotlinx.coroutines.flow.Flow

// Enhanced Card DAO with search and filtering
@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY manaCost ASC, name ASC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCardById(cardId: Int): CardEntity?

    @Query("SELECT * FROM cards WHERE name LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%'")
    fun searchCards(searchQuery: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE type = :type")
    fun getCardsByType(type: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE manaCost = :cost")
    fun getCardsByCost(cost: Int): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE rarity = :rarity")
    fun getCardsByRarity(rarity: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE isCustom = :isCustom")
    fun getCardsByCustomStatus(isCustom: Boolean): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteCardById(cardId: Int)
}