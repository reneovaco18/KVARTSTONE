package com.rench.kvartstone.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rench.kvartstone.R
import com.rench.kvartstone.domain.Card
import com.rench.kvartstone.domain.GameEngine
import com.rench.kvartstone.domain.Hero
import com.rench.kvartstone.domain.HeroPower
import com.rench.kvartstone.domain.MinionCard
import com.rench.kvartstone.domain.SpellCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {
    // Game state
    private lateinit var gameEngine: GameEngine

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

    fun initializeGame(difficulty: String) {
        // Create hero powers
        val playerPower = HeroPower(
            name = "Fireblast",
            cost = 2,
            imageRes = R.drawable.ic_hero_power_player,
            effect = { engine, _ -> engine.botHero.takeDamage(1) }
        )

        val botPower = HeroPower(
            name = "Armor Up",
            cost = 2,
            imageRes = R.drawable.ic_hero_power_bot,
            effect = { engine, _ -> engine.botHero.armor += 2 }
        )

        // Create heroes with hero powers
        val playerHeroObj = Hero(
            name = "Player",
            maxHealth = 20,
            imageRes = R.drawable.ic_hero_player,
            heroPower = playerPower,
            heroPowerImageRes = R.drawable.ic_hero_power_player
        )

        val botHeroObj = Hero(
            name = "Bot",
            maxHealth = when(difficulty) {
                "easy" -> 15
                "hard" -> 25
                else -> 20
            },
            imageRes = R.drawable.ic_hero_bot,
            heroPower = botPower,
            heroPowerImageRes = R.drawable.ic_hero_power_bot
        )

        // Initialize game engine with sample decks
        gameEngine = GameEngine(
            createSampleDeck(),
            createSampleDeck(),
            playerHeroObj,
            botHeroObj
        )

        updateGameState()
        _gameState.value = "READY"
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

    // Region: Player Actions
    fun selectCard(index: Int) {
        if (gameState.value == "READY") {
            _selectedCard.value = if (_selectedCard.value == index) null else index
        }
    }

    fun playSelectedCard(target: Any? = null) {
        _selectedCard.value?.let { index ->
            if (gameEngine.playCardFromHand(index, target)) {
                _selectedCard.value = null
                updateGameState()
            }
        }
    }

    fun selectMinion(index: Int) {
        if (gameState.value == "READY") {
            _selectedMinion.value = if (_selectedMinion.value == index) null else index
        }
    }

    fun attackWithSelectedMinion(target: Any) {
        _selectedMinion.value?.let { attackerIndex ->
            if (gameEngine.attack(
                    gameEngine.playerBoard[attackerIndex],
                    target
                )) {
                _selectedMinion.value = null
                updateGameState()
            }
        }
    }

    fun useHeroPower() {
        if (canUseHeroPower()) {
            gameEngine.playerHero.heroPower.effect(gameEngine, null)
            gameEngine.playerMana -= gameEngine.playerHero.heroPower.cost
            updateGameState()
        }
    }

    fun endTurn() {
        if (gameState.value == "READY") {
            _gameState.value = "BOT_TURN"
            viewModelScope.launch {
                delay(1000)
                gameEngine.endTurn()
                updateGameState()
                _gameState.value = "READY"
            }
        }
    }

    // Region: Helper methods
    fun canPlayCard(position: Int): Boolean {
        return gameEngine.playerHand.getOrNull(position)?.manaCost ?: 0 <= gameEngine.playerMana
    }

    fun canAttackWithMinion(position: Int): Boolean {
        return gameEngine.playerBoard.getOrNull(position)?.canAttack() ?: false
    }

    fun canUseHeroPower(): Boolean {
        return gameEngine.playerMana >= gameEngine.playerHero.heroPower.cost &&
                !gameEngine.playerHero.heroPower.usedThisTurn
    }

    fun getValidTargets(): List<Any> {
        return when (val card = _selectedCard.value?.let { gameEngine.playerHand.getOrNull(it) }) {
            is SpellCard -> card.getValidTargets(gameEngine)
            else -> emptyList()
        }
    }
}
