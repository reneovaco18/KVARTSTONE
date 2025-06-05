package com.rench.kvartstone.ui.cardmanagement

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.NumberPicker
import android.widget.RadioGroup
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputEditText
import com.rench.kvartstone.R

class CardCreatorFragment : Fragment(R.layout.card_creator_fragment) {

    private lateinit var manaCostPicker: NumberPicker
    private lateinit var attackPicker: NumberPicker
    private lateinit var healthPicker: NumberPicker
    private lateinit var cardNameInput: TextInputEditText
    private lateinit var cardTypeGroup: RadioGroup
    private lateinit var effectSpinner: Spinner
    private lateinit var saveCardButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupNumberPickers()
        setupListeners()
    }

    private fun initializeViews(view: View) {
        manaCostPicker = view.findViewById(R.id.manaCostPicker)
        attackPicker = view.findViewById(R.id.attackPicker)
        healthPicker = view.findViewById(R.id.healthPicker)
        cardNameInput = view.findViewById(R.id.cardNameInput)
        cardTypeGroup = view.findViewById(R.id.cardTypeGroup)
        effectSpinner = view.findViewById(R.id.effectSpinner)
        saveCardButton = view.findViewById(R.id.saveCardButton)
    }

    private fun setupNumberPickers() {
        // Mana Cost Picker (0-10)
        manaCostPicker.apply {
            minValue = 0
            maxValue = 10
            value = 1
            wrapSelectorWheel = false
        }

        // Attack Picker (0-12)
        attackPicker.apply {
            minValue = 0
            maxValue = 12
            value = 1
            wrapSelectorWheel = false
        }

        // Health Picker (1-12)
        healthPicker.apply {
            minValue = 1
            maxValue = 12
            value = 1
            wrapSelectorWheel = false
        }
    }

    private fun setupListeners() {
        saveCardButton.setOnClickListener {
            saveCard()
        }

        cardTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.minionType -> {
                    // Show minion stats
                    view?.findViewById<View>(R.id.minionStatsGroup)?.visibility = View.VISIBLE
                }
                R.id.spellType -> {
                    // Hide minion stats
                    view?.findViewById<View>(R.id.minionStatsGroup)?.visibility = View.GONE
                }
            }
        }
    }

    private fun saveCard() {
        val cardName = cardNameInput.text.toString()
        val manaCost = manaCostPicker.value
        val attack = if (cardTypeGroup.checkedRadioButtonId == R.id.minionType) attackPicker.value else null
        val health = if (cardTypeGroup.checkedRadioButtonId == R.id.minionType) healthPicker.value else null
        val effectPosition = effectSpinner.selectedItemPosition

        // TODO: Implement card saving logic
        // You can use your CardRepository here to save the card
    }
}
