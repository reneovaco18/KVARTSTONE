package com.rench.kvartstone.domain

sealed class AIDecision {
    data class PlayCard(val cardIndex: Int, val target: Any?) : AIDecision()
    data class Attack(val attackerIndex: Int, val target: Any) : AIDecision()
    data class UseHeroPower(val target: Any?) : AIDecision()
    object EndTurn : AIDecision()
}
