package com.rench.kvartstone.ui.deckbuilder

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.rench.kvartstone.R
import com.rench.kvartstone.data.entities.CardEntity

class DeckBuilderFragment : Fragment(R.layout.fragment_deck_builder) {

    private val viewModel: DeckBuilderViewModel by viewModels()
    private lateinit var availableCardsAdapter: DeckBuilderCardAdapter
    private lateinit var deckCompositionAdapter: DeckCompositionAdapter


    private lateinit var availableCardsRecyclerView: RecyclerView
    private lateinit var deckCompositionRecyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var filterSpinner: Spinner
    private lateinit var deckNameText: TextView
    private lateinit var deckCountText: TextView
    private lateinit var saveDeckButton: MaterialButton
    private lateinit var clearDeckButton: MaterialButton
    private lateinit var backButton: ImageButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val deckId = arguments?.getInt("deckId", -1) ?: -1
        if (deckId != -1) {
            viewModel.loadExistingDeck(deckId)
        }

        initializeViews(view)
        setupRecyclerViews()
        setupSearchAndFilter()
        observeViewModel()
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        availableCardsRecyclerView = view.findViewById(R.id.availableCardsRecyclerView)
        deckCompositionRecyclerView = view.findViewById(R.id.deckCompositionRecyclerView)
        searchView = view.findViewById(R.id.searchView)
        filterSpinner = view.findViewById(R.id.filterSpinner)
        deckNameText = view.findViewById(R.id.deckNameText)
        deckCountText = view.findViewById(R.id.deckCountText)
        saveDeckButton = view.findViewById(R.id.saveDeckButton)
        clearDeckButton = view.findViewById(R.id.clearDeckButton)
        backButton = view.findViewById(R.id.backButton)
    }

    private fun setupRecyclerViews() {

        availableCardsAdapter = DeckBuilderCardAdapter(
            onCardClick = { card -> viewModel.addCardToDeck(card) },
            onCardLongClick = { card -> showCardDetails(card) },
            getCardCount = { card -> viewModel.getCardCountInDeck(card.id) }
        )

        availableCardsRecyclerView.apply {
            adapter = availableCardsAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
            setHasFixedSize(true)
        }


        deckCompositionAdapter = DeckCompositionAdapter(
            onRemoveCard = { cardId -> viewModel.removeCardFromDeck(cardId) },
            onCardClick = { card -> showCardDetails(card) }
        )

        deckCompositionRecyclerView.apply {
            adapter = deckCompositionAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupSearchAndFilter() {

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


        val filterOptions = arrayOf("All Cards", "Minions", "Spells", "Low Cost (0-3)", "High Cost (4+)")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = spinnerAdapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> viewModel.clearFilters()
                    1 -> viewModel.filterByType("minion")
                    2 -> viewModel.filterByType("spell")
                    3 -> viewModel.filterByManaCost(0, 3)
                    4 -> viewModel.filterByManaCost(4, 10)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeViewModel() {
        viewModel.availableCards.observe(viewLifecycleOwner) { cards ->
            availableCardsAdapter.submitList(cards)
        }

        viewModel.deckComposition.observe(viewLifecycleOwner) { composition ->
            deckCompositionAdapter.submitList(composition)
        }

        viewModel.deckCount.observe(viewLifecycleOwner) { count ->
            deckCountText.text = "$count/10 cards"
            saveDeckButton.isEnabled = count == 10
        }

        viewModel.deckName.observe(viewLifecycleOwner) { name ->
            deckNameText.text = name
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.deckSaved.observe(viewLifecycleOwner) { saved ->
            if (saved) {
                findNavController().navigateUp()
            }
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        deckNameText.setOnClickListener {
            showEditDeckNameDialog()
        }

        saveDeckButton.setOnClickListener {
            viewModel.saveDeck()
        }

        clearDeckButton.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Deck")
                .setMessage("Are you sure you want to remove all cards from this deck?")
                .setPositiveButton("Clear") { _, _ ->
                    viewModel.clearDeck()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showEditDeckNameDialog() {
        val input = TextInputEditText(requireContext())
        input.setText(viewModel.deckName.value)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Deck Name")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    viewModel.setDeckName(newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCardDetails(card: CardEntity) {
        val details = buildString {
            append("Name: ${card.name}\n")
            append("Type: ${card.type.capitalize()}\n")
            append("Mana Cost: ${card.manaCost}\n")
            if (card.attack != null) append("Attack: ${card.attack}\n")
            if (card.health != null) append("Health: ${card.health}\n")
            if (card.description.isNotEmpty()) append("Description: ${card.description}")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(card.name)
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }
}
