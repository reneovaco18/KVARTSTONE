package com.rench.kvartstone.domain

import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class MinionCard(
    override val id: Int,
    override val name: String,
    override val manaCost: Int,
    override val imageResName: String,
    override val imageUri: String? = null,
    var attack: Int,
    var maxHealth: Int,
    var currentHealth: Int = maxHealth,
    var hasDivineShield: Boolean = false,
    var hasAttackedThisTurn: Boolean = false,
    var canAttackThisTurn: Boolean = false,


    var battlecryEffect : @RawValue ((GameEngineInterface, List<Any>) -> Unit)? = null,
    var deathrattleEffect: @RawValue ((GameEngineInterface, List<Any>) -> Unit)? = null,
    var summoned: Boolean = false,

) : Card(id, name, manaCost, imageResName, imageUri) {

    val health: Int get() = currentHealth

    fun takeDamage(amount: Int, gameEngine: GameEngineInterface? = null) {
        if (hasDivineShield) {
            hasDivineShield = false
            return
        }
        currentHealth -= amount
        if (currentHealth <= 0) triggerDeathrattle(gameEngine)
    }

    fun triggerBattlecry(engine: GameEngineInterface, targets: List<Any> = emptyList()) {
        battlecryEffect?.invoke(engine, targets)
    }

    private fun triggerDeathrattle(engine: GameEngineInterface? = null) {
        val realEngine = engine ?: resolveCurrentEngine()
        deathrattleEffect?.invoke(realEngine, emptyList())
    }

    private fun resolveCurrentEngine(): GameEngineInterface =
        try {
            ImprovedGameEngine.current
        } catch (e: Exception) {
            GameEngine.current
        }

    fun canAttack() = canAttackThisTurn && !hasAttackedThisTurn && currentHealth > 0
    fun isDead()  = currentHealth <= 0

    fun resetForNewTurn() {
        hasAttackedThisTurn = false
        canAttackThisTurn = true
        summoned = false
    }

    fun heal(amount: Int)          { currentHealth = minOf(currentHealth + amount, maxHealth) }
    fun gainDivineShield()         { hasDivineShield = true }
    fun buffAttack(amount: Int)    { attack += amount }
    fun buffHealth(amount: Int) {
        maxHealth += amount
        currentHealth += amount
    }
}
