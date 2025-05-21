// GamePlayFragment.kt
package com.rench.kvartstone.ui.GamePlay

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rench.kvartstone.R
import com.rench.kvartstone.ui.adapters.CardHandAdapter
import com.rench.kvartstone.ui.adapters.MinionBoardAdapter
import com.rench.kvartstone.ui.viewmodel.GameViewModel

class GamePlayFragment : Fragment(R.layout.fragment_game_play) {

    private val viewModel: GameViewModel by viewModels()
    private val args: GamePlayFragmentArgs by navArgs()

    private lateinit var handAdapter: CardHandAdapter
    private lateinit var playerBoardAdapter: MinionBoardAdapter
    private lateinit var botBoardAdapter: MinionBoardAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up adapters
        setupRecyclerViews(view)

        // Set up UI elements
        val playerHeroHealth = view.findViewById<TextView>(R.id.playerHeroHealth)
        val botHeroHealth = view.findViewById<TextView>(R.id.botHeroHealth)
        val playerManaText = view.findViewById<TextView>(R.id.playerManaText)
        val endTurnButton = view.findViewById<Button>(R.id.endTurnButton)
        val gameStatusText = view.findViewById<TextView>(R.id.gameStatusText)

        // Observe ViewModel
        viewModel.playerHand.observe(viewLifecycleOwner) { cards ->
            handAdapter.submitList(cards)
        }

        viewModel.playerBoard.observe(viewLifecycleOwner) { minions ->
            playerBoardAdapter.submitList(minions)
        }

        viewModel.botBoard.observe(viewLifecycleOwner) { minions ->
            botBoardAdapter.submitList(minions)
        }

        viewModel.playerHero.observe(viewLifecycleOwner) { hero ->
            playerHeroHealth.text = "Health: ${hero.currentHealth}/${hero.maxHealth}"
        }

        viewModel.botHero.observe(viewLifecycleOwner) { hero ->
            botHeroHealth.text = "Health: ${hero.currentHealth}/${hero.maxHealth}"
        }

        viewModel.playerMana.observe(viewLifecycleOwner) { mana ->
            playerManaText.text = "Mana: $mana"
        }

        viewModel.gameState.observe(viewLifecycleOwner) { state ->
            when (state) {
                "INITIALIZING" -> {
                    gameStatusText.text = "Game starting..."
                    endTurnButton.isEnabled = false
                }
                "READY" -> {
                    gameStatusText.text = "Your turn"
                    endTurnButton.isEnabled = true
                }
                "BOT_TURN" -> {
                    gameStatusText.text = "Bot's turn..."
                    endTurnButton.isEnabled = false
                }
                "GAME_OVER" -> {
                    val result = if (viewModel.playerHero.value?.isDead == true)
                        "You lost!" else "You won!"
                    gameStatusText.text = "Game Over: $result"
                    endTurnButton.isEnabled = false
                }
            }
        }

        // Setup click handlers
        endTurnButton.setOnClickListener {
            viewModel.endTurn()
        }

        // Initialize game with selected difficulty
        viewModel.initializeGame(args.difficulty)
    }

    private fun setupRecyclerViews(view: View) {
        // Hand
        val handRecyclerView = view.findViewById<RecyclerView>(R.id.playerHandRecyclerView)
        handAdapter = CardHandAdapter { position ->
            viewModel.selectCard(position)
        }
        handRecyclerView.adapter = handAdapter
        handRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )

        // Player board
        val playerBoardView = view.findViewById<RecyclerView>(R.id.playerBoardRecyclerView)
        playerBoardAdapter = MinionBoardAdapter { position ->
            // Minion selected for attack
            // In a full implementation, you'd track the selected minion
            // and then select a target
        }
        playerBoardView.adapter = playerBoardAdapter
        playerBoardView.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )

        // Bot board
        val botBoardView = view.findViewById<RecyclerView>(R.id.botBoardRecyclerView)
        botBoardAdapter = MinionBoardAdapter { position ->
            // Bot minion selected as attack target
            // In a full implementation, you'd check if an attacker was selected
            // and then perform the attack
        }
        botBoardView.adapter = botBoardAdapter
        botBoardView.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )
    }
}
