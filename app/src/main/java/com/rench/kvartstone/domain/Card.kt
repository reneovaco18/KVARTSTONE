// Card.kt
package com.rench.kvartstone.domain
sealed class Card(
    open val id: Int, // Add 'open'
    open val name: String,
    open val manaCost: Int,
    open val imageRes: Int
) {
    override fun toString(): String = name
}
