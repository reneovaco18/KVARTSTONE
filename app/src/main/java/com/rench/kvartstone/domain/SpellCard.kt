package com.rench.kvartstone.domain

import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

enum class TargetingType {
    NO_TARGET, SINGLE_MINION, SINGLE_FRIENDLY_MINION, SINGLE_ENEMY_MINION,
    SINGLE_CHARACTER, ALL_MINIONS, ALL_ENEMY_MINIONS, ALL_FRIENDLY_MINIONS,
    RANDOM_ENEMY, RANDOM_MINION
}

@Parcelize
data class SpellCard(
    override val id: Int,
    override val name: String,
    override val manaCost: Int,
    override val imageResName: String,
    override val imageUri: String? = null,
    val effect: @RawValue (GameEngineInterface, List<Any>) -> Unit,
    val targetingType: TargetingType = TargetingType.NO_TARGET,
    val description: String = ""
) : Card(id, name, manaCost, imageResName, imageUri) {

    fun cast(engine: GameEngineInterface, explicit: List<Any> = emptyList()) {
        val targets = if (explicit.isNotEmpty()) explicit else autoTargets(engine)
        effect(engine, targets)
    }

    fun requiresTarget() = targetingType in listOf(
        TargetingType.SINGLE_MINION,
        TargetingType.SINGLE_FRIENDLY_MINION,
        TargetingType.SINGLE_ENEMY_MINION,
        TargetingType.SINGLE_CHARACTER
    )

    fun getValidTargets(engine: GameEngineInterface): List<Any> = when (targetingType) {
        TargetingType.SINGLE_MINION          -> engine.playerBoard + engine.botBoard
        TargetingType.SINGLE_FRIENDLY_MINION -> if (engine.currentTurn == Turn.PLAYER)
            engine.playerBoard else engine.botBoard
        TargetingType.SINGLE_ENEMY_MINION    -> if (engine.currentTurn == Turn.PLAYER)
            engine.botBoard else engine.playerBoard
        TargetingType.SINGLE_CHARACTER       -> if (engine.currentTurn == Turn.PLAYER)
            engine.playerBoard + engine.botBoard + listOf(engine.botHero)
        else
            engine.playerBoard + engine.botBoard + listOf(engine.playerHero)
        else -> emptyList()
    }

    /* ---------- helpers ---------- */

    private fun autoTargets(engine: GameEngineInterface): List<Any> = when (targetingType) {
        TargetingType.NO_TARGET       -> emptyList()
        TargetingType.ALL_MINIONS     -> engine.playerBoard + engine.botBoard
        TargetingType.ALL_ENEMY_MINIONS ->
            if (engine.currentTurn == Turn.PLAYER) engine.botBoard else engine.playerBoard
        TargetingType.ALL_FRIENDLY_MINIONS ->
            if (engine.currentTurn == Turn.PLAYER) engine.playerBoard else engine.botBoard
        TargetingType.RANDOM_ENEMY    -> randomEnemy(engine)
        TargetingType.RANDOM_MINION   -> randomMinion(engine)
        else                          -> emptyList()
    }

    private fun randomEnemy(engine: GameEngineInterface): List<Any> {
        val pool = if (engine.currentTurn == Turn.PLAYER)
            engine.botBoard + listOf(engine.botHero)
        else
            engine.playerBoard + listOf(engine.playerHero)
        return if (pool.isEmpty()) emptyList() else listOf(pool.random())
    }

    private fun randomMinion(engine: GameEngineInterface): List<Any> {
        val pool = engine.playerBoard + engine.botBoard
        return if (pool.isEmpty()) emptyList() else listOf(pool.random())
    }
}
