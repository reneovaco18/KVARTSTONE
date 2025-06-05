package com.rench.kvartstone.domain

enum class TargetingType {
    NO_TARGET,            // No target needed (e.g., board clears)
    SINGLE_MINION,        // Target any one minion
    SINGLE_FRIENDLY_MINION, // Target only friendly minions
    SINGLE_ENEMY_MINION,  // Target only enemy minions
    SINGLE_CHARACTER,     // Target any minion or hero
    ALL_MINIONS,          // Affects all minions
    ALL_ENEMY_MINIONS,    // Affects all enemy minions
    ALL_FRIENDLY_MINIONS, // Affects all friendly minions
    RANDOM_ENEMY,         // Random enemy character
    RANDOM_MINION         // Random minion
}

data class SpellCard(
    override val id: Int,
    override val name: String,
    override val manaCost: Int,
    override val imageRes: Int,
    val effect: (GameEngineInterface, List<Any>) -> Unit,
    val targetingType: TargetingType = TargetingType.NO_TARGET,
    val description: String = ""
) : Card(id, name, manaCost, imageRes) {

    fun cast(gameEngine: GameEngineInterface, explicitTargets: List<Any> = emptyList()) {
        val finalTargets = if (explicitTargets.isEmpty()) {
            // Auto-targeting based on targeting type
            when (targetingType) {
                TargetingType.NO_TARGET -> emptyList()

                TargetingType.ALL_MINIONS ->
                    gameEngine.playerBoard + gameEngine.botBoard

                TargetingType.ALL_ENEMY_MINIONS -> {
                    if (gameEngine.currentTurn == Turn.PLAYER) gameEngine.botBoard
                    else gameEngine.playerBoard
                }

                TargetingType.ALL_FRIENDLY_MINIONS -> {
                    if (gameEngine.currentTurn == Turn.PLAYER) gameEngine.playerBoard
                    else gameEngine.botBoard
                }

                TargetingType.RANDOM_ENEMY -> {
                    val enemyCharacters = if (gameEngine.currentTurn == Turn.PLAYER) {
                        gameEngine.botBoard + listOf(gameEngine.botHero)
                    } else {
                        gameEngine.playerBoard + listOf(gameEngine.playerHero)
                    }
                    if (enemyCharacters.isNotEmpty()) listOf(enemyCharacters.random()) else emptyList()
                }

                TargetingType.RANDOM_MINION -> {
                    val allMinions = gameEngine.playerBoard + gameEngine.botBoard
                    if (allMinions.isNotEmpty()) listOf(allMinions.random()) else emptyList()
                }

                else -> explicitTargets // For other types, we need explicit targets
            }
        } else explicitTargets

        effect(gameEngine, finalTargets)
    }

    fun requiresTarget(): Boolean {
        return targetingType in listOf(
            TargetingType.SINGLE_MINION,
            TargetingType.SINGLE_FRIENDLY_MINION,
            TargetingType.SINGLE_ENEMY_MINION,
            TargetingType.SINGLE_CHARACTER
        )
    }

    fun getValidTargets(gameEngine: GameEngineInterface): List<Any> {
        return when (targetingType) {
            TargetingType.SINGLE_MINION ->
                gameEngine.playerBoard + gameEngine.botBoard

            TargetingType.SINGLE_FRIENDLY_MINION -> {
                if (gameEngine.currentTurn == Turn.PLAYER) gameEngine.playerBoard
                else gameEngine.botBoard
            }

            TargetingType.SINGLE_ENEMY_MINION -> {
                if (gameEngine.currentTurn == Turn.PLAYER) gameEngine.botBoard
                else gameEngine.playerBoard
            }

            TargetingType.SINGLE_CHARACTER -> {
                if (gameEngine.currentTurn == Turn.PLAYER) {
                    gameEngine.playerBoard + gameEngine.botBoard + listOf(gameEngine.botHero)
                } else {
                    gameEngine.playerBoard + gameEngine.botBoard + listOf(gameEngine.playerHero)
                }
            }

            else -> emptyList() // No valid targets for other targeting types
        }
    }
}
