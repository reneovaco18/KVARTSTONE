package com.rench.kvartstone.domain

data class MinionCard(
    override val id: Int,
    override val name: String,
    override val manaCost: Int,
    override val imageRes: Int,
    override val imageUri: String? = null,
    var attack: Int,
    var maxHealth: Int,
    var currentHealth: Int = maxHealth,
    var hasDivineShield: Boolean = false,
    var hasAttackedThisTurn: Boolean = false,
    var canAttackThisTurn: Boolean = false,
    var battlecryEffect: ((GameEngineInterface, List<Any>) -> Unit)? = null,
    var deathrattleEffect: ((GameEngineInterface) -> Unit)? = null,
    var summoned: Boolean = false
) : Card(id, name, manaCost, imageRes, imageUri) {

    val health: Int get() = currentHealth

    fun takeDamage(amount: Int, gameEngine: GameEngineInterface? = null) {
        if (hasDivineShield) {
            hasDivineShield = false
            return
        }
        currentHealth -= amount
        if (currentHealth <= 0) {
            triggerDeathrattle(gameEngine)
        }
    }

    fun triggerBattlecry(gameEngine: GameEngineInterface, targets: List<Any> = emptyList()) {
        battlecryEffect?.invoke(gameEngine, targets)
    }

    private fun triggerDeathrattle(gameEngine: GameEngineInterface? = null) {
        val engine = gameEngine ?: getCurrentEngine()
        deathrattleEffect?.invoke(engine)
    }

    private fun getCurrentEngine(): GameEngineInterface {
        return try {
            ImprovedGameEngine.current
        } catch (e: Exception) {
            try {
                GameEngine.current
            } catch (e2: Exception) {
                throw IllegalStateException("No game engine instance available for deathrattle trigger")
            }
        }
    }

    fun canAttack(): Boolean = canAttackThisTurn && !hasAttackedThisTurn && currentHealth > 0
    fun isDead(): Boolean = currentHealth <= 0

    fun resetForNewTurn() {
        hasAttackedThisTurn = false
        canAttackThisTurn = true
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
