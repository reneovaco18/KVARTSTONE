// Hero Power Selection Fragment
package com.rench.kvartstone.ui.heroselection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rench.kvartstone.R
import com.rench.kvartstone.domain.HeroPower

class HeroPowerSelectionFragment : Fragment(R.layout.fragment_hero_power_selection) {

    private val viewModel: HeroPowerSelectionViewModel by viewModels()
    private lateinit var heroPowerAdapter: HeroPowerSelectionAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var titleText: TextView
    private lateinit var confirmButton: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRecyclerView()
        observeViewModel()
        setupClickListeners()

        viewModel.loadAvailableHeroPowers()
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.heroPowerRecyclerView)
        titleText = view.findViewById(R.id.titleText)
        confirmButton = view.findViewById(R.id.confirmButton)
    }

    private fun setupRecyclerView() {
        heroPowerAdapter = HeroPowerSelectionAdapter { heroPower ->
            viewModel.selectHeroPower(heroPower)
        }

        recyclerView.apply {
            adapter = heroPowerAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeViewModel() {
        viewModel.heroPowers.observe(viewLifecycleOwner) { powers ->
            heroPowerAdapter.submitList(powers)
        }

        viewModel.selectedHeroPower.observe(viewLifecycleOwner) { selectedPower ->
            confirmButton.isEnabled = selectedPower != null
            heroPowerAdapter.setSelectedPower(selectedPower)
        }
    }

    private fun setupClickListeners() {
        confirmButton.setOnClickListener {
            viewModel.selectedHeroPower.value?.let { heroPower ->
                // Pass selected hero power to game setup
                val bundle = Bundle().apply {
                    putInt("selectedHeroPowerId", heroPower.id)
                }
                findNavController().navigate(R.id.action_heroPowerSelection_to_deckSelection, bundle)
            }
        }
    }
}
// Hero Power Selection Adapter
class HeroPowerSelectionAdapter(
    private val onPowerSelected: (HeroPower) -> Unit
) : ListAdapter<HeroPower, HeroPowerSelectionAdapter.HeroPowerViewHolder>(HeroPowerDiffCallback()) {

    private var selectedPowerId: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeroPowerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hero_power_selection, parent, false)
        return HeroPowerViewHolder(view)
    }

    override fun onBindViewHolder(holder: HeroPowerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setSelectedPower(heroPower: HeroPower?) {
        val oldSelectedId = selectedPowerId
        selectedPowerId = heroPower?.id

        // Notify changes for old and new selections
        oldSelectedId?.let { id ->
            notifyItemChanged(currentList.indexOfFirst { it.id == id })
        }
        selectedPowerId?.let { id ->
            notifyItemChanged(currentList.indexOfFirst { it.id == id })
        }
    }

    inner class HeroPowerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val powerImage: ImageView = itemView.findViewById(R.id.powerImage)
        private val powerName: TextView = itemView.findViewById(R.id.powerName)
        private val powerDescription: TextView = itemView.findViewById(R.id.powerDescription)
        private val powerCost: TextView = itemView.findViewById(R.id.powerCost)
        private val selectionIndicator: View = itemView.findViewById(R.id.selectionIndicator)

        fun bind(heroPower: HeroPower) {
            powerName.text = heroPower.name
            powerDescription.text = heroPower.description
            powerCost.text = heroPower.cost.toString()

            // Load power image
            val resourceId = itemView.context.resources.getIdentifier(
                heroPower.imageResName, "drawable", itemView.context.packageName
            )
            powerImage.setImageResource(if (resourceId != 0) resourceId else R.drawable.hero_power_fire)

            // Show selection state
            val isSelected = selectedPowerId == heroPower.id
            selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
            itemView.isSelected = isSelected

            // Set click listener
            itemView.setOnClickListener {
                onPowerSelected(heroPower)
            }
        }
    }

    class HeroPowerDiffCallback : DiffUtil.ItemCallback<HeroPower>() {
        override fun areItemsTheSame(oldItem: HeroPower, newItem: HeroPower): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HeroPower, newItem: HeroPower): Boolean {
            return oldItem == newItem
        }
    }
}