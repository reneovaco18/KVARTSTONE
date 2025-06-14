package com.rench.kvartstone.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.rench.kvartstone.R
import com.rench.kvartstone.domain.Card
import com.rench.kvartstone.domain.MinionCard
import com.rench.kvartstone.domain.SpellCard

class CardDetailFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_card_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val card = arguments?.getParcelable<Card>(ARG_CARD) ?: return

        val name: TextView = view.findViewById(R.id.cardDetailName)
        val image: ImageView = view.findViewById(R.id.cardDetailImage)
        val description: TextView = view.findViewById(R.id.cardDetailDescription)
        val statsContainer: LinearLayout = view.findViewById(R.id.cardDetailStatsContainer)
        val attack: TextView = view.findViewById(R.id.cardDetailAttack)
        val health: TextView = view.findViewById(R.id.cardDetailHealth)
        val backButton: Button = view.findViewById(R.id.backButton)

        name.text = card.name
        val imageResId = resources.getIdentifier(card.imageResName, "drawable", requireContext().packageName)
        image.setImageResource(if (imageResId != 0) imageResId else R.drawable.ic_card_generic)

        when (card) {
            is MinionCard -> {
                statsContainer.visibility = View.VISIBLE
                attack.text = "Attack: ${card.attack}"
                health.text = "Health: ${card.maxHealth}"
                description.text = card.name // Minions often have flavor text or ability text here
            }
            is SpellCard -> {
                statsContainer.visibility = View.GONE
                description.text = card.description
            }
        }

        backButton.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        private const val ARG_CARD = "card_data"

        fun newInstance(card: Card): CardDetailFragment {
            val fragment = CardDetailFragment()
            val args = Bundle()
            args.putParcelable(ARG_CARD, card)
            fragment.arguments = args
            return fragment
        }
    }
}
