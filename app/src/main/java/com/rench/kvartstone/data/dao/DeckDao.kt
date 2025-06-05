package com.rench.kvartstone.data.dao

import DeckEntity
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rench.kvartstone.data.entities.CardEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY isCustom ASC, createdAt DESC")
    fun getAllDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :deckId")
    suspend fun getDeckById(deckId: Int): DeckEntity?

    @Query("SELECT * FROM decks WHERE isCustom = 0")
    fun getPreBuiltDecks(): Flow<List<DeckEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity)

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)
}