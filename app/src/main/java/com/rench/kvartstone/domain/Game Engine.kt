package com.rench.kvartstone.domain

import kotlin.random.Random

open class GameEngine(
    playerDeckCards: List<Card>,
    botDeckCards: List<Card>,
    override val playerHero: Hero,
    override val botHero: Hero
) : GameEngineInterface {
    companion object {
        lateinit var current: GameEngineInterface
    }

    override var currentTurn = Turn.PLAYER
    override var playerMana = 1
    override var botMana = 1
    override var playerMaxMana = 1
    override var botMaxMana = 1
    override var turnNumber = 1

    override val playerDeck = playerDeckCards.toMutableList().shuffled().toMutableList()
    override val botDeck = botDeckCards.toMutableList().shuffled().toMutableList()
    override val playerHand = mutableListOf<Card>()
    override val botHand = mutableListOf<Card>()
    override val playerBoard = mutableListOf<MinionCard>()
    override val botBoard = mutableListOf<MinionCard>()

    override var gameOver = false
    override var playerWon = false

    init {
        current = this
        repeat(3) { drawCardForPlayer() }
        repeat(4) { drawCardForBot() }
    }

    override fun drawCardForPlayer(): Card? {
        if (playerDeck.isEmpty()) return null

        val card = playerDeck.removeAt(0)
        if (playerHand.size < 10) { // Hand limit
            playerHand.add(card)
        }
        return card
    }

    override fun drawCardForBot(): Card? {
        if (botDeck.isEmpty()) return null

        val card = botDeck.removeAt(0)
        if (botHand.size < 10) { // Hand limit
            botHand.add(card)
        }
        return card
    }

    override fun playCardFromHand(cardIndex: Int, target: Any?): Boolean {
        if (currentTurn != Turn.PLAYER || cardIndex >= playerHand.size) return false

        val card = playerHand[cardIndex]
        if (card.manaCost > playerMana) return false

        playerMana -= card.manaCost
        playerHand.removeAt(cardIndex)

        when (card) {
            is MinionCard -> {
                if (playerBoard.size < 7) { // Board limit
                    card.summoned = true
                    card.canAttackThisTurn = false
                    playerBoard.add(card)


                    val targets = if (target != null) listOf(target) else emptyList()
                    card.triggerBattlecry(this, targets)
                }
            }
            is SpellCard -> {
                val targets = if (target != null) listOf(target) else emptyList()
                card.cast(this, targets)
            }
        }


        cleanupDeadMinions()
        return true
    }

    fun playerMinionAttack(attackerIndex: Int, targetType: String, targetIndex: Int = -1): Boolean {
        if (currentTurn != Turn.PLAYER || attackerIndex >= playerBoard.size) return false

        val attacker = playerBoard[attackerIndex]
        if (!attacker.canAttack()) return false

        val target = when (targetType) {
            "hero" -> botHero
            "minion" -> {
                if (targetIndex >= 0 && targetIndex < botBoard.size) {
                    botBoard[targetIndex]
                } else null
            }
            else -> null
        }

        return if (target != null) {
            attack(attacker, target)
        } else false
    }


    override fun attack(attacker: MinionCard, target: Any): Boolean {
        if (!attacker.canAttack()) return false

        when (target) {
            is MinionCard -> {

                attacker.takeDamage(target.attack)
                target.takeDamage(attacker.attack)
            }
            is Hero -> {
                target.takeDamage(attacker.attack)
                if (target.isDead()) {
                    endGame(target == botHero)
                }
            }
        }

        attacker.hasAttackedThisTurn = true
        cleanupDeadMinions()
        return true
    }

    fun botPlayCard(): Boolean {
        if (botHand.isEmpty()) return false


        for (i in botHand.indices) {
            val card = botHand[i]
            if (card.manaCost <= botMana) {
                botMana -= card.manaCost
                botHand.removeAt(i)

                when (card) {
                    is MinionCard -> {
                        if (botBoard.size < 7) {
                            card.summoned = true
                            card.canAttackThisTurn = false
                            botBoard.add(card)


                            val targets = if (playerBoard.isNotEmpty()) {
                                listOf(playerBoard.random())
                            } else {
                                listOf(playerHero)
                            }
                            card.triggerBattlecry(this, targets)
                        }
                    }
                    is SpellCard -> {

                        val targets = when (card.targetingType) {
                            TargetingType.SINGLE_MINION -> {
                                if (playerBoard.isNotEmpty()) listOf(playerBoard.random()) else emptyList()
                            }
                            TargetingType.SINGLE_CHARACTER -> {
                                val allTargets = playerBoard + listOf(playerHero)
                                if (allTargets.isNotEmpty()) listOf(allTargets.random()) else emptyList()
                            }
                            else -> emptyList()
                        }
                        card.cast(this, targets)
                    }
                }

                cleanupDeadMinions()
                return true
            }
        }
        return false
    }

    private fun cleanupDeadMinions() {
        playerBoard.removeAll { it.isDead() }
        botBoard.removeAll { it.isDead() }
    }

    override fun endTurn() {
        when (currentTurn) {
            Turn.PLAYER -> {
                currentTurn = Turn.BOT
                botBoard.forEach { it.resetForNewTurn() }
                playerHero.resetHeroPower()
                botTurn()
            }
            Turn.BOT -> {
                currentTurn = Turn.PLAYER
                turnNumber++


                playerMaxMana = minOf(playerMaxMana + 1, 10)
                botMaxMana = minOf(botMaxMana + 1, 10)
                playerMana = playerMaxMana
                botMana = botMaxMana


                drawCardForPlayer()
                drawCardForBot()


                playerBoard.forEach { it.resetForNewTurn() }
                botHero.resetHeroPower()
            }
        }
    }

    private fun botTurn() {

        while (botPlayCard()) {

        }


        botBoard.filter { it.canAttack() }.forEach { minion ->
            val targets = playerBoard + listOf(playerHero)
            if (targets.isNotEmpty()) {
                attack(minion, targets.random())
            }
        }

        endTurn()
    }

    fun endGame(playerWins: Boolean) {
        gameOver = true
        playerWon = playerWins
    }

    override fun useHeroPower(target: Any?): Boolean {
        if (currentTurn != Turn.PLAYER) return false

        if (playerHero.heroPower.canUse(playerMana)) {
            playerHero.heroPower.use(this, target)
            playerMana -= playerHero.heroPower.cost
            return true
        }
        return false
    }
}
