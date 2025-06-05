// Enhanced Game Engine with improved AI and turn management
package com.rench.kvartstone.domain

import kotlin.random.Random
import kotlinx.coroutines.*

class ImprovedGameEngine(
    playerDeckCards: List<Card>,
    botDeckCards: List<Card>,
    val playerHero: Hero,
    val botHero: Hero
) {
    companion object {
        lateinit var current: ImprovedGameEngine
    }

    var currentTurn = Turn.PLAYER
    var playerMana = 1
    var botMana = 1
    var playerMaxMana = 1
    var botMaxMana = 1
    var turnNumber = 1
    var gamePhase = GamePhase.MULLIGAN

    val playerDeck = playerDeckCards.toMutableList().shuffled().toMutableList()
    val botDeck = botDeckCards.toMutableList().shuffled().toMutableList()
    val playerHand = mutableListOf<Card>()
    val botHand = mutableListOf<Card>()
    val playerBoard = mutableListOf<MinionCard>()
    val botBoard = mutableListOf<MinionCard>()

    var gameOver = false
    var playerWon = false
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

    fun drawCardForPlayer(): Card? {
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

    fun drawCardForBot(): Card? {
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

    fun playCardFromHand(cardIndex: Int, target: Any? = null): Boolean {
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

    fun playerMinionAttack(attackerIndex: Int, targetType: String, targetIndex: Int = -1): Boolean {
        if (currentTurn != Turn.PLAYER || attackerIndex >= playerBoard.size || isProcessingTurn) return false

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
            performAttack(attacker, target)
        } else false
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

    private fun botPlayCard(cardIndex: Int?, target: Any?): Boolean {
        if (cardIndex == null || cardIndex >= botHand.size) return false

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

    fun endTurn() {
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

                // Bot turn will be handled by UI calling processBotTurn()
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

    fun useHeroPower(target: Any? = null): Boolean {
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

// Game phases for better state management
enum class GamePhase {
    MULLIGAN,
    MAIN_GAME,
    GAME_OVER
}

// Game state data class for UI
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

// AI Decision classes
sealed class AIDecision {
    data class PlayCard(val cardIndex: Int, val target: Any?) : AIDecision()
    data class Attack(val attackerIndex: Int, val target: Any) : AIDecision()
    data class UseHeroPower(val target: Any?) : AIDecision()
}

// Enhanced AI Evaluator
class AIEvaluator(private val gameEngine: ImprovedGameEngine) {

    fun planTurn(hand: List<Card>, friendlyBoard: List<MinionCard>, enemyBoard: List<MinionCard>, mana: Int): List<AIDecision> {
        val decisions = mutableListOf<AIDecision>()
        var remainingMana = mana
        val availableCards = hand.toMutableList()

        // Phase 1: Play efficient cards
        while (remainingMana > 0 && availableCards.isNotEmpty()) {
            val bestCard = findBestCard(availableCards, remainingMana, enemyBoard)
            if (bestCard != null) {
                val cardIndex = hand.indexOf(bestCard)
                val target = if (bestCard is SpellCard) {
                    findBestSpellTarget(bestCard, enemyBoard, gameEngine.playerHero)
                } else null

                decisions.add(AIDecision.PlayCard(cardIndex, target))
                remainingMana -= bestCard.manaCost
                availableCards.remove(bestCard)
            } else {
                break
            }
        }

        return decisions
    }

    private fun findBestCard(cards: List<Card>, mana: Int, enemyBoard: List<MinionCard>): Card? {
        val playableCards = cards.filter { it.manaCost <= mana }
        if (playableCards.isEmpty()) return null

        // Prioritize based on game state
        return when {
            enemyBoard.size > 2 -> {
                // Prioritize removal spells
                playableCards.filterIsInstance<SpellCard>()
                    .filter { it.targetingType == TargetingType.ALL_ENEMY_MINIONS }
                    .maxByOrNull { evaluateCard(it) }
                    ?: playableCards.maxByOrNull { evaluateCard(it) }
            }
            else -> {
                // Prioritize efficient minions
                playableCards.maxByOrNull { evaluateCard(it) }
            }
        }
    }

    private fun evaluateCard(card: Card): Double {
        return when (card) {
            is MinionCard -> {
                val statValue = (card.attack + card.maxHealth) / card.manaCost.toDouble()
                val efficiency = if (card.manaCost == 0) 10.0 else statValue

                // Bonus for keywords
                val keywordBonus = when {
                    card.hasDivineShield -> 1.5
                    card.battlecryEffect != null -> 1.2
                    card.deathrattleEffect != null -> 1.1
                    else -> 1.0
                }

                efficiency * keywordBonus
            }
            is SpellCard -> {
                // Higher value for removal spells
                when (card.targetingType) {
                    TargetingType.ALL_ENEMY_MINIONS -> 8.0
                    TargetingType.SINGLE_CHARACTER -> 6.0
                    TargetingType.RANDOM_ENEMY -> 5.0
                    else -> 4.0
                }
            }
            else -> 1.0
        }
    }

    fun findBestAttackTarget(attacker: MinionCard, enemyBoard: List<MinionCard>, enemyHero: Hero): Any? {
        if (!attacker.canAttack()) return null

        // Check for taunt minions first
        val tauntMinions = enemyBoard.filter { it.keywords?.contains("taunt") == true }
        if (tauntMinions.isNotEmpty()) {
            return findOptimalTradeTarget(attacker, tauntMinions) ?: tauntMinions.first()
        }

        // If no taunts, consider all targets
        val allTargets = enemyBoard + listOf(enemyHero)
        return findBestTarget(attacker, allTargets, enemyHero)
    }

    private fun findOptimalTradeTarget(attacker: MinionCard, targets: List<MinionCard>): MinionCard? {
        return targets.filter { target ->
            // Can kill target without dying
            target.currentHealth <= attacker.attack &&
                    (attacker.currentHealth > target.attack || attacker.hasDivineShield)
        }.maxByOrNull { it.attack + it.currentHealth } // Prioritize valuable targets
    }

    private fun findBestTarget(attacker: MinionCard, allTargets: List<Any>, enemyHero: Hero): Any {
        val minions = allTargets.filterIsInstance<MinionCard>()

        // Prioritize killing enemy minions
        val killableMinions = minions.filter { it.currentHealth <= attacker.attack }
        if (killableMinions.isNotEmpty()) {
            return killableMinions.maxByOrNull { it.attack + it.currentHealth } ?: killableMinions.first()
        }

        // If can't kill any minions, consider going face if it's lethal
        if (enemyHero.currentHealth <= attacker.attack) {
            return enemyHero
        }

        // Otherwise, make efficient trades
        val efficientTrades = minions.filter { target ->
            val damageRatio = attacker.attack.toDouble() / target.currentHealth
            damageRatio >= 0.5 // Only attack if dealing significant damage
        }

        return efficientTrades.maxByOrNull { it.attack + it.currentHealth } ?: enemyHero
    }

    fun findBestSpellTarget(spell: SpellCard, enemyBoard: List<MinionCard>, enemyHero: Hero): Any? {
        return when (spell.targetingType) {
            TargetingType.SINGLE_CHARACTER -> {
                // Prioritize biggest threat or lethal on hero
                if (enemyHero.currentHealth <= 6 && spell.description.contains("damage", true)) {
                    enemyHero
                } else {
                    enemyBoard.maxByOrNull { it.attack + it.currentHealth }
                }
            }
            TargetingType.SINGLE_ENEMY_MINION -> {
                enemyBoard.maxByOrNull { it.attack + it.currentHealth }
            }
            else -> null
        }
    }

    fun canPlayMoreCards(hand: List<Card>, mana: Int): Boolean {
        return hand.any { it.manaCost <= mana }
    }

    fun findBestPlayableCard(hand: List<Card>, mana: Int): Int? {
        val playableCards = hand.mapIndexedNotNull { index, card ->
            if (card.manaCost <= mana) index to card else null
        }

        return playableCards.maxByOrNull { (_, card) -> evaluateCard(card) }?.first
    }
}