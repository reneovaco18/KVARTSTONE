// GameEngine.kt
package com.rench.kvartstone.domain

import kotlin.random.Random

enum class Turn { PLAYER, BOT }

class GameEngine(
    playerDeckCards: List<Card>,
    botDeckCards: List<Card>,
    val playerHero: Hero,
    val botHero: Hero
) {
    // Game state
    var currentTurn = Turn.PLAYER
    var playerMana = 1
    var botMana = 1
    var turnNumber = 1

    // Cards
    val playerDeck = playerDeckCards.toMutableList()
    val botDeck = botDeckCards.toMutableList()
    val playerHand = mutableListOf<Card>()
    val botHand = mutableListOf<Card>()
    val playerBoard = mutableListOf<MinionCard>()
    val botBoard = mutableListOf<MinionCard>()

    // Game state flags
    var gameOver = false
    var playerWon = false

    init {
        // Initial draw
        repeat(3) { drawCardForPlayer() }
        repeat(4) { drawCardForBot() }
    }

    fun drawCardForPlayer(): Card? {
        if (playerDeck.isEmpty()) return null

        val card = playerDeck.removeAt(0)
        playerHand.add(card)
        return card
    }

    fun drawCardForBot(): Card? {
        if (botDeck.isEmpty()) return null

        val card = botDeck.removeAt(0)
        botHand.add(card)
        return card
    }

    fun playCardFromHand(cardIndex: Int, target: Any? = null): Boolean {
        if (currentTurn != Turn.PLAYER || cardIndex >= playerHand.size) return false

        val card = playerHand[cardIndex]

        // Check mana
        if (card.manaCost > playerMana) return false

        when (card) {
            is MinionCard -> {
                // Check if board is full
                if (playerBoard.size >= 7) return false

                playerHand.removeAt(cardIndex)
                playerBoard.add(card)
                card.canAttackThisTurn = false // Summoning sickness
                playerMana -= card.manaCost
                return true
            }

            is SpellCard -> {
                playerHand.removeAt(cardIndex)
                playerMana -= card.manaCost
                card.effect(this, listOfNotNull(target))
                return true
            }
        }
    }

    fun playerMinionAttack(attackerIndex: Int, targetType: String, targetIndex: Int = -1): Boolean {
        if (currentTurn != Turn.PLAYER || attackerIndex >= playerBoard.size) return false

        val attacker = playerBoard[attackerIndex]

        if (!attacker.canAttackThisTurn || attacker.hasAttackedThisTurn) return false

        when (targetType) {
            "hero" -> {
                botHero.takeDamage(attacker.attack)
                attacker.hasAttackedThisTurn = true

                if (botHero.isDead) {
                    gameOver = true
                    playerWon = true
                }

                return true
            }

            "minion" -> {
                if (targetIndex >= botBoard.size) return false

                val target = botBoard[targetIndex]
                target.takeDamage(attacker.attack)
                attacker.takeDamage(target.attack)
                attacker.hasAttackedThisTurn = true

                // Remove dead minions
                if (target.isDead) {
                    botBoard.remove(target)
                }

                if (attacker.isDead) {
                    playerBoard.remove(attacker)
                }

                return true
            }

            else -> return false
        }
    }

    fun endTurn() {
        if (currentTurn == Turn.PLAYER) {
            currentTurn = Turn.BOT
            botTakeTurn()
        } else {
            currentTurn = Turn.PLAYER
            turnNumber++
            playerMana = minOf(10, turnNumber)

            // Reset player minions for new turn
            playerBoard.forEach { it.resetForTurn() }
            playerHero.resetForTurn()

            // Draw card for player
            drawCardForPlayer()
        }
    }

    private fun botTakeTurn() {
        // Simple AI implementation
        botMana = minOf(10, turnNumber)
        drawCardForBot()

        // Reset bot minions for new turn
        botBoard.forEach { it.resetForTurn() }
        botHero.resetForTurn()

        // Play cards if possible (very simple AI)
        val cardsToPlay = botHand.indices.filter {
            botHand[it].manaCost <= botMana
        }.shuffled()

        for (cardIndex in cardsToPlay) {
            val card = botHand[cardIndex]
            if (card.manaCost <= botMana) {
                when (card) {
                    is MinionCard -> {
                        if (botBoard.size < 7) {
                            botHand.removeAt(cardIndex)
                            botBoard.add(card)
                            botMana -= card.manaCost
                            break // Play one card per turn for now
                        }
                    }

                    is SpellCard -> {
                        // Simple targeting for spells
                        val target = if (playerBoard.isNotEmpty()) {
                            playerBoard[Random.nextInt(playerBoard.size)]
                        } else {
                            playerHero
                        }

                        botHand.removeAt(cardIndex)
                        botMana -= card.manaCost
                        card.effect(this, listOf(target))
                        break // Play one card per turn for now
                    }
                }
            }
        }

        // Attack with minions
        for (attacker in botBoard.filter { it.canAttackThisTurn }) {
            if (playerBoard.isNotEmpty()) {
                // Attack a random player minion
                val targetIndex = Random.nextInt(playerBoard.size)
                val target = playerBoard[targetIndex]

                target.takeDamage(attacker.attack)
                attacker.takeDamage(target.attack)
                attacker.hasAttackedThisTurn = true

                // Remove dead minions
                if (target.isDead) {
                    playerBoard.remove(target)
                }

                if (attacker.isDead) {
                    botBoard.remove(attacker)
                }
            } else {
                // Attack player hero
                playerHero.takeDamage(attacker.attack)
                attacker.hasAttackedThisTurn = true

                if (playerHero.isDead) {
                    gameOver = true
                    playerWon = false
                }
            }
        }

        // End turn
        endTurn()
    }
}
