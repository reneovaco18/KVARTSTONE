package com.rench.kvartstone.ui.deckselection

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rench.kvartstone.R
import com.rench.kvartstone.domain.Deck

class DeckSelectionAdapter(
    private val onDeckSelected: (Deck) -> Unit,
    private val onDeckLongHeld : (Deck) -> Unit      //  ← NEW
) : ListAdapter<Deck, DeckSelectionAdapter.DeckViewHolder>(DeckDiffCallback()) {

    private var selectedDeckId: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deck_selection, parent, false)
        return DeckViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setSelectedDeck(deck: Deck?) {
        val oldSelectedId = selectedDeckId
        selectedDeckId = deck?.id

        oldSelectedId?.let { id ->
            val oldIndex = currentList.indexOfFirst { it.id == id }
            if (oldIndex != -1) notifyItemChanged(oldIndex)
        }
        selectedDeckId?.let { id ->
            val newIndex = currentList.indexOfFirst { it.id == id }
            if (newIndex != -1) notifyItemChanged(newIndex)
        }
    }

    inner class DeckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deckName: TextView = itemView.findViewById(R.id.deckName)
        private val deckDescription: TextView = itemView.findViewById(R.id.deckDescription)
        private val cardCount: TextView = itemView.findViewById(R.id.cardCount)
        private val selectionIndicator: View = itemView.findViewById(R.id.selectionIndicator)

        fun bind(deck: Deck) {
            deckName.text = deck.name
            deckDescription.text = deck.description
            cardCount.text = "${deck.cards.size} cards"

            val isSelected = selectedDeckId == deck.id
            selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE
            itemView.isSelected = isSelected

            itemView.setOnClickListener {
                onDeckSelected(deck)
            }
            itemView.setOnLongClickListener {                       // ← NEW
                onDeckLongHeld(deck)
                true
            }
        }
    }

    class DeckDiffCallback : DiffUtil.ItemCallback<Deck>() {
        override fun areItemsTheSame(oldItem: Deck, newItem: Deck): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Deck, newItem: Deck): Boolean {
            return oldItem == newItem
        }
    }
}
