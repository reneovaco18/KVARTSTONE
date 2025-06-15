package com.rench.kvartstone.domain

/**
 * Encodes a spell effect as data so designers can add new ones without code.
 * `type` : "damage", "heal" …
 * `value`: integer payload (e.g. 3 => deal 3 dmg / heal 3)
 */
data class SpellEffect(
    val type: String,
    val value: Int
) {
    override fun toString() = "$type:$value"          // stored in DB
    companion object {
        fun fromString(raw: String?): SpellEffect? =
            raw?.split(':')?.takeIf { it.size == 2 }?.let {
                SpellEffect(it[0], it[1].toIntOrNull() ?: 0)
            }
    }
}
