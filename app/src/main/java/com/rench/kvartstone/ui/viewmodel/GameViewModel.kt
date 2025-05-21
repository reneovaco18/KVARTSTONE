package com.rench.kvartstone.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rench.kvartstone.R
import com.rench.kvartstone.domain.Card
import com.rench.kvartstone.domain.GameEngine
import com.rench.kvartstone.domain.Hero
import com.rench.kvartstone.domain.MinionCard
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

    private val _gameState = MutableLiveData("INITIALIZING") // READY, BOT_TURN, GAME_OVER
    val gameState: LiveData<String> = _gameState

    private val _selectedCard = MutableLiveData<Int?>(null)
    val selectedCard: LiveData<Int?> = _selectedCard

    fun initializeGame(difficulty: String) {
        // Create sample deck for now
        val playerDeck = createSampleDeck()
        val botDeck = createSampleDeck()

        // Create heroes
        val playerHeroObj = Hero(
            name = "Player",
            maxHealth = 20,
            imageRes = R.drawable.ic_hero_player,
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
            heroPowerImageRes = R.drawable.ic_hero_power_bot
        )

        // Initialize game engine
        gameEngine = GameEngine(playerDeck, botDeck, playerHeroObj, botHeroObj)

        // Update LiveData
        updateAllGameState()

        _gameState.value = "READY"
    }

    private fun createSampleDeck(): List<Card> {
        val deck = mutableListOf<Card>()

        // Add some minions
        repeat(10) {
            deck.add(MinionCard(
                id = 100 + it,
                name = "Minion $it",
                manaCost = (it % 5) + 1,
                imageRes = R.drawable.ic_card_minion_generic,
                attack = (it % 3) + 1,
                health = (it % 4) + 1
            ))
        }

        // Add some spells - simplified for now
        repeat(5) {
            deck.add(SpellCard(
                id = 200 + it,
                name = "Spell $it",
                manaCost = (it % 3) + 1,
                imageRes = R.drawable.ic_card_spell_generic,
                effect = { engine, targets ->
                    // Simple effect - damage first target
                    if (targets.isNotEmpty()) {
                        when (val target = targets[0]) {
                            is MinionCard -> target.takeDamage(2)
                            is Hero -> target.takeDamage(2)
                        }
                    }
                }
            ))
        }

        return deck.shuffled()
    }

    private fun updateAllGameState() {
        _playerHand.value = gameEngine.playerHand.toList()
        _playerBoard.value = gameEngine.playerBoard.toList()
        _botBoard.value = gameEngine.botBoard.toList()
        _playerHero.value = gameEngine.playerHero
        _botHero.value = gameEngine.botHero
        _playerMana.value = gameEngine.playerMana

        if (gameEngine.gameOver) {
            _gameState.value = "GAME_OVER"
        }
    }

    fun selectCard(index: Int) {
        if (gameState.value != "READY") return

        _selectedCard.value = if (_selectedCard.value == index) null else index
    }

    fun playSelectedCard(target: Any? = null) {
        if (gameState.value != "READY") return

        val cardIndex = _selectedCard.value ?: return

        if (gameEngine.playCardFromHand(cardIndex, target)) {
            _selectedCard.value = null
            updateAllGameState()
        }
    }

    fun attackWithMinion(attackerIndex: Int, targetType: String, targetIndex: Int = -1) {
        if (gameState.value != "READY") return

        if (gameEngine.playerMinionAttack(attackerIndex, targetType, targetIndex)) {
            updateAllGameState()
        }
    }

    fun endTurn() {
        if (gameState.value != "READY") return

        _gameState.value = "BOT_TURN"

        viewModelScope.launch {
            // Add a small delay to make bot turn visible
            delay(1000)

            gameEngine.endTurn()
            updateAllGameState()

            _gameState.value = "READY"
        }
    }
}