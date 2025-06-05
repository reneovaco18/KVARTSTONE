package com.rench.kvartstone.domain

import kotlin.random.Random
import kotlinx.coroutines.*

class ImprovedGameEngine(
    playerDeckCards: List<Card>,
    botDeckCards: List<Card>,
    override val playerHero: Hero,
    override val botHero: Hero
) : GameEngineInterface {
    companion object {
        lateinit var current: ImprovedGameEngine
    }

    override var currentTurn = Turn.PLAYER
    override var playerMana = 1
    override var botMana = 1
    override var playerMaxMana = 1
    override var botMaxMana = 1
    override var turnNumber = 1
    var gamePhase = GamePhase.MULLIGAN

    override val playerDeck = playerDeckCards.toMutableList().shuffled().toMutableList()
    override val botDeck = botDeckCards.toMutableList().shuffled().toMutableList()
    override val playerHand = mutableListOf<Card>()
    override val botHand = mutableListOf<Card>()
    override val playerBoard = mutableListOf<MinionCard>()
    override val botBoard = mutableListOf<MinionCard>()

    override var gameOver = false
    override var playerWon = false
    var isProcessingTurn = false

    // Effect queues for proper sequencing
    private val battlecryQueue = mutableListOf<() -> Unit>()
    private val deathrattleQueue = mutableListOf<() -> Unit>()
    private val endTurnEffects = mutableListOf<() -> Unit>()

    // AI decision making components
    private val aiEvaluator = AIEvaluator(this)

    init {
        current = this
        startGame()
    }

    private fun startGame() {
        gamePhase = GamePhase.MULLIGAN
        // Initial draw with mulligan option
        repeat(3) { drawCardForPlayer() }
        repeat(4) { drawCardForBot() }

        // Skip mulligan for now, go straight to game
        gamePhase = GamePhase.MAIN_GAME
    }

    override fun drawCardForPlayer(): Card? {
        if (playerDeck.isEmpty()) {
            // Fatigue damage
            playerHero.takeDamage(playerHand.size + 1)
            return null
        }

        val card = playerDeck.removeAt(0)
        if (playerHand.size < 10) {
            playerHand.add(card)
        }
        return card
    }

    override fun drawCardForBot(): Card? {
        if (botDeck.isEmpty()) {
            // Fatigue damage
            botHero.takeDamage(botHand.size + 1)
            return null
        }

        val card = botDeck.removeAt(0)
        if (botHand.size < 10) {
            botHand.add(card)
        }
        return card
    }

    override fun playCardFromHand(cardIndex: Int, target: Any?): Boolean {
        if (currentTurn != Turn.PLAYER || cardIndex >= playerHand.size || isProcessingTurn) return false

        val card = playerHand[cardIndex]
        if (card.manaCost > playerMana) return false

        playerMana -= card.manaCost
        playerHand.removeAt(cardIndex)

        when (card) {
            is MinionCard -> {
                if (playerBoard.size < 7) {
                    playMinion(card, playerBoard)
                }
            }
            is SpellCard -> {
                castSpell(card, target)
            }
        }

        processEffectQueues()
        cleanupBoard()
        checkGameEnd()
        return true
    }

    // Add the missing attack method that implements the interface
    override fun attack(attacker: MinionCard, target: Any): Boolean {
        return performAttack(attacker, target)
    }

    private fun playMinion(minion: MinionCard, board: MutableList<MinionCard>) {
        minion.summoned = true
        minion.canAttackThisTurn = false
        board.add(minion)

        // Queue battlecry effect
        minion.battlecryEffect?.let { effect ->
            battlecryQueue.add {
                effect.invoke(this, emptyList()) // Can be enhanced for targeting
            }
        }
    }

    private fun castSpell(spell: SpellCard, target: Any?) {
        val targets = if (target != null) listOf(target) else determineSpellTargets(spell)
        spell.cast(this, targets)
    }

    private fun determineSpellTargets(spell: SpellCard): List<Any> {
        return when (spell.targetingType) {
            TargetingType.ALL_ENEMY_MINIONS -> {
                if (currentTurn == Turn.PLAYER) botBoard else playerBoard
            }
            TargetingType.ALL_FRIENDLY_MINIONS -> {
                if (currentTurn == Turn.PLAYER) playerBoard else botBoard
            }
            TargetingType.ALL_MINIONS -> playerBoard + botBoard
            TargetingType.RANDOM_ENEMY -> {
                val enemies = if (currentTurn == Turn.PLAYER) {
                    botBoard + listOf(botHero)
                } else {
                    playerBoard + listOf(playerHero)
                }
                if (enemies.isNotEmpty()) listOf(enemies.random()) else emptyList()
            }
            else -> emptyList()
        }
    }

    // Enhanced AI Turn Processing
    suspend fun processBotTurn() {
        if (currentTurn != Turn.BOT || isProcessingTurn) return

        isProcessingTurn = true
        delay(500) // Visual delay for player

        try {
            val aiDecisions = aiEvaluator.planTurn(botHand, botBoard, playerBoard, botMana)

            // Execute AI decisions in order
            for (decision in aiDecisions) {
                when (decision) {
                    is AIDecision.PlayCard -> {
                        if (botPlayCard(decision.cardIndex, decision.target)) {
                            delay(300) // Animation time
                        }
                    }
                    is AIDecision.Attack -> {
                        if (botAttack(decision.attackerIndex, decision.target)) {
                            delay(300)
                        }
                    }
                    is AIDecision.UseHeroPower -> {
                        if (botUseHeroPower(decision.target)) {
                            delay(300)
                        }
                    }
                    is AIDecision.EndTurn -> {
                        // End turn will be handled automatically
                    }
                }
            }

            // Use remaining mana efficiently
            while (aiEvaluator.canPlayMoreCards(botHand, botMana)) {
                val bestCard = aiEvaluator.findBestPlayableCard(botHand, botMana)
                if (bestCard != null && botPlayCard(bestCard, null)) {
                    delay(300)
                } else {
                    break
                }
            }

            // Attack with available minions
            val availableAttackers = botBoard.filter { it.canAttack() }
            for (attacker in availableAttackers) {
                val bestTarget = aiEvaluator.findBestAttackTarget(attacker, playerBoard, playerHero)
                if (bestTarget != null) {
                    val attackerIndex = botBoard.indexOf(attacker)
                    performAttack(attacker, bestTarget)
                    delay(300)
                }
            }

        } finally {
            isProcessingTurn = false
            endTurn()
        }
    }

    private fun botPlayCard(cardIndex: Int, target: Any?): Boolean {
        if (cardIndex >= botHand.size) return false

        val card = botHand[cardIndex]
        if (card.manaCost > botMana) return false

        botMana -= card.manaCost
        botHand.removeAt(cardIndex)

        when (card) {
            is MinionCard -> {
                if (botBoard.size < 7) {
                    playMinion(card, botBoard)
                    return true
                }
            }
            is SpellCard -> {
                val spellTarget = target ?: aiEvaluator.findBestSpellTarget(card, playerBoard, playerHero)
                castSpell(card, spellTarget)
                return true
            }
        }

        processEffectQueues()
        cleanupBoard()
        return false
    }

    private fun botAttack(attackerIndex: Int, target: Any): Boolean {
        if (attackerIndex >= botBoard.size) return false
        val attacker = botBoard[attackerIndex]
        return performAttack(attacker, target)
    }

    private fun botUseHeroPower(target: Any?): Boolean {
        if (botHero.heroPower.canUse(botMana)) {
            botHero.heroPower.use(this, target)
            botMana -= botHero.heroPower.cost
            return true
        }
        return false
    }

    private fun performAttack(attacker: MinionCard, target: Any): Boolean {
        if (!attacker.canAttack()) return false

        when (target) {
            is MinionCard -> {
                // Handle divine shield
                if (target.hasDivineShield) {
                    target.hasDivineShield = false
                } else {
                    target.takeDamage(attacker.attack)
                    if (target.currentHealth <= 0) {
                        queueDeathrattle(target)
                    }
                }

                // Attacker takes damage back
                if (attacker.hasDivineShield) {
                    attacker.hasDivineShield = false
                } else {
                    attacker.takeDamage(target.attack)
                    if (attacker.currentHealth <= 0) {
                        queueDeathrattle(attacker)
                    }
                }
            }
            is Hero -> {
                target.takeDamage(attacker.attack)
                if (target.isDead()) {
                    endGame(target == botHero)
                }
            }
        }

        attacker.hasAttackedThisTurn = true
        processEffectQueues()
        cleanupBoard()
        return true
    }

    private fun queueDeathrattle(minion: MinionCard) {
        minion.deathrattleEffect?.let { effect ->
            deathrattleQueue.add {
                effect.invoke(this)
            }
        }
    }

    private fun processEffectQueues() {
        // Process battlecries first
        while (battlecryQueue.isNotEmpty()) {
            val effect = battlecryQueue.removeAt(0)
            effect.invoke()
        }

        // Then process deathrattles
        while (deathrattleQueue.isNotEmpty()) {
            val effect = deathrattleQueue.removeAt(0)
            effect.invoke()
        }
    }

    private fun cleanupBoard() {
        val deadMinions = playerBoard.filter { it.isDead() }
        deadMinions.forEach { queueDeathrattle(it) }
        playerBoard.removeAll { it.isDead() }

        val deadBotMinions = botBoard.filter { it.isDead() }
        deadBotMinions.forEach { queueDeathrattle(it) }
        botBoard.removeAll { it.isDead() }

        // Process any new deathrattles
        processEffectQueues()
    }

    override fun endTurn() {
        // Process end of turn effects
        while (endTurnEffects.isNotEmpty()) {
            val effect = endTurnEffects.removeAt(0)
            effect.invoke()
        }

        when (currentTurn) {
            Turn.PLAYER -> {
                currentTurn = Turn.BOT
                botBoard.forEach { it.resetForNewTurn() }
                playerHero.resetHeroPower()
            }
            Turn.BOT -> {
                currentTurn = Turn.PLAYER
                turnNumber++

                // Increase mana
                playerMaxMana = minOf(playerMaxMana + 1, 10)
                botMaxMana = minOf(botMaxMana + 1, 10)
                playerMana = playerMaxMana
                botMana = botMaxMana

                // Draw cards
                drawCardForPlayer()
                drawCardForBot()

                // Reset for new turn
                playerBoard.forEach { it.resetForNewTurn() }
                botHero.resetHeroPower()
            }
        }

        checkGameEnd()
    }

    override fun useHeroPower(target: Any?): Boolean {
        if (currentTurn != Turn.PLAYER || !playerHero.heroPower.canUse(playerMana)) return false

        playerHero.heroPower.use(this, target)
        playerMana -= playerHero.heroPower.cost
        processEffectQueues()
        return true
    }

    private fun checkGameEnd() {
        when {
            playerHero.isDead() -> endGame(false)
            botHero.isDead() -> endGame(true)
        }
    }

    private fun endGame(playerWins: Boolean) {
        gameOver = true
        playerWon = playerWins
        gamePhase = GamePhase.GAME_OVER
    }

    // Additional helper methods for the UI
    fun getGameState(): GameState {
        return GameState(
            currentTurn = currentTurn,
            turnNumber = turnNumber,
            playerMana = playerMana,
            playerMaxMana = playerMaxMana,
            gameOver = gameOver,
            playerWon = playerWon,
            gamePhase = gamePhase,
            isProcessingTurn = isProcessingTurn
        )
    }
}
