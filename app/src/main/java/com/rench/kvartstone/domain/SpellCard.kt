package com.rench.kvartstone.domain

enum class TargetingType {
    NO_TARGET,
    SINGLE_MINION,
    SINGLE_FRIENDLY_MINION,
    SINGLE_ENEMY_MINION,
    SINGLE_CHARACTER,
    ALL_MINIONS,
    ALL_ENEMY_MINIONS,
    ALL_FRIENDLY_MINIONS,
    RANDOM_ENEMY,
    RANDOM_MINION
}

data class SpellCard(
    override val id: Int,
    override val name: String,
    override val manaCost: Int,
    override val imageRes: Int,
    override val imageUri: String? = null,
    val effect: (GameEngineInterface, List<Any>) -> Unit,
    val targetingType: TargetingType = TargetingType.NO_TARGET,
    val description: String = ""
) : Card(id, name, manaCost, imageRes, imageUri) {

    fun cast(gameEngine: GameEngineInterface, explicitTargets: List<Any> = emptyList()) {
        val finalTargets = if (explicitTargets.isEmpty()) {
            when (targetingType) {
                TargetingType.NO_TARGET -> emptyList()
                TargetingType.ALL_MINIONS -> gameEngine.playerBoard + gameEngine.botBoard
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
                else -> explicitTargets
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
            TargetingType.SINGLE_MINION -> gameEngine.playerBoard + gameEngine.botBoard
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
            else -> emptyList()
        }
    }
}
