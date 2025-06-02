package com.rench.kvartstone.domain

data class MinionCard(
    override val id: Int,
    override val name: String,
    override val manaCost: Int,
    override val imageRes: Int,
    var attack: Int,
    var maxHealth: Int,
    var currentHealth: Int = maxHealth,
    var hasDivineShield: Boolean = false,
    var hasAttackedThisTurn: Boolean = false,
    var canAttackThisTurn: Boolean = false, // Summoning sickness
    var battlecryEffect: ((GameEngine, List<Any>) -> Unit)? = null,
    var deathrattleEffect: ((GameEngine) -> Unit)? = null,
    var summoned: Boolean = false // Track if just summoned this turn
) : Card(id, name, manaCost, imageRes) {

    val health: Int get() = currentHealth // Backward compatibility

    fun takeDamage(amount: Int) {
        if (hasDivineShield) {
            hasDivineShield = false
            return // Divine shield absorbs first damage
        }
        currentHealth -= amount
        if (currentHealth <= 0) {
            triggerDeathrattle()
        }
    }

    fun triggerBattlecry(gameEngine: GameEngine, targets: List<Any> = emptyList()) {
        battlecryEffect?.invoke(gameEngine, targets)
    }

    private fun triggerDeathrattle() {
        deathrattleEffect?.invoke(GameEngine.current)
    }

    fun canAttack(): Boolean = canAttackThisTurn && !hasAttackedThisTurn && currentHealth > 0

    fun isDead(): Boolean = currentHealth <= 0

    fun resetForNewTurn() {
        hasAttackedThisTurn = false
        canAttackThisTurn = true // Can attack after first turn
        summoned = false
    }

    fun heal(amount: Int) {
        currentHealth = minOf(currentHealth + amount, maxHealth)
    }

    fun gainDivineShield() {
        hasDivineShield = true
    }

    fun buffAttack(amount: Int) {
        attack += amount
    }

    fun buffHealth(amount: Int) {
        maxHealth += amount
        currentHealth += amount
    }
}
