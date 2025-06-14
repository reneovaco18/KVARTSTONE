package com.rench.kvartstone.domain

data class Hero(
    val name: String,
    val heroPower: HeroPower,
    var maxHealth: Int = 30,
    var currentHealth: Int = 30,
    var armor: Int = 0
) {
    fun takeDamage(amount: Int) {
        val damageToHealth = amount - armor
        armor = maxOf(0, armor - amount)
        if (damageToHealth > 0) {
            currentHealth -= damageToHealth
        }
    }

    fun heal(amount: Int) {
        currentHealth = minOf(currentHealth + amount, maxHealth)
    }

    fun isDead(): Boolean = currentHealth <= 0

    fun addArmor(amount: Int) {
        armor += amount
    }

    fun resetHeroPower() {
        heroPower.resetForNewTurn()
    }
}
