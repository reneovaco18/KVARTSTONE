package com.rench.kvartstone.domain

data class GameState(
    val currentTurn: Turn,
    val turnNumber: Int,
    val playerMana: Int,
    val playerMaxMana: Int,
    val gameOver: Boolean,
    val playerWon: Boolean,
    val gamePhase: GamePhase,
    val isProcessingTurn: Boolean
)
