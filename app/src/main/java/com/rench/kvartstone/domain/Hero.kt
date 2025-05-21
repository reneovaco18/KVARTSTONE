// Hero.kt
package com.rench.kvartstone.domain

class Hero(
    val name: String,
    val maxHealth: Int = 20,
    val imageRes: Int,
    val heroPowerImageRes: Int
) {
    var currentHealth = maxHealth
    var heroPowerUsed = false

    fun takeDamage(amount: Int) {
        currentHealth -= amount
    }

    fun heal(amount: Int) {
        currentHealth = minOf(maxHealth, currentHealth + amount)
    }

    fun resetForTurn() {
        heroPowerUsed = false
    }

    val isDead: Boolean
        get() = currentHealth <= 0
}
