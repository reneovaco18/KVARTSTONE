package com.rench.kvartstone.ui.GameSetup

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rench.kvartstone.R

class GameSetupFragment : Fragment(R.layout.fragment_game_setup) {

    private var selectedDifficulty = 1 // Use Int instead of String
    private var selectedHeroPowerId = 1
    private var selectedDeckId = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get arguments if passed from previous fragment
        arguments?.let {
            selectedHeroPowerId = it.getInt("selectedHeroPowerId", 1)
            selectedDeckId = it.getInt("selectedDeckId", 1)
        }

        // Set up difficulty selection
        val difficultyGroup = view.findViewById<RadioGroup>(R.id.difficultyGroup)
        difficultyGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedDifficulty = when (checkedId) {
                R.id.easyRadio -> 1
                R.id.hardRadio -> 3
                else -> 2 // normal
            }
        }

        // Start game button
        view.findViewById<Button>(R.id.startGameButton).setOnClickListener {
            val bundle = Bundle().apply {
                putInt("heroPowerId", selectedHeroPowerId)
                putInt("deckId", selectedDeckId)
                putInt("difficulty", selectedDifficulty)
            }
            findNavController().navigate(R.id.action_gameSetupFragment_to_gamePlayFragment, bundle)
        }
    }
}
