package com.rench.kvartstone.ui.GamePlay

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rench.kvartstone.R
import com.rench.kvartstone.domain.MinionCard
import com.rench.kvartstone.domain.Hero
import com.rench.kvartstone.domain.SpellCard
import com.rench.kvartstone.ui.adapters.CardHandAdapter
import com.rench.kvartstone.ui.adapters.MinionBoardAdapter
import com.rench.kvartstone.ui.viewmodel.GameViewModel

class GamePlayFragment : Fragment(R.layout.fragment_game_play) {

    private val viewModel: GameViewModel by viewModels()
    private val args: GamePlayFragmentArgs by navArgs()

    private lateinit var handAdapter: CardHandAdapter
    private lateinit var playerBoardAdapter: MinionBoardAdapter
    private lateinit var botBoardAdapter: MinionBoardAdapter
    private lateinit var handToggleButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var playerHandArea: androidx.cardview.widget.CardView
    private var isHandVisible = false
    // UI Elements
    private lateinit var playerHeroHealth: TextView
    private lateinit var botHeroHealth: TextView
    private lateinit var playerManaText: TextView
    private lateinit var endTurnButton: Button
    private lateinit var gameStatusText: TextView
    private lateinit var heroPowerButton: Button
    private lateinit var turnNumberText: TextView

    // State for targeting
    private var awaitingTarget = false
    private var validTargets = listOf<Any>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRecyclerViews(view)
        observeViewModel()
        setupClickListeners()

        // Initialize game with proper parameters
        val heroPowerId = args.heroPowerId ?: 1 // Default to 1 if not provided
        val deckId = args.deckId ?: 1 // Default to 1 if not provided

