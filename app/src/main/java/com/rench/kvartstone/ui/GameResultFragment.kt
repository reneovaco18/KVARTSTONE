package com.rench.kvartstone.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.rench.kvartstone.R
import com.rench.kvartstone.databinding.FragmentGameResultBinding

class GameResultFragment : Fragment(R.layout.fragment_game_result) {

    private var _binding: FragmentGameResultBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGameResultBinding.bind(view)

        val args = GameResultFragmentArgs.fromBundle(requireArguments())
        binding.resultText.text = if (args.playerWon) "Victory!" else "Defeat!"

        binding.backToMenuButton.setOnClickListener {
            val action =
                GameResultFragmentDirections.actionGameResultFragmentToMainMenuFragment()
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
