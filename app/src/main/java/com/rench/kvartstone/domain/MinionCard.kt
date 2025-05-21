// MinionCard.kt
package com.rench.kvartstone.domain

class MinionCard(
    id: Int,
    name: String,
    manaCost: Int,
    imageRes: Int,
    val attack: Int,
    val health: Int,
    val baseHealth: Int = health
) : Card(id, name, manaCost, imageRes) {

    var currentHealth = health
    var canAttackThisTurn = false
    var hasAttackedThisTurn = false

    fun takeDamage(amount: Int) {
        currentHealth -= amount
    }

    fun heal(amount: Int) {
        currentHealth = minOf(baseHealth, currentHealth + amount)
    }

    fun resetForTurn() {
        canAttackThisTurn = true
        hasAttackedThisTurn = false
    }

    val isDead: Boolean
        get() = currentHealth <= 0
}