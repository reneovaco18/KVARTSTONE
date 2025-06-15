package com.rench.kvartstone.ui.cardmanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rench.kvartstone.R
import com.rench.kvartstone.data.entities.CardEntity
import com.rench.kvartstone.ui.ext.loadCard

class CardManagementAdapter(
    private val onCardClick: (CardEntity) -> Unit,
    private val onCardLongClick: (CardEntity) -> Unit,
    private val onDeleteClick: (CardEntity) -> Unit
) : ListAdapter<CardEntity, CardManagementAdapter.CardViewHolder>(CardEntityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_management, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardImage: ImageView = itemView.findViewById(R.id.cardImage)
        private val cardName: TextView = itemView.findViewById(R.id.cardName)
        private val cardType: TextView = itemView.findViewById(R.id.cardType)
        private val cardCost: TextView = itemView.findViewById(R.id.cardCost)
        private val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)

        fun bind(card: CardEntity) {
            cardName.text = card.name
            cardType.text = card.type.capitalize()
            cardCost.text = card.manaCost.toString()

            // Load card image
            val resourceId = itemView.context.resources.getIdentifier(
                card.imageResName, "drawable", itemView.context.packageName
            )
            cardImage.loadCard(card)
            itemView.setOnClickListener { onCardClick(card) }
            itemView.setOnLongClickListener { onCardLongClick(card); true }
            deleteButton.setOnClickListener { onDeleteClick(card) }
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
