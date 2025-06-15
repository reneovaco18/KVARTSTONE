package com.rench.kvartstone.ui.fragments

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.rench.kvartstone.R
import com.rench.kvartstone.domain.*
import com.rench.kvartstone.ui.ext.loadCard

class CardDetailFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_card_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val card = arguments?.getParcelable<Card>(ARG_CARD) ?: return

        val name        = view.findViewById<TextView>(R.id.cardDetailName)
        val cardImage   = view.findViewById<ImageView>(R.id.cardDetailImage)
        val manaCostTxt = view.findViewById<TextView>(R.id.cardDetailManaCost)
        val typeTxt     = view.findViewById<TextView>(R.id.cardDetailType)
        val desc        = view.findViewById<TextView>(R.id.cardDetailDescription)
        val statRow     = view.findViewById<LinearLayout>(R.id.cardDetailStatsContainer)
        val atkTxt      = view.findViewById<TextView>(R.id.cardDetailAttack)
        val hpTxt       = view.findViewById<TextView>(R.id.cardDetailHealth)
        val closeBtn    = view.findViewById<Button>(R.id.backButton)

        // populate fields
        name.text = card.name
        cardImage.loadCard(card)  // Load the actual card image or placeholder
        manaCostTxt.text = "Mana Cost: ${card.manaCost}"
        typeTxt.text = "Type: ${card::class.simpleName?.replace("Card", "")}"

        when (card) {
            is MinionCard -> {
                statRow.visibility = View.VISIBLE
                atkTxt.text = "Attack: ${card.attack}"
                hpTxt.text = "Health: ${card.maxHealth}"
                desc.text = if (card.hasDivineShield) {
                    "Minion with Divine Shield • ${card.name}"
                } else {
                    "Minion • ${card.name}"
                }
            }
            is SpellCard -> {
                statRow.visibility = View.GONE
                desc.text = card.description.ifBlank { "Cast this spell to unleash its power." }
            }
        }

        closeBtn.setOnClickListener { dismiss() }
    }

    override fun onStart() {
        super.onStart()

        // Make dialog take up most of the screen
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.95).toInt()
        val height = (displayMetrics.heightPixels * 0.90).toInt()

        dialog?.window?.setLayout(width, height)
    }

    companion object {
        private const val ARG_CARD = "card_data"
        fun newInstance(card: Card) = CardDetailFragment().apply {
            arguments = Bundle().apply { putParcelable(ARG_CARD, card) }
        }
    }
}
