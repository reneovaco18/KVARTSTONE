// SpellCard.kt
package com.rench.kvartstone.domain

class SpellCard(
    id: Int,
    name: String,
    manaCost: Int,
    imageRes: Int,
    val effect: (GameEngine, List<Any>) -> Unit
) : Card(id, name, manaCost, imageRes)