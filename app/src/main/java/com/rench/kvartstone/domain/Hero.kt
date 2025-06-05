package com.rench.kvartstone.domain

data class Hero(
    val name: String,
    val maxHealth: Int,
    val imageRes: Int,
    val heroPower: HeroPower,
    val heroPowerImageRes: Int,
    var currentHealth: Int = maxHealth,
    var armor: Int = 0
) {
    fun takeDamage(amount: Int) {
        val actualDamage = if (armor >= amount) {
            armor -= amount
            0
        } else {
            val remainingDamage = amount - armor
            armor = 0
            remainingDamage
        }
        currentHealth -= actualDamage
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
