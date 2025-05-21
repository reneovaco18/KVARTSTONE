// MinionBoardAdapter.kt
package com.rench.kvartstone.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rench.kvartstone.R
import com.rench.kvartstone.domain.MinionCard

class MinionBoardAdapter(
    private val onMinionClick: (Int) -> Unit
) : ListAdapter<MinionCard, MinionBoardAdapter.MinionViewHolder>(MinionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MinionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_minion_board, parent, false)
        return MinionViewHolder(view, onMinionClick)
    }

    override fun onBindViewHolder(holder: MinionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MinionViewHolder(
        itemView: View,
        private val onMinionClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val minionImage: ImageView = itemView.findViewById(R.id.minionImage)
        private val minionAttack: TextView = itemView.findViewById(R.id.minionAttack)
        private val minionHealth: TextView = itemView.findViewById(R.id.minionHealth)

        init {
            itemView.setOnClickListener {
                onMinionClick(adapterPosition)
            }
        }

        fun bind(minion: MinionCard) {
            minionImage.setImageResource(minion.imageRes)
            minionAttack.text = minion.attack.toString()
            minionHealth.text = minion.currentHealth.toString()

            // Visual indication if minion can attack
            itemView.alpha = if (minion.canAttackThisTurn && !minion.hasAttackedThisTurn) 1.0f else 0.7f
        }
    }

    class MinionDiffCallback : DiffUtil.ItemCallback<MinionCard>() {
        override fun areItemsTheSame(oldItem: MinionCard, newItem: MinionCard): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MinionCard, newItem: MinionCard): Boolean {
            return oldItem.attack == newItem.attack &&
                    oldItem.currentHealth == newItem.currentHealth &&
                    oldItem.canAttackThisTurn == newItem.canAttackThisTurn &&
                    oldItem.hasAttackedThisTurn == newItem.hasAttackedThisTurn
        }
    }
}