package com.rench.kvartstone.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rench.kvartstone.R
import com.rench.kvartstone.domain.MinionCard

class MinionBoardAdapter(
    private val onMinionClick: (Int) -> Unit,
    private val canAttackWithMinion: (Int) -> Boolean = { false },
    private val isMinionSelected: (Int) -> Boolean = { false },
    private val isValidTarget: (MinionCard) -> Boolean = { false },
    private val isPlayerBoard: Boolean = true
) : ListAdapter<MinionCard, MinionBoardAdapter.MinionViewHolder>(MinionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MinionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_minion_board, parent, false)
        return MinionViewHolder(
            view,
            onMinionClick,
            canAttackWithMinion,
            isMinionSelected,
            isValidTarget,
            isPlayerBoard
        )
    }

    override fun onBindViewHolder(holder: MinionViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class MinionViewHolder(
        itemView: View,
        private val onMinionClick: (Int) -> Unit,
        private val canAttackWithMinion: (Int) -> Boolean,
        private val isMinionSelected: (Int) -> Boolean,
        private val isValidTarget: (MinionCard) -> Boolean,
        private val isPlayerBoard: Boolean
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardView: CardView = itemView as CardView
        private val minionImage: ImageView = itemView.findViewById(R.id.minionImage)
        private val minionAttack: TextView = itemView.findViewById(R.id.minionAttack)
        private val minionHealth: TextView = itemView.findViewById(R.id.minionHealth)
        private val divineShieldIndicator: View? = itemView.findViewById(R.id.divineShieldIndicator)
        private val summonsicknessIndicator: View? = itemView.findViewById(R.id.summonsicknessIndicator)
        private val highlightBorder: View = itemView.findViewById(R.id.highlightBorder)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onMinionClick(adapterPosition)
                }
            }
        }

        fun bind(minion: MinionCard, position: Int) {
            val imageResId = itemView.context.resources.getIdentifier(minion.imageResName, "drawable", itemView.context.packageName)
            minionImage.setImageResource(if (imageResId != 0) imageResId else R.drawable.ic_card_minion_generic)
            minionAttack.text = minion.attack.toString()
            minionHealth.text = minion.currentHealth.toString()
            highlightBorder.visibility = View.GONE // Hide border by default

            val canAttack = if (isPlayerBoard) canAttackWithMinion(position) else false
            val isSelected = if (isPlayerBoard) isMinionSelected(position) else false
            val isTarget = isValidTarget(minion)
            val cannotAct = minion.summoned || (minion.hasAttackedThisTurn && isPlayerBoard)
            itemView.alpha = if (cannotAct) 0.6f else 1.0f

            if (isPlayerBoard) {
                // Friendly minion highlighting
                if (isSelected) {
                    highlightBorder.visibility = View.VISIBLE
                    highlightBorder.setBackgroundResource(R.drawable.border_selected_attacker)
                } else if (canAttack) {
                    highlightBorder.visibility = View.VISIBLE
                    highlightBorder.setBackgroundResource(R.drawable.border_can_attack)
                }
            } else {
                // Enemy minion highlighting
                if (isTarget) {
                    highlightBorder.visibility = View.VISIBLE
                    highlightBorder.setBackgroundResource(R.drawable.border_valid_target)
                }
            }

            // Divine Shield visual effect
            divineShieldIndicator?.visibility = if (minion.hasDivineShield) View.VISIBLE else View.GONE
            if (minion.hasDivineShield) {
                cardView.setCardBackgroundColor(itemView.context.getColor(R.color.divine_shield_gold))
            } else {
                cardView.setCardBackgroundColor(itemView.context.getColor(R.color.minion_brown))
            }

            // Summoning sickness or has attacked
            summonsicknessIndicator?.visibility = if (minion.summoned || minion.hasAttackedThisTurn) View.VISIBLE else View.GONE

            // Selection highlight
            if (isSelected) { // FIX: Use the 'isSelected' variable
                cardView.setCardBackgroundColor(
                    itemView.context.getColor(R.color.selected_green)
                )
                cardView.cardElevation = 12f
            } else {
                cardView.cardElevation = 4f
            }

            // Health color coding
            when {
                minion.currentHealth <= 0 -> {
                    minionHealth.setTextColor(itemView.context.getColor(R.color.dead_red))
                }
                minion.currentHealth < minion.maxHealth -> {
                    minionHealth.setTextColor(itemView.context.getColor(R.color.damaged_orange))
                }
                else -> {
                    minionHealth.setTextColor(itemView.context.getColor(R.color.healthy_green))
                }
            }

            // Attack color coding for buffed minions
            if (minion.attack > minion.id % 10) { // Simple heuristic for base attack
                minionAttack.setTextColor(itemView.context.getColor(R.color.buffed_blue))
            } else {
                minionAttack.setTextColor(itemView.context.getColor(R.color.attack_red))
            }

            itemView.isClickable = isPlayerBoard
        }
    }

    class MinionDiffCallback : DiffUtil.ItemCallback<MinionCard>() {
        override fun areItemsTheSame(oldItem: MinionCard, newItem: MinionCard): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MinionCard, newItem: MinionCard): Boolean {
            return oldItem.attack == newItem.attack &&
                    oldItem.currentHealth == newItem.currentHealth &&
                    oldItem.maxHealth == newItem.maxHealth &&
                    oldItem.hasDivineShield == newItem.hasDivineShield &&
                    oldItem.hasAttackedThisTurn == newItem.hasAttackedThisTurn &&
                    oldItem.canAttackThisTurn == newItem.canAttackThisTurn &&
                    oldItem.summoned == newItem.summoned
        }
    }
}
