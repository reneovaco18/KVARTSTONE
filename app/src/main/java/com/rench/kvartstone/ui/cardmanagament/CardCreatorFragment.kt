package com.rench.kvartstone.ui.cardmanagement

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.android.material.textfield.TextInputEditText
import com.rench.kvartstone.R
import com.rench.kvartstone.KvartstoneApplication
import com.rench.kvartstone.data.entities.CardEntity
import com.rench.kvartstone.permission.ImagePermissionHelper
import com.rench.kvartstone.utils.ImageStorageManager
import com.rench.kvartstone.domain.SpellEffect
import kotlinx.coroutines.launch

class CardCreatorFragment : Fragment(R.layout.card_creator_fragment) {

    private lateinit var manaCostPicker: NumberPicker
    private lateinit var attackPicker: NumberPicker
    private lateinit var healthPicker: NumberPicker
    private lateinit var cardNameInput: TextInputEditText
    private lateinit var cardTypeGroup: RadioGroup
    private lateinit var effectSpinner: Spinner
    private lateinit var saveCardButton: Button
    private lateinit var cardImagePreview: ImageView
    private lateinit var selectImageButton: Button


    private lateinit var imagePermissionHelper: ImagePermissionHelper
    private var selectedImageUri: Uri? = null

    private val cardRepository by lazy {
        KvartstoneApplication.getInstance(requireContext()).cardRepository
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            cardImagePreview.load(it) {
                crossfade(true)
                placeholder(R.drawable.ic_card_minion_generic)
                error(R.drawable.ic_card_minion_generic)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupNumberPickers()
        setupSpinners()
        setupPermissions()
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
        cardImagePreview = view.findViewById(R.id.cardImagePreview)
        selectImageButton = view.findViewById(R.id.btn_select_image)
    }

    private fun setupNumberPickers() {

        manaCostPicker.apply {
            minValue = 0
            maxValue = 10
            value = 1
            wrapSelectorWheel = false
        }


        attackPicker.apply {
            minValue = 0
            maxValue = 12
            value = 1
            wrapSelectorWheel = false
        }


        healthPicker.apply {
            minValue = 1
            maxValue = 12
            value = 1
            wrapSelectorWheel = false
        }
    }

    private fun setupSpinners() {

        val effects = arrayOf("None", "Deal Damage", "Heal", "Draw Card", "Destroy Minion")
        val effectAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, effects)
        effectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        effectSpinner.adapter = effectAdapter
    }


    private fun setupPermissions() {
        imagePermissionHelper = ImagePermissionHelper(
            fragment = this,
            onGranted = { imagePickerLauncher.launch("image/*") },
            onDenied = {
                Toast.makeText(context, "Permission required to select custom images", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupListeners() {
        saveCardButton.setOnClickListener {
            saveCard()
        }

        selectImageButton.setOnClickListener {
            imagePermissionHelper.request()
        }

        cardTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.minionType -> {

                    view?.findViewById<View>(R.id.minionStatsGroup)?.visibility = View.VISIBLE
                    effectSpinner.visibility = View.GONE
                }
                R.id.spellType -> {

                    view?.findViewById<View>(R.id.minionStatsGroup)?.visibility = View.GONE
                    effectSpinner.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun saveCard() {
        val cardName = cardNameInput.text.toString().trim()

        if (cardName.isBlank()) {
            Toast.makeText(context, "Card name is required", Toast.LENGTH_SHORT).show()
            return
        }

        val manaCost = manaCostPicker.value
        val isMinion = cardTypeGroup.checkedRadioButtonId == R.id.minionType
        val attack = if (isMinion) attackPicker.value else null
        val health = if (isMinion) healthPicker.value else null


        val effect = if (!isMinion && effectSpinner.selectedItemPosition > 0) {
            when (effectSpinner.selectedItemPosition) {
                1 -> SpellEffect("damage", 3).toString()
                2 -> SpellEffect("heal", 3).toString()
                3 -> SpellEffect("draw", 1).toString()
                4 -> SpellEffect("destroy", 1).toString()
                else -> null
            }
        } else null


        val imageUri = selectedImageUri?.let { uri ->
            try {
                ImageStorageManager.saveImageToInternalStorage(requireContext(), uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
                null
            }
        }

        val newCard = CardEntity(
            name = cardName,
            description = "Custom ${if (isMinion) "minion" else "spell"} card",
            type = if (isMinion) "minion" else "spell",
            manaCost = manaCost,
            attack = attack,
            health = health,
            effect = effect,
            imageResName = "ic_card_generic",
            rarity = "common",
            heroClass = "neutral",
            isCustom = true,
            imageUri = imageUri
        )


        lifecycleScope.launch {
            try {
                cardRepository.insert(newCard)


                Toast.makeText(context, "Card '$cardName' created successfully!", Toast.LENGTH_SHORT).show()


                clearForm()


                parentFragmentManager.popBackStack()

            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save card: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearForm() {
        cardNameInput.setText("")
        manaCostPicker.value = 1
        attackPicker.value = 1
        healthPicker.value = 1
        cardTypeGroup.check(R.id.minionType)
        effectSpinner.setSelection(0)
        selectedImageUri = null
        cardImagePreview.setImageResource(R.drawable.ic_card_minion_generic)
    }
}
