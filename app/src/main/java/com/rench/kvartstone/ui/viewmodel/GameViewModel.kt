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

    fun initializeGame(heroPowerId: Int, deckId: Int) {
        viewModelScope.launch {
            val heroPowerRepo = HeroPowerRepository(getApplication())
            val deckRepo = DeckRepository(getApplication())

            val selectedHeroPower = heroPowerRepo.getHeroPowerById(heroPowerId)
            val selectedDeck = deckRepo.getDeckById(deckId)

            val playerHero = Hero(
                name = "Player",
                maxHealth = 30,
                imageRes = R.drawable.hero_mage_icon, // Change to: R.drawable.hero_mage_icon
                heroPower = selectedHeroPower,
                heroPowerImageRes = selectedHeroPower.imageRes
            )

            val botHero = Hero(
                name = "Bot",
                maxHealth = 30,
                imageRes = R.drawable.hero_frame,
                heroPower = createDefaultBotHeroPower(),
                heroPowerImageRes = R.drawable.hero_power_fire
            )

            gameEngine = ImprovedGameEngine(
                selectedDeck.cards,
                createDefaultBotDeck(),
                playerHero,
                botHero
            )

            updateGameState()
        }
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
                    imageRes = R.drawable.card_icon_dragon,
                    attack = (index % 3) + 1,
                    maxHealth = (index % 4) + 1
                )
                else -> SpellCard(
                    id = index,
                    name = "Spell $index",
                    manaCost = (index % 3) + 1,
                    imageRes = R.drawable.card_icon_dragon,
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
        if (gameEngine.currentTurn == Turn.PLAYER) {
            gameEngine.endTurn()
            // Process bot turn
            viewModelScope.launch {
                gameEngine.processBotTurn()
                updateGameState()
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
