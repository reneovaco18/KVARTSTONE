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
import com.rench.kvartstone.domain.Card
import com.rench.kvartstone.domain.MinionCard
import com.rench.kvartstone.domain.SpellCard

class CardHandAdapter(
    private val onCardClick: (Int) -> Unit,
    private val canPlayCard: (Int) -> Boolean,
    private val onCardLongClick: (Card) -> Unit, // <-- NEW
    private val isCardSelected: (Int) -> Boolean
) : ListAdapter<Card, CardHandAdapter.CardViewHolder>(CardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_hand, parent, false)
        return CardViewHolder(view, onCardClick, onCardLongClick, canPlayCard, isCardSelected)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class CardViewHolder(
        itemView: View,
        private val onCardClick: (Int) -> Unit,
        private val onCardLongClick: (Card) -> Unit, // <-- NEW
        private val canPlayCard: (Int) -> Boolean,
        private val isCardSelected: (Int) -> Boolean
    ) : RecyclerView.ViewHolder(itemView) {


        private val cardView: CardView = itemView as CardView
        private val cardImage: ImageView = itemView.findViewById(R.id.cardImage)
        private val cardName: TextView = itemView.findViewById(R.id.cardName)
        private val cardMana: TextView = itemView.findViewById(R.id.cardMana)
        private val cardAttack: TextView = itemView.findViewById(R.id.cardAttack)
        private val cardHealth: TextView = itemView.findViewById(R.id.cardHealth)

        init {
            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onCardClick(adapterPosition)
                }
            }

            // Set up the long click listener
            itemView.setOnLongClickListener {
                currentCard?.let { card ->
                    onCardLongClick(card)
                }
                true // Consume the event
            }
        }
        private var currentCard: Card? = null
        fun bind(card: Card, position: Int) {
            this.currentCard = card // Store the card
            val imageResId = itemView.context.resources.getIdentifier(card.imageResName, "drawable", itemView.context.packageName)
            cardImage.setImageResource(if (imageResId != 0) imageResId else R.drawable.ic_card_generic)

            cardName.text = card.name
            cardMana.text = card.manaCost.toString()

            // Show/hide attack and health based on card type
            when (card) {
                is MinionCard -> {
                    cardAttack.visibility = View.VISIBLE
                    cardHealth.visibility = View.VISIBLE
                    cardAttack.text = card.attack.toString()
                    cardHealth.text = card.maxHealth.toString()

                    // Show divine shield indicator
                    if (card.hasDivineShield) {
                        cardView.setCardBackgroundColor(itemView.context.getColor(R.color.divine_shield_gold))
                    }
                }
                is SpellCard -> {
                    cardAttack.visibility = View.GONE
                    cardHealth.visibility = View.GONE
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.spell_blue))
                }
            }

            // Visual feedback for playability and selection
            val canPlay = canPlayCard(position)
            val selected = isCardSelected(position)

            itemView.alpha = if (canPlay) 1.0f else 0.5f

            if (selected) {
                cardView.setCardBackgroundColor(itemView.context.getColor(R.color.selected_green))
                cardView.cardElevation = 12f
            } else {
                when (card) {
                    is MinionCard -> {
                        if (card.hasDivineShield) {
                            cardView.setCardBackgroundColor(itemView.context.getColor(R.color.divine_shield_gold))
                        } else {
                            cardView.setCardBackgroundColor(itemView.context.getColor(R.color.minion_brown))
                        }
                    }
                    is SpellCard -> {
                        cardView.setCardBackgroundColor(itemView.context.getColor(R.color.spell_blue))
                    }
                }
                cardView.cardElevation = 4f
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
