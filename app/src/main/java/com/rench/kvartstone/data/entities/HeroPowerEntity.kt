package com.rench.kvartstone.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hero_powers")
data class HeroPowerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val manaCost: Int,
    val imageResName: String,
    val effectType: String,
    val effectValue: Int,
    val targetType: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
