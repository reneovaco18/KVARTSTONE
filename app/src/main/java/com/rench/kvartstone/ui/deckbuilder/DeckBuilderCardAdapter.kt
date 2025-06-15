package com.rench.kvartstone.ui.deckbuilder

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
import com.rench.kvartstone.data.entities.CardEntity
import com.rench.kvartstone.ui.ext.loadCard

class DeckBuilderCardAdapter(
    private val onCardClick: (CardEntity) -> Unit,
    private val onCardLongClick: (CardEntity) -> Unit,
    private val getCardCount: (CardEntity) -> Int
) : ListAdapter<CardEntity, DeckBuilderCardAdapter.CardViewHolder>(CardEntityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_deck_builder_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.root_card_view)
        private val cardImage: ImageView = itemView.findViewById(R.id.cardArt)
        private val cardName: TextView = itemView.findViewById(R.id.cardName)
        private val cardCost: TextView = itemView.findViewById(R.id.cardCost)
        private val cardType: TextView = itemView.findViewById(R.id.cardType)
        private val cardStats: TextView = itemView.findViewById(R.id.cardStats)
        private val cardCount: TextView = itemView.findViewById(R.id.cardCount)

        fun bind(card: CardEntity) {
            cardName.text = card.name
            cardCost.text = card.manaCost.toString()
            cardType.text = card.type.replaceFirstChar { it.uppercase() }


            val resourceId = itemView.context.resources.getIdentifier(
                card.imageResName, "drawable", itemView.context.packageName
            )
            cardImage.loadCard(card)


            if (card.type == "minion" && card.attack != null && card.health != null) {
                cardStats.visibility = View.VISIBLE
                cardStats.text = "${card.attack}/${card.health}"
            } else {
                cardStats.visibility = View.GONE
            }


            val countInDeck = getCardCount(card)
            if (countInDeck > 0) {
                cardCount.visibility = View.VISIBLE
                cardCount.text = countInDeck.toString()
                cardView.setCardBackgroundColor(itemView.context.getColor(R.color.minion_in_deck))
            } else {
                cardCount.visibility = View.GONE
                cardView.setCardBackgroundColor(itemView.context.getColor(R.color.card_default))
            }


            itemView.setOnClickListener { onCardClick(card) }
            itemView.setOnLongClickListener { onCardLongClick(card); true }


            when (card.type) {
                "minion" -> cardView.setCardBackgroundColor(
                    if (countInDeck > 0) itemView.context.getColor(R.color.minion_in_deck)
                    else itemView.context.getColor(R.color.minion_brown))
                "spell" -> cardView.setCardBackgroundColor(
                    if (countInDeck > 0) itemView.context.getColor(R.color.spell_in_deck)
                    else itemView.context.getColor(R.color.spell_blue))
            }
        }
    }

    class CardEntityDiffCallback : DiffUtil.ItemCallback<CardEntity>() {
        override fun areItemsTheSame(oldItem: CardEntity, newItem: CardEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CardEntity, newItem: CardEntity): Boolean {
            return oldItem == newItem
        }
    }
}
