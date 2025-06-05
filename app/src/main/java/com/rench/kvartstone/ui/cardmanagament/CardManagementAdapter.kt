package com.rench.kvartstone.ui.cardmanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rench.kvartstone.R
import com.rench.kvartstone.data.entities.CardEntity

class CardManagementAdapter(
    private val onCardClick: (CardEntity) -> Unit,
    private val onCardLongClick: (CardEntity) -> Unit,
    private val onDeleteClick: (CardEntity) -> Unit
) : RecyclerView.Adapter<CardManagementAdapter.CardViewHolder>() {

    private var cards: List<CardEntity> = emptyList()

    fun submitList(newCards: List<CardEntity>) {
        cards = newCards
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = cards.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_management, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(cards[position])
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

            // Load card image
            val resourceId = itemView.context.resources.getIdentifier(
                card.imageResName, "drawable", itemView.context.packageName
            )
            cardImage.setImageResource(if (resourceId != 0) resourceId else R.drawable.card_icon_dragon)

            // Show custom indicator for user-created cards
            customIndicator.visibility = if (card.isCustom) View.VISIBLE else View.GONE

            // Set rarity color
            val rarityColor = when (card.rarity.lowercase()) {
                "legendary" -> R.color.card_legendary_orange
                "epic" -> R.color.card_epic_purple
                "rare" -> R.color.card_rare_blue
                else -> R.color.card_common_gray
            }
            cardRarity.setTextColor(itemView.context.getColor(rarityColor))

            // Set click listeners
            itemView.setOnClickListener { onCardClick(card) }
            itemView.setOnLongClickListener {
                onCardLongClick(card)
                true
            }
            deleteButton.setOnClickListener { onDeleteClick(card) }
        }
    }
}
