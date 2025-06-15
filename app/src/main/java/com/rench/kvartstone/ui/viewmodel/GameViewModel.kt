package com.rench.kvartstone.ui.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.rench.kvartstone.R
import com.rench.kvartstone.data.repositories.DeckRepository
import com.rench.kvartstone.data.repositories.HeroPowerRepository
import com.rench.kvartstone.domain.*
import kotlinx.coroutines.launch
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.Lifecycle
import com.rench.kvartstone.notification.NotificationHelper

class GameViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val MAX_HAND_SIZE  = 5
        private const val MAX_BOARD_SIZE = 7
    }

    /* ---------- engine & repos ---------- */

    private var lastPushedTurn: Turn? = null

    private lateinit var engine: ImprovedGameEngine
    private val heroPowerRepo = HeroPowerRepository(app)
    private val deckRepo      = DeckRepository(app)

    /* ---------- LiveData exposed to UI ---------- */

    private val _playerHand   = MutableLiveData<List<Card>>(emptyList())
    val playerHand: LiveData<List<Card>> = _playerHand

    private val _playerBoard  = MutableLiveData<List<MinionCard>>(emptyList())
    val playerBoard: LiveData<List<MinionCard>> = _playerBoard

    private val _botBoard     = MutableLiveData<List<MinionCard>>(emptyList())
    val botBoard: LiveData<List<MinionCard>> = _botBoard

    private val _playerHero   = MutableLiveData<Hero>()
    val playerHero: LiveData<Hero> = _playerHero

    private val _botHero      = MutableLiveData<Hero>()
    val botHero: LiveData<Hero> = _botHero

    private val _playerMana   = MutableLiveData(1)
    val playerMana: LiveData<Int> = _playerMana

    private val _playerMaxMana = MutableLiveData(1)
    val playerMaxMana: LiveData<Int> = _playerMaxMana
    private val _botMana       = MutableLiveData(1)
    val   botMana: LiveData<Int> = _botMana

    private val _botMaxMana    = MutableLiveData(1)
    val   botMaxMana: LiveData<Int> = _botMaxMana
    private val _turnNumber   = MutableLiveData(1)
    val turnNumber: LiveData<Int> = _turnNumber

    private val _deckCount    = MutableLiveData(30)
    val deckCount: LiveData<Int> = _deckCount

    private val _gameState    = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    private val _gameMessage  = MutableLiveData("")
    val gameMessage: LiveData<String> = _gameMessage

    private val _selectedCard   = MutableLiveData<Int?>(null)
    val selectedCard: LiveData<Int?> = _selectedCard

    private val _selectedMinion = MutableLiveData<Int?>(null)
    val selectedMinion: LiveData<Int?> = _selectedMinion

    private val _validAttackTargets = MutableLiveData<List<Any>>(emptyList())
    val validAttackTargets: LiveData<List<Any>> = _validAttackTargets

    /* ---------- initialisation ---------- */


    fun initializeGame(heroPowerId: Int, deckId: Int) = viewModelScope.launch {
        val playerDeck = deckRepo.getDeckById(deckId) ?: return@launch
        val botDeck    = deckRepo.getRandomDeckExcept(deckId) ?: playerDeck

        val playerHero = Hero(
            name      = "Player",
            heroPower = HeroPowerFactory.createHeroPower(heroPowerId)
        )
        val botHero = Hero(
            name      = "Bot",
            heroPower = HeroPowerFactory.createHeroPower(1)
        )

        engine = ImprovedGameEngine(playerDeck.cards, botDeck.cards, playerHero, botHero)
        pushStateToUi()
    }


    /* ---------- public helpers called from the fragment ---------- */

    fun getValidTargetsForSelectedCard(): List<Any> {
        val idx  = _selectedCard.value ?: return emptyList()
        val card = _playerHand.value?.getOrNull(idx) ?: return emptyList()
        return if (card is SpellCard) card.getValidTargets(engine) else emptyList()
    }

    fun selectCard(index: Int?) {
        if (index != null) {
            _selectedMinion.value = null
            _validAttackTargets.value = emptyList()
        }
        _selectedCard.value = if (_selectedCard.value == index) null else index
    }

    fun selectMinion(index: Int?) {

        _validAttackTargets.value = emptyList()


        if (index == null || index < 0) {
            _selectedMinion.value = null
            return
        }
        val board = _playerBoard.value ?: return
        if (index !in board.indices) {
            _selectedMinion.value = null
            return
        }


        _selectedCard.value = null
        _selectedMinion.value =
            if (_selectedMinion.value == index) null else index

        val minion = board[index]
        if (minion.canAttack()) {
            _validAttackTargets.value = getValidTargetsForSelectedMinion()
            _gameMessage.value = "Select a target for ${minion.name}"
        } else {
            _gameMessage.value = "${minion.name} cannot attack."
        }
    }

    fun playSelectedCard(target: Any?): Boolean {
        val idx  = _selectedCard.value ?: return false
        val card = _playerHand.value?.getOrNull(idx) ?: return false

        if (card.manaCost > engine.playerMana) {
            _gameMessage.value = "Not enough mana!"
            return false
        }
        if (card is MinionCard && engine.playerBoard.size >= MAX_BOARD_SIZE) {
            _gameMessage.value = "Board is full!"
            return false
        }

        val success = engine.playCardFromHand(idx, target)
        if (success) {
            _selectedCard.value = null
            _gameMessage.value = "${card.name} played!"
            pushStateToUi()
        }
        return success
    }

    fun attackWithSelectedMinion(target: Any): Boolean {
        val idx = _selectedMinion.value ?: return false
        val board = _playerBoard.value ?: return false
        if (idx !in board.indices) return false

        val attacker = board[idx]
        val success = engine.attack(attacker, target)
        if (success) {
            _selectedMinion.value = null
            val targetName = when (target) {
                is MinionCard -> target.name
                is Hero       -> target.name
                else          -> "target"
            }
            _gameMessage.value = "${attacker.name} attacked $targetName!"
            pushStateToUi()
        }
        return success
    }

    fun useHeroPower(target: Any? = null): Boolean {
        if (!canUseHeroPower()) return false
        val ok = engine.useHeroPower(target)
        if (ok) {
            _gameMessage.value = "Hero power used!"
            pushStateToUi()
        }
        return ok
    }

    fun endTurn() {
        if (!playerReady()) return
        _selectedCard.value = null
        _selectedMinion.value = null
        engine.endTurn()
        pushStateToUi()

        if (engine.currentTurn == Turn.BOT) {
            viewModelScope.launch {
                engine.processBotTurn()
                pushStateToUi()
            }
        }
    }



    fun canPlayCard(pos: Int): Boolean {
        val hand = _playerHand.value ?: return false
        if (pos !in hand.indices) return false
        val card = hand[pos]

        val boardFree = card !is MinionCard || engine.playerBoard.size < MAX_BOARD_SIZE
        val enoughMana = card.manaCost <= engine.playerMana
        return boardFree && enoughMana && playerReady()
    }

    fun canAttackWithMinion(pos: Int): Boolean {
        val board = _playerBoard.value ?: return false
        return pos in board.indices && board[pos].canAttack() && playerReady()
    }

    fun canUseHeroPower(): Boolean {
        if (!::engine.isInitialized) return false
        val heroPower = engine.playerHero.heroPower
        return heroPower.canUse(engine.playerMana)
    }
    /* ---------- hero-power helpers ---------- */

    fun heroPowerRequiresTarget(): Boolean {
        val hp = engine.playerHero.heroPower
        return hp.id == 1
    }

    fun validTargetsForHeroPower(): List<Any> =
        engine.playerBoard + engine.botBoard +
                listOf(engine.playerHero, engine.botHero)
    /* ---------- private helpers ---------- */

    private fun playerReady(): Boolean {
        val gs = _gameState.value ?: return false
        return !gs.gameOver && !gs.isProcessingTurn && gs.currentTurn == Turn.PLAYER
    }

    private fun getValidTargetsForSelectedMinion(): List<Any> {
        val idx   = _selectedMinion.value ?: return emptyList()
        val board = _playerBoard.value  ?: return emptyList()
        val minion = board.getOrNull(idx) ?: return emptyList()
        if (!minion.canAttack()) return emptyList()


        return buildList {
            addAll(engine.botBoard)
            add(engine.botHero)
        }
    }

    private fun pushStateToUi() {
        if (!::engine.isInitialized) return

        val state = engine.getGameState()
        _gameState.value   = state
        _playerHand.value  = engine.playerHand.take(MAX_HAND_SIZE)
        _playerBoard.value = engine.playerBoard.toList()
        _botBoard.value    = engine.botBoard.toList()
        _playerHero.value  = engine.playerHero
        _botHero.value     = engine.botHero
        _playerMana.value  = engine.playerMana
        _playerMaxMana.value = engine.playerMaxMana
        _turnNumber.value  = engine.turnNumber
        _deckCount.value   = engine.playerDeck.size
        _botMana.value        = engine.botMana
        _botMaxMana.value     = engine.botMaxMana

        if (lastPushedTurn == Turn.BOT && state.currentTurn == Turn.PLAYER) {

            val inForeground = ProcessLifecycleOwner.get().lifecycle.currentState
                .isAtLeast(Lifecycle.State.STARTED)
            if (!inForeground) {
                NotificationHelper.showYourTurn(getApplication())
            }
        }
        lastPushedTurn = state.currentTurn

    }



    private fun defaultPlayerHeroPower() = HeroPower(
        id          = 1,
        name        = "Fireblast",
        description = "Deal 1 damage",
        cost        = 2,
        imageResName = R.drawable.ic_hero_power_player.toString(),
        effect = { eng, target ->
            val realTarget = when (target) {
                is List<*> -> target.firstOrNull()
                else       -> target
            } ?: eng.botHero

            when (realTarget) {
                is MinionCard -> realTarget.takeDamage(1)
                is Hero       -> realTarget.takeDamage(1)
            }
        }
    )

    private fun defaultBotHeroPower() = HeroPower(
        id          = 2,
        name        = "Armor Up!",
        description = "Gain 2 armor",
        cost        = 2,
        imageResName = R.drawable.ic_hero_power_bot.toString(),
        effect      = { eng, _ -> eng.botHero.addArmor(2) }
    )

}
