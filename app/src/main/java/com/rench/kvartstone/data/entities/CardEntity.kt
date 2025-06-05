package com.rench.kvartstone.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String = "", // Add this field
    val type: String,
    val manaCost: Int,
    val attack: Int?,
    val health: Int?,
    val effect: String?,
    val imageResName: String,
    val rarity: String = "common",
    val imageUri: String? = null,
    val keywords: String? = null,
    val heroClass: String = "neutral",
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
