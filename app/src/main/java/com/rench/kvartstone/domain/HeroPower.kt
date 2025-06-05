package com.rench.kvartstone.domain

data class HeroPower(
    val id: Int = 0,
    val name: String,
    val description: String = "",
    val cost: Int,
    val imageRes: Int,
    val imageResName: String = "",
    val effect: (GameEngineInterface, Any?) -> Unit,
    var usedThisTurn: Boolean = false
) {
    fun canUse(currentMana: Int): Boolean {
        return !usedThisTurn && currentMana >= cost
    }

    fun use(gameEngine: GameEngineInterface, target: Any? = null) {
        if (canUse(gameEngine.playerMana)) {
            effect(gameEngine, target)
            usedThisTurn = true
            // Note: Mana reduction is now handled by the calling code to maintain flexibility
        }
    }

    fun resetForNewTurn() {
        usedThisTurn = false
    }
}
