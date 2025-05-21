// GameSetupFragment.kt
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

    private var selectedDifficulty = "normal"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up difficulty selection
        val difficultyGroup = view.findViewById<RadioGroup>(R.id.difficultyGroup)
        difficultyGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedDifficulty = when (checkedId) {
                R.id.easyRadio -> "easy"
                R.id.hardRadio -> "hard"
                else -> "normal"
            }
        }

        // Start game button
        view.findViewById<Button>(R.id.startGameButton).setOnClickListener {
            val action = GameSetupFragmentDirections
                .actionGameSetupFragmentToGamePlayFragment(selectedDifficulty)
            findNavController().navigate(action)
        }
    }
}
