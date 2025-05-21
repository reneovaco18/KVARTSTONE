// CardEntity.kt
package com.rench.kvartstone.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val type: String, // "minion" or "spell"
    val manaCost: Int,
    val attack: Int?, // null for spells
    val health: Int?, // null for spells
    val effect: String?, // description of effect for spells
    val imageResName: String // resource name (not ID)
)