// Card Editor Dialog for creating and editing cards
package com.rench.kvartstone.ui.cardmanagement

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rench.kvartstone.R
import com.rench.kvartstone.data.entities.CardEntity

class CardEditorDialog : DialogFragment() {

    private var card: CardEntity? = null
    private var onSaveCallback: ((CardEntity) -> Unit)? = null
    private var selectedImageUri: Uri? = null

    // UI Components
    private lateinit var nameInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var typeSpinner: Spinner
    private lateinit var raritySpinner: Spinner
    private lateinit var costSeekBar: SeekBar
    private lateinit var costLabel: TextView
    private lateinit var attackSeekBar: SeekBar
    private lateinit var attackLabel: TextView
    private lateinit var healthSeekBar: SeekBar
    private lateinit var healthLabel: TextView
    private lateinit var statsContainer: LinearLayout
    private lateinit var imageView: ImageView
    private lateinit var selectImageButton: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                imageView.setImageURI(uri)
            }
        }
    }

    companion object {
        fun newInstance(card: CardEntity?, onSave: (CardEntity) -> Unit): CardEditorDialog {
            return CardEditorDialog().apply {
                this.card = card
                this.onSaveCallback = onSave
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext(), R.style.FullScreenDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_card_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupSpinners()
        setupSeekBars()
        setupClickListeners()
        populateFields()
    }

    private fun initializeViews(view: View) {
        nameInput = view.findViewById(R.id.nameInput)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        typeSpinner = view.findViewById(R.id.typeSpinner)
        raritySpinner = view.findViewById(R.id.raritySpinner)
        costSeekBar = view.findViewById(R.id.costSeekBar)
        costLabel = view.findViewById(R.id.costLabel)
        attackSeekBar = view.findViewById(R.id.attackSeekBar)
        attackLabel = view.findViewById(R.id.attackLabel)
        healthSeekBar = view.findViewById(R.id.healthSeekBar)
        healthLabel = view.findViewById(R.id.healthLabel)
        statsContainer = view.findViewById(R.id.statsContainer)
        imageView = view.findViewById(R.id.cardImagePreview)
        selectImageButton = view.findViewById(R.id.selectImageButton)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)
    }

    private fun setupSpinners() {
        // Type Spinner
        val types = arrayOf("Minion", "Spell")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = typeAdapter

        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isMinion = position == 0
                statsContainer.visibility = if (isMinion) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Rarity Spinner
        val rarities = arrayOf("Common", "Rare", "Epic", "Legendary")
        val rarityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, rarities)
        rarityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        raritySpinner.adapter = rarityAdapter
    }

    private fun setupSeekBars() {
        // Cost SeekBar (0-10)
        costSeekBar.max = 10
        costSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                costLabel.text = "Mana Cost: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Attack SeekBar (0-20)
        attackSeekBar.max = 20
        attackSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                attackLabel.text = "Attack: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Health SeekBar (1-20)
        healthSeekBar.max = 19
        healthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val health = progress + 1
                healthLabel.text = "Health: $health"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupClickListeners() {
        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        saveButton.setOnClickListener {
            if (validateFields()) {
                saveCard()
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun populateFields() {
        card?.let { existingCard ->
            nameInput.setText(existingCard.name)
            descriptionInput.setText(existingCard.description)

            // Set type spinner
            val typePosition = if (existingCard.type == "minion") 0 else 1
            typeSpinner.setSelection(typePosition)

            // Set rarity spinner
            val rarityPosition = when (existingCard.rarity.lowercase()) {
                "common" -> 0
                "rare" -> 1
                "epic" -> 2
                "legendary" -> 3
                else -> 0
            }
            raritySpinner.setSelection(rarityPosition)

            // Set seekbars
            costSeekBar.progress = existingCard.manaCost
            costLabel.text = "Mana Cost: ${existingCard.manaCost}"

            if (existingCard.type == "minion") {
                attackSeekBar.progress = existingCard.attack ?: 0
                attackLabel.text = "Attack: ${existingCard.attack ?: 0}"

                healthSeekBar.progress = (existingCard.health ?: 1) - 1
                healthLabel.text = "Health: ${existingCard.health ?: 1}"
            }

            // Load existing image
            if (existingCard.imageUri != null) {
                selectedImageUri = Uri.parse(existingCard.imageUri)
                imageView.setImageURI(selectedImageUri)
            }
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        if (nameInput.text.toString().trim().isEmpty()) {
            nameInput.error = "Name is required"
            isValid = false
        }

        if (descriptionInput.text.toString().trim().isEmpty()) {
            descriptionInput.error = "Description is required"
            isValid = false
        }

        return isValid
    }

    private fun saveCard() {
        val isMinion = typeSpinner.selectedItemPosition == 0
        val rarity = (raritySpinner.selectedItem as String).lowercase()

        val newCard = CardEntity(
            id = card?.id ?: 0,
            name = nameInput.text.toString().trim(),
            description = descriptionInput.text.toString().trim(),
            type = if (isMinion) "minion" else "spell",
            rarity = rarity,
            manaCost = costSeekBar.progress,
            attack = if (isMinion) attackSeekBar.progress else null,
            health = if (isMinion) healthSeekBar.progress + 1 else null,
            effect = null, // Can be enhanced later for complex effects
            imageResName = card?.imageResName ?: "ic_card_placeholder",
            imageUri = selectedImageUri?.toString(),
            keywords = null, // Can be enhanced later
            heroClass = "neutral",
            isCustom = true,
            createdAt = card?.createdAt ?: System.currentTimeMillis()
        )

        onSaveCallback?.invoke(newCard)
        dismiss()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }
}