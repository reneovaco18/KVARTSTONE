// ui/ext/CardMapper.kt
package com.rench.kvartstone.ui.ext

import android.content.Context
import com.rench.kvartstone.data.repositories.CardRepository
import com.rench.kvartstone.data.entities.CardEntity
import com.rench.kvartstone.domain.Card

object CardMapper {
    fun toDomain(ctx: Context, entity: CardEntity): Card =
        CardRepository(ctx).entityToCard(entity)
}
