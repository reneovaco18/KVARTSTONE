// Enhanced HeroPower Entity for database storage
package com.rench.kvartstone.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hero_powers")
data class HeroPowerEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val manaCost: Int,
    val imageResName: String,
    val effectType: String, // "damage", "heal", "draw", "armor", "summon"
    val effectValue: Int,
    val targetType: String, // "enemy_hero", "any_character", "self", "all_enemies", "random"
    val isActive: Boolean = true
)