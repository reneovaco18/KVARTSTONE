package com.rench.kvartstone.ui.MainMenu

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rench.kvartstone.R
import com.rench.kvartstone.data.repositories.CardRepository
import com.rench.kvartstone.data.repositories.DeckRepository
import com.rench.kvartstone.data.repositories.HeroPowerRepository
import kotlinx.coroutines.launch

class MainMenuFragment : Fragment(R.layout.fragment_main_menu) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnPlay).setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_heroPowerSelectionFragment)
        }

        view.findViewById<Button>(R.id.btnDeckManager).setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_cardManagementFragment)
        }
        view.findViewById<Button>(R.id.btnDeckBuilder).setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_deckBuilder)
        }

        // Initialize default data if needed
        lifecycleScope.launch {
            initializeDefaultData()
        }
    }

    private suspend fun initializeDefaultData() {
        val heroPowerRepo = HeroPowerRepository(requireContext())
        val deckRepo = DeckRepository(requireContext())
        val cardRepo = CardRepository(requireContext())

        // Initialize default content
        heroPowerRepo.initializeDefaultHeroPowers()
        deckRepo.initializeDefaultDecks()
        cardRepo.initializeDefaultCards()
    }}