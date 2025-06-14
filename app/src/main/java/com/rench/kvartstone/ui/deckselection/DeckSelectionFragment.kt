package com.rench.kvartstone.ui.deckselection

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rench.kvartstone.R
import com.rench.kvartstone.domain.Deck
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeckSelectionFragment : Fragment(R.layout.fragment_deck_selection) {

    private val viewModel: DeckSelectionViewModel by viewModels()
    private lateinit var deckAdapter: DeckSelectionAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var titleText: TextView
    private lateinit var confirmButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val heroPowerId = arguments?.getInt("selectedHeroPowerId", 1) ?: 1

        initializeViews(view)
        setupRecyclerView()
        observeViewModel()
        setupClickListeners(heroPowerId)

        viewModel.loadAvailableDecks()
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.deckRecyclerView)
        titleText = view.findViewById(R.id.titleText)
        confirmButton = view.findViewById(R.id.confirmButton)
    }

    private fun setupRecyclerView() {
        deckAdapter = DeckSelectionAdapter(
            onDeckSelected = { deck -> viewModel.selectDeck(deck) },
            onDeckLongHeld = { deck -> askDelete(deck) }
        )


        recyclerView.apply {
            adapter = deckAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewModel.decks.observe(viewLifecycleOwner) { decks ->
            deckAdapter.submitList(decks)
        }

        viewModel.selectedDeck.observe(viewLifecycleOwner) { selectedDeck ->
            confirmButton.isEnabled = selectedDeck != null
            deckAdapter.setSelectedDeck(selectedDeck)
        }
    }
    private fun askDelete(deck: Deck) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_deck_title))
            .setMessage(getString(R.string.delete_deck_msg, deck.name))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteDeck(deck)
            }
            .show()
    }

    private fun setupClickListeners(heroPowerId: Int) {
        confirmButton.setOnClickListener {
            viewModel.selectedDeck.value?.let { deck ->
                val bundle = Bundle().apply {
                    putInt("heroPowerId", heroPowerId)
                    putInt("deckId", deck.id)
                    putInt("difficulty", 2)
                }

                findNavController().navigate(R.id.action_deckSelection_to_gamePlayFragment, bundle)
            }
        }
    }
}
