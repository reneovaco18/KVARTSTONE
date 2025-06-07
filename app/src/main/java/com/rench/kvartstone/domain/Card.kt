package com.rench.kvartstone.domain

sealed class Card(
    open val id: Int,
    open val name: String,
    open val manaCost: Int,
    open val imageRes: Int,
    open val imageUri: String? = null
) {
    override fun toString(): String = name
}