        viewModel.initializeGame(heroPowerId, deckId)
        // ADD THIS LINE:
        initializeHandPosition()
    }

    private fun initializeViews(view: View) {
        playerHeroHealth = view.findViewById(R.id.playerHeroHealth)
        botHeroHealth = view.findViewById(R.id.botHeroHealth)
        playerManaText = view.findViewById(R.id.playerManaText)
        endTurnButton = view.findViewById(R.id.endTurnButton)
        gameStatusText = view.findViewById(R.id.gameStatusText)
        heroPowerButton = view.findViewById(R.id.heroPowerButton)
        turnNumberText = view.findViewById(R.id.turnNumberText)
        handToggleButton = view.findViewById(R.id.handToggleButton)
        playerHandArea = view.findViewById(R.id.playerHandArea)
    }

    private fun setupRecyclerViews(view: View) {
        // Hand adapter with enhanced functionality
        val handRecyclerView = view.findViewById<RecyclerView>(R.id.playerHandRecyclerView)
        handAdapter = CardHandAdapter(
            onCardClick = { position -> handleCardSelection(position) },
            canPlayCard = { position -> viewModel.canPlayCard(position) },
            isCardSelected = { position -> viewModel.selectedCard.value == position }
        )
        handRecyclerView.adapter = handAdapter
        handRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )

        // Player board adapter
        val playerBoardView = view.findViewById<RecyclerView>(R.id.playerBoardRecyclerView)
        playerBoardAdapter = MinionBoardAdapter(
            onMinionClick = { position -> handleMinionSelection(position) },
            canAttackWithMinion = { position -> viewModel.canAttackWithMinion(position) },
            isMinionSelected = { position -> viewModel.selectedMinion.value == position },
            isPlayerBoard = true
        )
        playerBoardView.adapter = playerBoardAdapter
        playerBoardView.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )

        // Bot board adapter (for targeting)
        val botBoardView = view.findViewById<RecyclerView>(R.id.botBoardRecyclerView)
        botBoardAdapter = MinionBoardAdapter(
            onMinionClick = { position -> handleTargetSelection(position, true) },
            isPlayerBoard = false
        )
        botBoardView.adapter = botBoardAdapter
        botBoardView.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )
    }

    private fun observeViewModel() {
        // Observe game state changes
        viewModel.playerHand.observe(viewLifecycleOwner) { cards ->
            handAdapter.submitList(cards) {
                handAdapter.notifyDataSetChanged() // Force update for selection states
            }
        }

        viewModel.playerBoard.observe(viewLifecycleOwner) { minions ->
            playerBoardAdapter.submitList(minions) {
                playerBoardAdapter.notifyDataSetChanged()
            }
        }

        viewModel.botBoard.observe(viewLifecycleOwner) { minions ->
            botBoardAdapter.submitList(minions) {
                botBoardAdapter.notifyDataSetChanged()
            }
        }

        viewModel.playerHero.observe(viewLifecycleOwner) { hero ->
            updateHeroDisplay(hero, playerHeroHealth, true)
            // Update hero power button when hero changes
            updateHeroPowerButton()
        }

        viewModel.botHero.observe(viewLifecycleOwner) { hero ->
            updateHeroDisplay(hero, botHeroHealth, false)

            // Make bot hero clickable for targeting
            botHeroHealth.setOnClickListener {
                if (awaitingTarget && validTargets.contains(hero)) {
                    handleHeroTargeting(hero)
                }
            }
        }

        viewModel.playerMana.observe(viewLifecycleOwner) { mana ->
            val maxMana = viewModel.playerMaxMana.value ?: 1
            playerManaText.text = "Mana: $mana/$maxMana"
            // Update hero power button when mana changes
            updateHeroPowerButton()
        }

        viewModel.playerMaxMana.observe(viewLifecycleOwner) { maxMana ->
            val currentMana = viewModel.playerMana.value ?: 0
            playerManaText.text = "Mana: $currentMana/$maxMana"
            // Update hero power button when max mana changes
            updateHeroPowerButton()
        }

        viewModel.turnNumber.observe(viewLifecycleOwner) { turn ->
            turnNumberText.text = "Turn: $turn"
        }

        viewModel.gameState.observe(viewLifecycleOwner) { state ->
            updateGameState(state)
        }

        viewModel.gameMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                gameStatusText.text = message
                // Auto-clear message after a delay
                view?.postDelayed({
                    if (gameStatusText.text == message) {
                        gameStatusText.text = getStateMessage(viewModel.gameState.value ?: "")
                    }
                }, 3000)
            }
        }

        // Update adapters when selection changes
        viewModel.selectedCard.observe(viewLifecycleOwner) {
            handAdapter.notifyDataSetChanged()
            updateTargetingMode()
        }

        viewModel.selectedMinion.observe(viewLifecycleOwner) {
            playerBoardAdapter.notifyDataSetChanged()
            updateTargetingMode()
        }
    }
    private fun initializeHandPosition() {
        // Set initial position for smooth animation
        playerHandArea.translationY = playerHandArea.height.toFloat()
        playerHandArea.alpha = 0f
    }
    private fun toggleHandVisibility() {
        isHandVisible = !isHandVisible

        if (isHandVisible) {
            // Show hand with slide up animation
            playerHandArea.visibility = View.VISIBLE
            playerHandArea.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .start()

            // Update button icon (rotate arrow up)
            handToggleButton.animate()
                .rotation(180f)
                .setDuration(300)
                .start()
        } else {
            // Hide hand with slide down animation
            playerHandArea.animate()
                .translationY(playerHandArea.height.toFloat())
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    playerHandArea.visibility = View.GONE
                }
                .start()

            // Update button icon (rotate arrow down)
            handToggleButton.animate()
                .rotation(0f)
                .setDuration(300)
                .start()
        }
    }
    private fun updateHeroPowerButton() {
        val canUse = viewModel.canUseHeroPower()
        val gameReady = viewModel.gameState.value == "READY"

        heroPowerButton.isEnabled = canUse && gameReady
        heroPowerButton.alpha = if (heroPowerButton.isEnabled) 1.0f else 0.5f
    }

    private fun setupClickListeners() {
        endTurnButton.setOnClickListener {
            viewModel.endTurn()
            clearSelections()
        }

        heroPowerButton.setOnClickListener {
            if (viewModel.canUseHeroPower()) {
                viewModel.useHeroPower()
                updateHeroPowerButton()
            } else {
                showMessage("Cannot use hero power!")
            }
        }

        // Make player hero clickable for self-targeting
        playerHeroHealth.setOnClickListener {
            val hero = viewModel.playerHero.value
            if (awaitingTarget && hero != null && validTargets.contains(hero)) {
                handleHeroTargeting(hero)
            }
        }

        // ADD THIS BLOCK:
        handToggleButton.setOnClickListener {
            toggleHandVisibility()
        }
    }

    private fun handleCardSelection(position: Int) {
        if (awaitingTarget) {
            clearTargeting()
            return
        }

        val currentSelection = viewModel.selectedCard.value
        if (currentSelection == position) {
            // Same card clicked again - try to play it
            val card = viewModel.playerHand.value?.getOrNull(position)
            if (card is SpellCard && card.requiresTarget()) {
                // Spell needs target - enter targeting mode
                updateTargetingMode()
            } else {
                // Play the card immediately (minions or non-targeting spells)
                viewModel.playSelectedCard(null)
                clearSelections()
            }
        } else {
            // Different card clicked - select it
            viewModel.selectCard(position)
        }
    }


    private fun handleMinionSelection(position: Int) {
        if (awaitingTarget) {
            clearTargeting()
            return
        }

        val selectedCard = viewModel.selectedCard.value
        if (selectedCard != null) {
            // Playing a card on a minion target
            val minions = viewModel.playerBoard.value ?: return
            if (position < minions.size) {
                viewModel.playSelectedCard(minions[position])
                clearSelections()
            }
        } else {
            // Selecting minion for attack
            viewModel.selectMinion(position)
        }
    }

    private fun handleTargetSelection(position: Int, isBotMinion: Boolean) {
        val selectedCard = viewModel.selectedCard.value
        val selectedMinion = viewModel.selectedMinion.value

        when {
            selectedCard != null && awaitingTarget -> {
                // Playing card on target
                val targets = if (isBotMinion) viewModel.botBoard.value else viewModel.playerBoard.value
                if (targets != null && position < targets.size) {
                    viewModel.playSelectedCard(targets[position])
                    clearSelections()
                }
            }
            selectedMinion != null -> {
                // Attacking with minion
                val targets = viewModel.botBoard.value ?: return
                if (position < targets.size) {
                    viewModel.attackWithSelectedMinion(targets[position])
                    clearSelections()
                }
            }
            awaitingTarget -> {
                clearTargeting()
            }
        }
    }

    private fun handleHeroTargeting(hero: Hero) {
        val selectedCard = viewModel.selectedCard.value
        val selectedMinion = viewModel.selectedMinion.value

        when {
            selectedCard != null -> {
                viewModel.playSelectedCard(hero)
                clearSelections()
            }
            selectedMinion != null -> {
                viewModel.attackWithSelectedMinion(hero)
                clearSelections()
            }
            else -> clearTargeting()
        }
    }

    private fun updateTargetingMode() {
        awaitingTarget = false
        validTargets = emptyList()

        val selectedCard = viewModel.selectedCard.value
        if (selectedCard != null) {
            validTargets = viewModel.getValidTargetsForSelectedCard()
            awaitingTarget = validTargets.isNotEmpty()
        }

        // Update UI to show targeting mode
        if (awaitingTarget) {
            gameStatusText.text = "Select a target..."
        }
    }

    private fun updateHeroDisplay(hero: Hero, textView: TextView, isPlayer: Boolean) {
        val healthText = if (hero.armor > 0) {
            "Health: ${hero.currentHealth}/${hero.maxHealth} (+${hero.armor} armor)"
        } else {
            "Health: ${hero.currentHealth}/${hero.maxHealth}"
        }
        textView.text = healthText

        // Color code health
        val healthPercentage = hero.currentHealth.toFloat() / hero.maxHealth
        val color = when {
            healthPercentage > 0.6f -> R.color.hero_health_high
            healthPercentage > 0.3f -> R.color.hero_health_medium
            else -> R.color.hero_health_low
        }
        textView.setTextColor(requireContext().getColor(color))
    }

    private fun updateGameState(state: String) {
        when (state) {
            "INITIALIZING" -> {
                gameStatusText.text = "Initializing game..."
                endTurnButton.isEnabled = false
                heroPowerButton.isEnabled = false
            }
            "READY" -> {
                gameStatusText.text = getStateMessage(state)
                endTurnButton.isEnabled = true
                updateHeroPowerButton()
            }
            "BOT_TURN" -> {
                gameStatusText.text = "Bot's turn..."
                endTurnButton.isEnabled = false
                heroPowerButton.isEnabled = false
                clearSelections()
            }
            "GAME_OVER" -> {
                endTurnButton.isEnabled = false
                heroPowerButton.isEnabled = false
                clearSelections()
            }
            "ERROR" -> {
                endTurnButton.isEnabled = false
                heroPowerButton.isEnabled = false
            }
        }
    }

    private fun getStateMessage(state: String): String {
        return when (state) {
            "READY" -> "Your turn - Turn ${viewModel.turnNumber.value ?: 1}"
            "BOT_TURN" -> "Bot's turn..."
            "GAME_OVER" -> viewModel.gameMessage.value ?: "Game Over"
            else -> "Game in progress..."
        }
    }

    private fun clearSelections() {
        viewModel.selectCard(-1) // Deselect card
        viewModel.selectMinion(-1) // Deselect minion
        clearTargeting()
    }

    private fun clearTargeting() {
        awaitingTarget = false
        validTargets = emptyList()
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
