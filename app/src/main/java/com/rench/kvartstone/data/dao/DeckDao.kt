package com.rench.kvartstone.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rench.kvartstone.data.entities.DeckEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE id = :deckId")
    suspend fun getDeckById(deckId: Int): DeckEntity?

    @Query("SELECT * FROM decks WHERE isCustom = :isCustom ORDER BY createdAt DESC")
    fun getDecksByCustomStatus(isCustom: Boolean): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE heroClass = :heroClass ORDER BY createdAt DESC")
    fun getDecksByHeroClass(heroClass: String): Flow<List<DeckEntity>>

    @Query("SELECT * FROM decks WHERE name LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%'")
    fun searchDecks(searchQuery: String): Flow<List<DeckEntity>>
    @Query("SELECT * FROM decks")
    suspend fun getAllDecksOnce(): List<DeckEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecks(decks: List<DeckEntity>)

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Delete
    suspend fun deleteDeck(deck: DeckEntity)

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: Int)

    @Query("SELECT COUNT(*) FROM decks")
    suspend fun getDeckCount(): Int

    @Query("SELECT COUNT(*) FROM decks WHERE isCustom = 1")
    suspend fun getCustomDeckCount(): Int
}
