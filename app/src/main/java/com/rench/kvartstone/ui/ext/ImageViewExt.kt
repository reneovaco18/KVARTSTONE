package com.rench.kvartstone.ui.ext

import android.net.Uri
import android.widget.ImageView
import coil.load
import com.rench.kvartstone.R
import com.rench.kvartstone.data.entities.CardEntity
import com.rench.kvartstone.domain.Card

/* ---------- domain-level cards ---------- */
fun ImageView.loadCard(card: Card) = loadInternal(card.imageResName, card.imageUri)

/* ---------- DB-level cards --------------- */
fun ImageView.loadCard(card: CardEntity) =
    loadInternal(card.imageResName, card.imageUri)

/* ---------- shared implementation -------- */
private fun ImageView.loadInternal(resName: String, uri: String?) {
    val resId = context.resources.getIdentifier(
        resName, "drawable", context.packageName
    )
    when {
        uri != null -> load(Uri.parse(uri)) {
            placeholder(resId.takeIf { it != 0 } ?: R.drawable.ic_card_generic)
            error(R.drawable.ic_card_generic)          // <- existing drawable
            crossfade(true)
        }
        resId != 0 -> setImageResource(resId)
        else       -> setImageResource(R.drawable.ic_card_generic)
    }
}
