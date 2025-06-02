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
        private val isPlayerBoard: Boolean
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardView: CardView = itemView as CardView
        private val minionImage: ImageView = itemView.findViewById(R.id.minionImage)
        private val minionAttack: TextView = itemView.findViewById(R.id.minionAttack)
        private val minionHealth: TextView = itemView.findViewById(R.id.minionHealth)
        private val divineShieldIndicator: View? = itemView.findViewById(R.id.divineShieldIndicator)
        private val summonsicknessIndicator: View? = itemView.findViewById(R.id.summonsicknessIndicator)

        init {
            itemView.setOnClickListener {
                onMinionClick(adapterPosition)
            }
        }

        fun bind(minion: MinionCard, position: Int) {
            minionImage.setImageResource(minion.imageRes)
            minionAttack.text = minion.attack.toString()
            minionHealth.text = minion.currentHealth.toString()

            // Visual indicators
            val canAttack = if (isPlayerBoard) canAttackWithMinion(position) else false
            val selected = if (isPlayerBoard) isMinionSelected(position) else false

            // Divine Shield visual effect
            if (minion.hasDivineShield) {
                divineShieldIndicator?.visibility = View.VISIBLE
                cardView.setCardBackgroundColor(
                    itemView.context.getColor(R.color.divine_shield_gold)
                )
            } else {
                divineShieldIndicator?.visibility = View.GONE
                cardView.setCardBackgroundColor(
                    itemView.context.getColor(R.color.minion_brown)
                )
            }

            // Summoning sickness or has attacked
            if (minion.summoned || minion.hasAttackedThisTurn) {
                summonsicknessIndicator?.visibility = View.VISIBLE
                itemView.alpha = 0.6f
            } else {
                summonsicknessIndicator?.visibility = View.GONE
                itemView.alpha = if (canAttack || !isPlayerBoard) 1.0f else 0.8f
            }

            // Selection highlight
            if (selected) {
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

            // Clickable only if it's player's board and can attack
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
