package com.rench.kvartstone.domain

data class HeroPower(
    val name: String,
    val cost: Int,
    val imageRes: Int,
    val description: String,
    val effect: (GameEngine, Any?) -> Unit,
    val targetingType: TargetingType = TargetingType.NO_TARGET,
    var usedThisTurn: Boolean = false
) {
    fun canUse(currentMana: Int): Boolean {
        return !usedThisTurn && currentMana >= cost
    }

    fun use(gameEngine: GameEngine, target: Any? = null) {
        if (canUse(gameEngine.playerMana)) {
            effect(gameEngine, target)
            usedThisTurn = true
            gameEngine.playerMana -= cost
        }
    }

    fun resetForNewTurn() {
        usedThisTurn = false
    }
}
