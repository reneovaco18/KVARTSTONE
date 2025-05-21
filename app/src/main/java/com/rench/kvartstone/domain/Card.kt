// Card.kt
package com.rench.kvartstone.domain

sealed class Card(
    val id: Int,
    val name: String,
    val manaCost: Int,
    val imageRes: Int
) {
    override fun toString(): String = name
}
