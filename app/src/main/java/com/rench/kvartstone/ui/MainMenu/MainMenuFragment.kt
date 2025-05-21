package com.rench.kvartstone.ui.MainMenu

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rench.kvartstone.R

class MainMenuFragment : Fragment(R.layout.fragment_main_menu) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnPlay).setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_gameSetupFragment)
        }

        view.findViewById<Button>(R.id.btnDeckManager).setOnClickListener {
            findNavController().navigate(R.id.action_mainMenuFragment_to_deckListFragment)
        }
    }
}
