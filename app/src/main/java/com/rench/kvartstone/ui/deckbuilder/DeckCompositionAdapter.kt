package com.rench.kvartstone.ui.deckbuilder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rench.kvartstone.R
import com.rench.kvartstone.data.entities.CardEntity
import com.rench.kvartstone.ui.ext.loadCard

class DeckCompositionAdapter(
    private val onRemoveCard: (Int) -> Unit,
    private val onCardClick: (CardEntity) -> Unit
) : ListAdapter<DeckCompositionItem, DeckCompositionAdapter.DeckCardViewHolder>(DeckItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckCardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_deck_composition_card, parent, false)
        return DeckCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeckCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeckCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardName: TextView = itemView.findViewById(R.id.deckCardName)
        private val cardCost: TextView = itemView.findViewById(R.id.deckCardCost)
        private val cardCount: TextView = itemView.findViewById(R.id.deckCardCount)
        private val removeButton: ImageButton = itemView.findViewById(R.id.removeCardButton)
        private val cardImage: ImageView = itemView.findViewById(R.id.cardArt)

        fun bind(item: DeckCompositionItem) {
            val card = item.card
            cardName.text = card.name
            cardCost.text = card.manaCost.toString()
            cardCount.text = "x${item.count}"


            val resourceId = itemView.context.resources.getIdentifier(
                card.imageResName, "drawable", itemView.context.packageName
            )
            cardImage.loadCard(card)


            removeButton.setOnClickListener { onRemoveCard(card.id) }
            itemView.setOnClickListener { onCardClick(card) }
        }
    }

    class DeckItemDiffCallback : DiffUtil.ItemCallback<DeckCompositionItem>() {
        override fun areItemsTheSame(oldItem: DeckCompositionItem, newItem: DeckCompositionItem): Boolean {
            return oldItem.card.id == newItem.card.id
        }

        override fun areContentsTheSame(oldItem: DeckCompositionItem, newItem: DeckCompositionItem): Boolean {
            return oldItem.count == newItem.count && oldItem.card == newItem.card
        }
    }
}
