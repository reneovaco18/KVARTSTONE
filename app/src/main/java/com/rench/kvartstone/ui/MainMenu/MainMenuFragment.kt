package com.rench.kvartstone.ui.MainMenu
import com.rench.kvartstone.receiver.DailyReminderReceiver
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.rench.kvartstone.R
import com.rench.kvartstone.core.Constants
import com.rench.kvartstone.data.repositories.*
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


        view.findViewById<Button>(R.id.btnTestReminder).setOnClickListener {
            val intent = Intent(requireContext(), DailyReminderReceiver::class.java).apply {
                action = Constants.ALARM_ACTION
            }
            requireContext().sendBroadcast(intent)
        }



        lifecycleScope.launch { initializeDefaultData() }
    }

    private suspend fun initializeDefaultData() {
        val heroPowerRepo = HeroPowerRepository(requireContext())
        val deckRepo      = DeckRepository(requireContext())
        val cardRepo      = CardRepository(requireContext())

        heroPowerRepo.initializeDefaultHeroPowers()
        deckRepo.initializeDefaultDecks()
        cardRepo.initializeDefaultCards()
    }
}
