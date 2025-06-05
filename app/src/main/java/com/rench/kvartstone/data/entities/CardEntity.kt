// CardEntity.kt
package com.rench.kvartstone.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// Enhanced Card Entity with image support
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val type: String, // "minion", "spell", "weapon"
    val rarity: String, // "common", "rare", "epic", "legendary"
    val manaCost: Int,
    val attack: Int?, // null for spells
    val health: Int?, // null for spells
    val effect: String?, // JSON string for complex effects
    val imageResName: String, // resource name or file path
    val imageUri: String?, // for user-added images
    val keywords: String?, // JSON array: ["taunt", "divine_shield", "battlecry"]
    val heroClass: String = "neutral",
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)