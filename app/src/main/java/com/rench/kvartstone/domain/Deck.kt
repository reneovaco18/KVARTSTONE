package com.rench.kvartstone.domain

data class Deck(
    val id: Int,
    val name: String,
    val description: String,
    val cards: List<Card>,
    val heroClass: String = "neutral"
)
