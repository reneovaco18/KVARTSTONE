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

    private lateinit var gameEngine: GameEngineInterface

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

    fun initializeGame(heroPowerId: Int, deckId: Int) {
        viewModelScope.launch {
            try {
                val selectedHeroPower = createDefaultPlayerHeroPower() // Fallback
                val selectedDeck = createDefaultDeck() // Fallback

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

                gameEngine = GameEngine(
                    selectedDeck.cards,
                    createDefaultBotDeck().cards,
                    playerHero,
                    botHero
                )

                updateGameState()
                _gameState.value = "READY"
            } catch (e: Exception) {
                _gameState.value = "ERROR"
                _gameMessage.value = "Failed to initialize game: ${e.message}"
            }
        }
    }

    private fun createDefaultPlayerHeroPower(): HeroPower {
        return HeroPower(
            id = 1,
            name = "Fireblast",
            description = "Deal 1 damage",
            cost = 2,
            imageRes = R.drawable.ic_hero_power_player,
            effect = { engine, _ ->
                engine.botHero.takeDamage(1)
            }
        )
    }

    private fun createDefaultBotHeroPower(): HeroPower {
        return HeroPower(
            id = 2,
            name = "Armor Up",
            description = "Gain 2 armor",
            cost = 2,
            imageRes = R.drawable.ic_hero_power_bot,
            effect = { engine, _ ->
                engine.botHero.addArmor(2)
            }
        )
    }

    private fun createDefaultDeck(): Deck {
        val cards = createSampleDeck()
        return Deck(
            id = 1,
            name = "Default Deck",
            description = "A basic starter deck",
            cards = cards
        )
    }

    private fun createDefaultBotDeck(): Deck {
        val cards = createSampleDeck()
        return Deck(
            id = 2,
            name = "Bot Deck",
            description = "Bot's deck",
            cards = cards
        )
    }

    private fun createSampleDeck(): List<Card> {
        return List(15) { index ->
            when {
                index < 10 -> MinionCard(
                    id = index,
                    name = "Minion $index",
                    manaCost = (index % 5) + 1,
                    imageRes = R.drawable.ic_card_minion_generic,
                    attack = (index % 3) + 1,
                    maxHealth = (index % 4) + 1
                )
                else -> SpellCard(
                    id = index,
                    name = "Spell $index",
                    manaCost = (index % 3) + 1,
                    imageRes = R.drawable.ic_card_spell_generic,
                    effect = { engine, targets ->
                        targets.filterIsInstance<MinionCard>().firstOrNull()?.takeDamage(2)
                    }
                )
            }
        }.shuffled()
    }

    fun getValidTargetsForSelectedCard(): List<Any> {
        val selectedCardIndex = selectedCard.value
        val hand = playerHand.value ?: return emptyList()
        if (selectedCardIndex == null || selectedCardIndex !in hand.indices) return emptyList()
        val card = hand[selectedCardIndex]
        return when (card) {
            is SpellCard -> card.getValidTargets(gameEngine)
            else -> emptyList()
        }
    }

    private fun updateGameState() {
        _playerHand.value = gameEngine.playerHand.toList()
        _playerBoard.value = gameEngine.playerBoard.toList()
        _botBoard.value = gameEngine.botBoard.toList()
        _playerHero.value = gameEngine.playerHero
        _botHero.value = gameEngine.botHero
        _playerMana.value = gameEngine.playerMana
        _playerMaxMana.value = gameEngine.playerMaxMana
        _turnNumber.value = gameEngine.turnNumber

        if (gameEngine.gameOver) {
            _gameState.value = "GAME_OVER"
            _gameMessage.value = if (gameEngine.playerWon) "Victory!" else "Defeat!"
        }
    }

    // Player Actions
    fun selectCard(index: Int) {
        if (gameState.value == "READY") {
            _selectedCard.value = if (_selectedCard.value == index) null else index
        }
    }

    fun playSelectedCard(target: Any? = null): Boolean {
        return _selectedCard.value?.let { index ->
            if (gameEngine.playCardFromHand(index, target)) {
                _selectedCard.value = null
                updateGameState()
                true
            } else {
                false
            }
        } ?: false
    }

    fun selectMinion(index: Int) {
        if (gameState.value == "READY") {
            _selectedMinion.value = if (_selectedMinion.value == index) null else index
        }
    }

    fun attackWithSelectedMinion(target: Any): Boolean {
        return _selectedMinion.value?.let { attackerIndex ->
            if (gameEngine.playerBoard.size > attackerIndex) {
                val attacker = gameEngine.playerBoard[attackerIndex]
                // Perform attack logic here
                val success = gameEngine.attack(attacker, target)
                if (success) {
                    _selectedMinion.value = null
                    updateGameState()
                }
                success
            } else {
                false
            }
        } ?: false
    }

    fun useHeroPower(): Boolean {
        return if (canUseHeroPower()) {
            val success = gameEngine.useHeroPower(null)
            if (success) {
                updateGameState()
            }
            success
        } else {
            false
        }
    }

    fun endTurn() {
        if (gameEngine.currentTurn == Turn.PLAYER) {
            gameEngine.endTurn()
            viewModelScope.launch {
                // Process bot turn if using ImprovedGameEngine
                if (gameEngine is ImprovedGameEngine) {
                    (gameEngine as ImprovedGameEngine).processBotTurn()
                }
                updateGameState()
            }
        }
    }

    // Helper methods
    fun canPlayCard(position: Int): Boolean {
        return gameEngine.playerHand.getOrNull(position)?.manaCost ?: 0 <= gameEngine.playerMana
    }

    fun canAttackWithMinion(position: Int): Boolean {
        return gameEngine.playerBoard.getOrNull(position)?.canAttack() ?: false
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

    fun getValidTargets(): List<Any> {
        return when (val card = _selectedCard.value?.let { gameEngine.playerHand.getOrNull(it) }) {
            is SpellCard -> card.getValidTargets(gameEngine)
            else -> emptyList()
        }
    }
}
