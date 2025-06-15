package com.rench.kvartstone.domain

data class HeroPower(
    val id: Int,
    val name: String,
    val description: String,
    val cost: Int,
    val imageResName: String,
    val effect: (GameEngineInterface, Any?) -> Unit
) {
    var usedThisTurn: Boolean = false
        private set

    // Smart targeting detection
    val isTargeted: Boolean
        get() = when (id) {
            1 -> true   // Fireblast targets any character
            else -> false   // All others are self-targeted or non-targeted
        }

    fun canUse(currentMana: Int) = !usedThisTurn && currentMana >= cost

    fun use(engine: GameEngineInterface, target: Any?) {
        if (canUse(if (engine.currentTurn == Turn.PLAYER) engine.playerMana else engine.botMana)) {
            effect(engine, target)
            usedThisTurn = true
        }
    }

    fun resetForNewTurn() {
        usedThisTurn = false
    }
}
