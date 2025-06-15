package com.rench.kvartstone.ui.cardmanagement

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import coil.load
import com.rench.kvartstone.R
import com.rench.kvartstone.data.entities.CardEntity
import com.rench.kvartstone.domain.SpellEffect
import com.rench.kvartstone.ui.fragments.CardDetailFragment
import com.rench.kvartstone.utils.ImageStorageManager
import java.io.File
import com.rench.kvartstone.data.repositories.CardRepository
import com.rench.kvartstone.ui.ext.CardMapper
import com.rench.kvartstone.ui.ext.loadCard

class CardEditorDialog : DialogFragment() {

    companion object {
        fun newInstance(card: CardEntity?, onSave: (CardEntity) -> Unit): CardEditorDialog {
            return CardEditorDialog().apply {
                this.existingCard = card
                this.onSaveCallback = onSave
            }
        }
    }

    private var existingCard: CardEntity? = null
    private var onSaveCallback: ((CardEntity) -> Unit)? = null
    private var selectedImageUri: Uri? = null
    private lateinit var effectGroup     : LinearLayout
    private lateinit var effectTypeSpinner: Spinner
    private lateinit var effectValuePicker: NumberPicker
    private lateinit var previewContainer : FrameLayout
    private var previewFragment: CardDetailFragment? = null
    private lateinit var nameInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var typeSpinner: Spinner
    private lateinit var raritySpinner: Spinner
    private lateinit var costSeekBar: SeekBar
    private lateinit var costLabel: TextView
    private lateinit var attackSeekBar: SeekBar
    private lateinit var attackLabel: TextView
    private lateinit var healthSeekBar: SeekBar
    private lateinit var healthLabel: TextView
    private lateinit var statsContainer: LinearLayout
    private lateinit var cardImagePreview: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    // add fields


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
    override fun onStart() {
        super.onStart()

        // Set dialog to 90% width and 80% height
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.90).toInt()
        val height = (displayMetrics.heightPixels * 0.80).toInt()

        dialog?.window?.setLayout(width, height)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_card_editor, container, false)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullWidthDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupSpinners()
        setupSeekBars()
        setupClickListeners()

        existingCard?.let { populateFields(it) }
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
        cardImagePreview = view.findViewById(R.id.cardImagePreview)
        selectImageButton = view.findViewById(R.id.btn_select_image)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)
        previewContainer    = view.findViewById(R.id.previewContainer)
        effectGroup         = view.findViewById(R.id.effectGroup)
        effectTypeSpinner   = view.findViewById(R.id.effectTypeSpinner)
        effectValuePicker   = view.findViewById(R.id.effectValuePicker)

    }

    private fun setupSpinners() {
        // effect spinner
        val effects = arrayOf("None", "Deal Damage", "Heal Hero")
        effectTypeSpinner.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, effects).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        effectValuePicker.minValue = 1
        effectValuePicker.maxValue = 10

        val types = arrayOf("Minion", "Spell")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = typeAdapter

        val rarities = arrayOf("Common", "Rare", "Epic", "Legendary")
        val rarityAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, rarities)
        rarityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        raritySpinner.adapter = rarityAdapter

        typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isSpell  = position == 1
                effectGroup.visibility = if (isSpell) View.VISIBLE else View.GONE

                val isMinion = position == 0
                statsContainer.visibility = if (isMinion) View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    private fun refreshPreview() {
        val entity = buildCardEntity()   // you already have the builder
        val card   = CardMapper.toDomain(requireContext(), entity)

        childFragmentManager.beginTransaction().apply {
            previewFragment?.let { remove(it) }
            previewFragment = CardDetailFragment.newInstance(card)
            add(R.id.previewContainer, previewFragment!!)
        }.commitNowAllowingStateLoss()

        // keep small thumbnail in sync
        cardImagePreview.loadCard(card)
    }
    private fun buildCardEntity(): CardEntity {
        val name   = nameInput.text.toString().trim()
        val desc   = descriptionInput.text.toString().trim()
        val type   = if (typeSpinner.selectedItemPosition == 0) "minion" else "spell"
        val cost   = costSeekBar.progress
        val atk    = if (type == "minion") attackSeekBar.progress else null
        val hp     = if (type == "minion") healthSeekBar.progress else null
        val rarity = arrayOf("common","rare","epic","legendary")[raritySpinner.selectedItemPosition]

        // spell effect
        val effect = if (type == "spell" && effectTypeSpinner.selectedItemPosition != 0) {
            val kind  = if (effectTypeSpinner.selectedItemPosition == 1) "damage" else "heal"
            SpellEffect(kind, effectValuePicker.value).toString()
        } else null

        return existingCard?.copy(
            name = name, description = desc, type = type, manaCost = cost,
            attack = atk, health = hp, rarity = rarity, effect = effect,
            imageUri = selectedImageUri?.let { ImageStorageManager.saveImageToInternalStorage(requireContext(), it) }
                ?: existingCard?.imageUri
        ) ?: CardEntity(
            name       = name,
            description= desc,
            type       = type,
            manaCost   = cost,
            attack     = atk,
            health     = hp,
            effect     = effect,
            imageResName = "ic_card_generic",
            rarity     = rarity,
            imageUri   = selectedImageUri?.let { ImageStorageManager.saveImageToInternalStorage(requireContext(), it) },
            isCustom   = true
        )
    }

    private fun setupSeekBars() {
        costSeekBar.max = 10
        attackSeekBar.max = 12
        healthSeekBar.max = 12

        costSeekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(sb: SeekBar?, p: Int, f: Boolean) {
                costLabel.text = "Cost: $p"
                refreshPreview()                       // <-- here
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })


        attackSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                attackLabel.text = "Attack: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        healthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                healthLabel.text = "Health: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupClickListeners() {
        saveButton.setOnClickListener {
            saveCard()
        }

        cancelButton.setOnClickListener {
            dismiss()
        }

        selectImageButton.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        refreshPreview();
    }

    private fun populateFields(card: CardEntity) {
        nameInput.setText(card.name)
        descriptionInput.setText(card.description)
        typeSpinner.setSelection(if (card.type == "minion") 0 else 1)
        costSeekBar.progress = card.manaCost

        val rarityPosition = when (card.rarity.lowercase()) {
            "common" -> 0
            "rare" -> 1
            "epic" -> 2
            "legendary" -> 3
            else -> 0
        }
        raritySpinner.setSelection(rarityPosition)

        if (card.type == "minion") {
            attackSeekBar.progress = card.attack ?: 0
            healthSeekBar.progress = card.health ?: 1
        }

        // Load existing custom image if it exists
        if (card.imageUri != null) {
            cardImagePreview.load(File(card.imageUri)) {
                crossfade(true)
                placeholder(R.drawable.ic_card_minion_generic)
                error(R.drawable.ic_card_minion_generic)
            }
        }
    }

    private fun saveCard() {
        val entity = buildCardEntity()
        if (entity.name.isBlank()) {
            Toast.makeText(context, "Name is required", Toast.LENGTH_SHORT).show()
            return
        }
        onSaveCallback?.invoke(entity)
        dismiss()
        refreshPreview();
    }

}
