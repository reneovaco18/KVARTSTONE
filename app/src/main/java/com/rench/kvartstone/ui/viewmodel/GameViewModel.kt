package com.rench.kvartstone.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.rench.kvartstone.R
import com.rench.kvartstone.data.repositories.HeroPowerRepository
import com.rench.kvartstone.data.repositories.DeckRepository
import com.rench.kvartstone.domain.*
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val MAX_HAND_SIZE = 5
        private const val MAX_BOARD_SIZE = 7
    }

    private lateinit var gameEngine: ImprovedGameEngine

    // Repository instances
    private val heroPowerRepository = HeroPowerRepository(application)
    private val deckRepository = DeckRepository(application)

    // LiveData for UI
    private val _playerHand = MutableLiveData<List<Card>>(emptyList())
    val playerHand: LiveData<List<Card>> = _playerHand

    private val _playerBoard = MutableLiveData<List<MinionCard>>(emptyList())
    val playerBoard: LiveData<List<MinionCard>> = _playerBoard

    private val _botBoard = MutableLiveData<List<MinionCard>>(emptyList())
    val botBoard: LiveData<List<MinionCard>> = _botBoard

    private val _playerHero = MutableLiveData<Hero>()
    val playerHero: LiveData<Hero> = _playerHero

    private val _botHero = MutableLiveData<Hero>()
    val botHero: LiveData<Hero> = _botHero

    private val _playerMana = MutableLiveData(1)
    val playerMana: LiveData<Int> = _playerMana

    private val _playerMaxMana = MutableLiveData(1)
    val playerMaxMana: LiveData<Int> = _playerMaxMana

    private val _turnNumber = MutableLiveData(1)
    val turnNumber: LiveData<Int> = _turnNumber

    private val _gameState = MutableLiveData("INITIALIZING")
    val gameState: LiveData<String> = _gameState

    private val _gameMessage = MutableLiveData("")
    val gameMessage: LiveData<String> = _gameMessage

    private val _selectedCard = MutableLiveData<Int?>(null)
    val selectedCard: LiveData<Int?> = _selectedCard

    private val _selectedMinion = MutableLiveData<Int?>(null)
    val selectedMinion: LiveData<Int?> = _selectedMinion

    private val _deckCount = MutableLiveData(30)
    val deckCount: LiveData<Int> = _deckCount

    fun initializeGame(heroPowerId: Int, deckId: Int) {
        viewModelScope.launch {
            try {
                val selectedHeroPower = createDefaultPlayerHeroPower()
                val selectedDeck = createDefaultDeck()

                val playerHero = Hero(
                    name = "Player",
                    maxHealth = 30,
                    imageRes = R.drawable.ic_hero_player,
                    heroPower = selectedHeroPower,
                    heroPowerImageRes = selectedHeroPower.imageRes
                )

                val botHero = Hero(
                    name = "Bot",
                    maxHealth = 30,
                    imageRes = R.drawable.ic_hero_bot,
                    heroPower = createDefaultBotHeroPower(),
                    heroPowerImageRes = R.drawable.ic_hero_power_bot
                )

                gameEngine = ImprovedGameEngine(
                    selectedDeck.cards,
                    createDefaultBotDeck().cards,
                    playerHero,
                    botHero
                )

                limitHandSize()
                updateGameState()
                _gameState.value = "READY"
                _gameMessage.value = "Game started! Click cards to select, click again to play."
            } catch (e: Exception) {
                _gameState.value = "ERROR"
                _gameMessage.value = "Failed to initialize game: ${e.message}"
            }
        }
    }

    private fun limitHandSize() {
        if (::gameEngine.isInitialized) {
            while (gameEngine.playerHand.size > MAX_HAND_SIZE) {
                gameEngine.playerHand.removeLastOrNull()
            }
        }
    }

    private fun createDefaultPlayerHeroPower(): HeroPower {
        return HeroPower(
            id = 1,
            name = "Fireblast",
            description = "Deal 1 damage to any character",
            cost = 2,
            imageRes = R.drawable.ic_hero_power_player,
            effect = { engine, target ->
                when (target) {
                    is MinionCard -> target.takeDamage(1)
                    is Hero -> target.takeDamage(1)
                    else -> engine.botHero.takeDamage(1)
                }
            }
        )
    }

    private fun createDefaultBotHeroPower(): HeroPower {
        return HeroPower(
            id = 2,
            name = "Armor Up!",
            description = "Gain 2 armor",
            cost = 2,
            imageRes = R.drawable.ic_hero_power_bot,
            effect = { engine, _ ->
                engine.botHero.addArmor(2)
            }
        )
    }

    private fun createDefaultDeck(): Deck {
        val cards = CardFactory.createPlayerDeck()
        return Deck(
            id = 1,
            name = "Default Deck",
            description = "A basic starter deck",
            cards = cards
        )
    }

    private fun createDefaultBotDeck(): Deck {
        val cards = CardFactory.createBotDeck()
        return Deck(
            id = 2,
            name = "Bot Deck",
            description = "Bot's deck",
            cards = cards
        )
    }

    fun getValidTargetsForSelectedCard(): List<Any> {
        val selectedCardIndex = selectedCard.value ?: return emptyList()
        val hand = playerHand.value ?: return emptyList()
        if (selectedCardIndex !in hand.indices) return emptyList()

        val card = hand[selectedCardIndex]
        return when (card) {
            is SpellCard -> card.getValidTargets(gameEngine)
            else -> emptyList()
        }
    }

    private fun updateGameState() {
        if (!::gameEngine.isInitialized) return

        _playerHand.value = gameEngine.playerHand.take(MAX_HAND_SIZE)
        _playerBoard.value = gameEngine.playerBoard.toList()
        _botBoard.value = gameEngine.botBoard.toList()
        _playerHero.value = gameEngine.playerHero
        _botHero.value = gameEngine.botHero
        _playerMana.value = gameEngine.playerMana
        _playerMaxMana.value = gameEngine.playerMaxMana
        _turnNumber.value = gameEngine.turnNumber
        _deckCount.value = gameEngine.playerDeck.size

        when {
            gameEngine.gameOver -> {
                _gameState.value = "GAME_OVER"
                _gameMessage.value = if (gameEngine.playerWon) "Victory!" else "Defeat!"
            }
            gameEngine.currentTurn == Turn.PLAYER -> {
                _gameState.value = "READY"
                _gameMessage.value = "Your turn - Turn ${gameEngine.turnNumber}"
            }
            gameEngine.currentTurn == Turn.BOT -> {
                _gameState.value = "BOT_TURN"
                _gameMessage.value = "Enemy turn..."
            }
        }
    }

    fun selectCard(index: Int) {
        if (gameState.value != "READY") return

        if (index < 0) {
            _selectedCard.value = null
            return
        }

        val currentSelection = _selectedCard.value
        if (currentSelection == index) {
            playSelectedCard(null)
        } else {
            _selectedCard.value = index
            _selectedMinion.value = null

            val hand = playerHand.value ?: return
            if (index < hand.size) {
                val card = hand[index]
                if (card is SpellCard && card.requiresTarget()) {
                    _gameMessage.value = "Select a target for ${card.name}"
                } else {
                    _gameMessage.value = "Click ${card.name} again to play it"
                }
            }
        }
    }

    fun playSelectedCard(target: Any?): Boolean {
        val cardIndex = _selectedCard.value ?: return false
        val hand = playerHand.value ?: return false

        if (cardIndex >= hand.size) return false
        val card = hand[cardIndex]

        if (card.manaCost > gameEngine.playerMana) {
            _gameMessage.value = "Not enough mana! Need ${card.manaCost}, have ${gameEngine.playerMana}"
            return false
        }

        if (card is MinionCard && gameEngine.playerBoard.size >= MAX_BOARD_SIZE) {
            _gameMessage.value = "Board is full! (Maximum ${MAX_BOARD_SIZE} minions)"
            return false
        }

        val success = gameEngine.playCardFromHand(cardIndex, target)
        if (success) {
            _selectedCard.value = null
            _gameMessage.value = "${card.name} played!"
            updateGameState()
        } else {
            _gameMessage.value = "Cannot play ${card.name} right now"
        }
        return success
    }

    fun selectMinion(index: Int) {
        if (gameState.value != "READY") return

        if (index < 0) {
            _selectedMinion.value = null
            return
        }

        _selectedMinion.value = if (_selectedMinion.value == index) null else index
        _selectedCard.value = null

        val board = playerBoard.value ?: return
        if (index < board.size) {
            val minion = board[index]
            if (minion.canAttack()) {
                _gameMessage.value = "Select a target to attack with ${minion.name}"
            } else {
                _gameMessage.value = "${minion.name} cannot attack this turn"
            }
        }
    }

    fun attackWithSelectedMinion(target: Any): Boolean {
        val minionIndex = _selectedMinion.value ?: return false
        val board = playerBoard.value ?: return false

        if (minionIndex >= board.size) return false
        val attacker = board[minionIndex]

        val success = gameEngine.attack(attacker, target)
        if (success) {
            _selectedMinion.value = null
            val targetName = when (target) {
                is MinionCard -> target.name
                is Hero -> target.name
                else -> "target"
            }
            _gameMessage.value = "${attacker.name} attacked ${targetName}!"
            updateGameState()
        } else {
            _gameMessage.value = "${attacker.name} cannot attack that target"
        }
        return success
    }

    fun useHeroPower(target: Any? = null): Boolean {
        if (!canUseHeroPower()) return false

        val success = gameEngine.useHeroPower(target)
        if (success) {
            _gameMessage.value = "Hero power used!"
            updateGameState()
        } else {
            _gameMessage.value = "Cannot use hero power!"
        }
        return success
    }

    fun endTurn() {
        if (gameEngine.currentTurn != Turn.PLAYER) return

        _selectedCard.value = null
        _selectedMinion.value = null

        gameEngine.endTurn()

        if (gameEngine.currentTurn == Turn.BOT) {
            _gameState.value = "BOT_TURN"
            viewModelScope.launch {
                gameEngine.processBotTurn()
                updateGameState()
            }
        } else {
            updateGameState()
        }
    }

    fun canPlayCard(position: Int): Boolean {
        val hand = playerHand.value ?: return false
        if (position >= hand.size) return false

        val card = hand[position]
        val hasEnoughMana = card.manaCost <= gameEngine.playerMana
        val boardNotFull = if (card is MinionCard) {
            gameEngine.playerBoard.size < MAX_BOARD_SIZE
        } else true

        return hasEnoughMana && boardNotFull && gameState.value == "READY"
    }

    fun canAttackWithMinion(position: Int): Boolean {
        val board = playerBoard.value ?: return false
        return position < board.size &&
                board[position].canAttack() &&
                gameState.value == "READY"
    }

    fun canUseHeroPower(): Boolean {
        return if (::gameEngine.isInitialized) {
            gameEngine.playerMana >= gameEngine.playerHero.heroPower.cost &&
                    !gameEngine.playerHero.heroPower.usedThisTurn &&
                    gameState.value == "READY"
        } else {
            false
        }
    }

    fun getHandSize(): Int = playerHand.value?.size ?: 0
    fun getMaxHandSize(): Int = MAX_HAND_SIZE
    fun getBoardSize(): Int = playerBoard.value?.size ?: 0
    fun getMaxBoardSize(): Int = MAX_BOARD_SIZE
}
