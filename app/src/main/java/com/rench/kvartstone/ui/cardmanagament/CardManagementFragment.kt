// Card Management Fragment for CRUD operations
package com.rench.kvartstone.ui.cardmanagement

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rench.kvartstone.R
import com.rench.kvartstone.data.entities.CardEntity

class CardManagementFragment : Fragment(R.layout.fragment_card_management) {

    private val viewModel: CardManagementViewModel by viewModels()
    private lateinit var cardAdapter: CardManagementAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var filterSpinner: Spinner
    private lateinit var fabAddCard: FloatingActionButton

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.setSelectedImageUri(uri)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRecyclerView()
        setupSearchAndFilter()
        observeViewModel()
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.cardsRecyclerView)
        searchView = view.findViewById(R.id.searchView)
        filterSpinner = view.findViewById(R.id.filterSpinner)
        fabAddCard = view.findViewById(R.id.fabAddCard)
    }

    private fun setupRecyclerView() {
        cardAdapter = CardManagementAdapter(
            onCardClick = { card -> editCard(card) },
            onCardLongClick = { card -> showCardOptions(card) },
            onDeleteClick = { card -> confirmDeleteCard(card) }
        )

        recyclerView.apply {
            adapter = cardAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
            setHasFixedSize(true)
        }
    }

    private fun setupSearchAndFilter() {
        // Setup search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchCards(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.searchCards(it) }
                return true
            }
        })

        // Setup filter spinner
        val filterOptions = arrayOf("All Cards", "Minions", "Spells", "Custom Cards", "By Rarity")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = spinnerAdapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> viewModel.showAllCards()
                    1 -> viewModel.filterByType("minion")
                    2 -> viewModel.filterByType("spell")
                    3 -> viewModel.filterByCustomStatus(true)
                    4 -> showRarityDialog()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeViewModel() {
        viewModel.cards.observe(viewLifecycleOwner) { cards ->
            cardAdapter.submitList(cards)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            view?.findViewById<ProgressBar>(R.id.progressBar)?.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        fabAddCard.setOnClickListener {
            showCardEditorDialog(null)
        }
    }

    private fun editCard(card: CardEntity) {
        showCardEditorDialog(card)
    }

    private fun showCardEditorDialog(card: CardEntity?) {
        CardEditorDialog.newInstance(card) { editedCard ->
            if (card == null) {
                viewModel.createCard(editedCard)
            } else {
                viewModel.updateCard(editedCard)
            }
        }.show(parentFragmentManager, "CardEditor")
    }

    private fun showCardOptions(card: CardEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(card.name)
            .setItems(arrayOf("Edit", "Duplicate", "Delete")) { _, which ->
                when (which) {
                    0 -> editCard(card)
                    1 -> duplicateCard(card)
                    2 -> confirmDeleteCard(card)
                }
            }
            .show()
    }

    private fun duplicateCard(card: CardEntity) {
        val duplicatedCard = card.copy(
            id = 0, // Auto-generate new ID
            name = "${card.name} (Copy)",
            isCustom = true,
            createdAt = System.currentTimeMillis()
        )
        viewModel.createCard(duplicatedCard)
    }

    private fun confirmDeleteCard(card: CardEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Card")
            .setMessage("Are you sure you want to delete '${card.name}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCard(card)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRarityDialog() {
        val rarities = arrayOf("Common", "Rare", "Epic", "Legendary")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by Rarity")
            .setItems(rarities) { _, which ->
                viewModel.filterByRarity(rarities[which].lowercase())
            }
            .show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }
}

// Card Management Adapter
class CardManagementAdapter(
    private val onCardClick: (CardEntity) -> Unit,
    private val onCardLongClick: (CardEntity) -> Unit,
    private val onDeleteClick: (CardEntity) -> Unit
) : ListAdapter<CardEntity, CardManagementAdapter.CardViewHolder>(CardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_management, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardImage: ImageView = itemView.findViewById(R.id.cardImage)
        private val cardName: TextView = itemView.findViewById(R.id.cardName)
        private val cardType: TextView = itemView.findViewById(R.id.cardType)
        private val cardCost: TextView = itemView.findViewById(R.id.cardCost)
        private val cardRarity: TextView = itemView.findViewById(R.id.cardRarity)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        private val customIndicator: View = itemView.findViewById(R.id.customIndicator)

        fun bind(card: CardEntity) {
            cardName.text = card.name
            cardType.text = card.type.replaceFirstChar { it.uppercase() }
            cardCost.text = card.manaCost.toString()
            cardRarity.text = card.rarity.replaceFirstChar { it.uppercase() }

            // Set rarity color
            val rarityColor = when (card.rarity.lowercase()) {
                "legendary" -> R.color.card_legendary_orange
                "epic" -> R.color.card_epic_purple
                "rare" -> R.color.card_rare_blue
                else -> R.color.card_common_gray
            }
            cardRarity.setTextColor(itemView.context.getColor(rarityColor))

            // Show custom indicator
            customIndicator.visibility = if (card.isCustom) View.VISIBLE else View.GONE

            // Load card image
            if (card.imageUri != null) {
                // Load from URI (user-added image)
                Glide.with(itemView.context)
                    .load(card.imageUri)
                    .placeholder(R.drawable.card_icon_dragon)
                    .into(cardImage)
            } else {
                // Load from resources
                val resourceId = itemView.context.resources.getIdentifier(
                    card.imageResName, "drawable", itemView.context.packageName
                )
                cardImage.setImageResource(if (resourceId != 0) resourceId else R.drawable.card_icon_dragon)
            }

            // Set click listeners
            itemView.setOnClickListener { onCardClick(card) }
            itemView.setOnLongClickListener {
                onCardLongClick(card)
                true
            }
            deleteButton.setOnClickListener { onDeleteClick(card) }
        }
    }

    class CardDiffCallback : DiffUtil.ItemCallback<CardEntity>() {
        override fun areItemsTheSame(oldItem: CardEntity, newItem: CardEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CardEntity, newItem: CardEntity): Boolean {
            return oldItem == newItem
        }
    }
}