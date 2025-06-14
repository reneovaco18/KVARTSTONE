package com.rench.kvartstone.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val heroClass: String,
    val cardIds: String, // JSON string of card IDs
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
