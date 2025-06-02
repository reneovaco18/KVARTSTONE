package com.rench.kvartstone.domain

// HeroPower.kt
data class HeroPower(
    val name: String,
    val cost: Int,
    val imageRes: Int,
    val effect: (GameEngine, Any?) -> Unit,
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
