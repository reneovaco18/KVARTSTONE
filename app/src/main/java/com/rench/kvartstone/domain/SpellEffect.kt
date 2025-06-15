package com.rench.kvartstone.domain

data class SpellEffect(
    val type: String,
    val value: Int
) {
    override fun toString() = "$type:$value"
    companion object {
        fun fromString(raw: String?): SpellEffect? =
            raw?.split(':')?.takeIf { it.size == 2 }?.let {
                SpellEffect(it[0], it[1].toIntOrNull() ?: 0)
            }
    }
}
