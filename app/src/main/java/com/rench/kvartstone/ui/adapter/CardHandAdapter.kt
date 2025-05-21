// CardHandAdapter.kt
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
import com.rench.kvartstone.domain.Card
import com.rench.kvartstone.domain.MinionCard
import com.rench.kvartstone.domain.SpellCard

class CardHandAdapter(
    private val onCardClick: (Int) -> Unit
) : ListAdapter<Card, CardHandAdapter.CardViewHolder>(CardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_hand, parent, false)
        return CardViewHolder(view, onCardClick)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CardViewHolder(
        itemView: View,
        private val onCardClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardImage: ImageView = itemView.findViewById(R.id.cardImage)
        private val cardName: TextView = itemView.findViewById(R.id.cardName)
        private val cardMana: TextView = itemView.findViewById(R.id.cardMana)
        private val cardAttack: TextView = itemView.findViewById(R.id.cardAttack)
        private val cardHealth: TextView = itemView.findViewById(R.id.cardHealth)

        init {
            itemView.setOnClickListener {
                onCardClick(adapterPosition)
            }
        }

        fun bind(card: Card) {
            cardImage.setImageResource(card.imageRes)
            cardName.text = card.name
            cardMana.text = card.manaCost.toString()

            when (card) {
                is MinionCard -> {
                    cardAttack.visibility = View.VISIBLE
                    cardHealth.visibility = View.VISIBLE
                    cardAttack.text = card.attack.toString()
                    cardHealth.text = card.health.toString()
                }
                is SpellCard -> {
                    cardAttack.visibility = View.GONE
                    cardHealth.visibility = View.GONE
                }
            }
        }
    }

    class CardDiffCallback : DiffUtil.ItemCallback<Card>() {
        override fun areItemsTheSame(oldItem: Card, newItem: Card): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Card, newItem: Card): Boolean {
            return oldItem == newItem
        }
    }
}