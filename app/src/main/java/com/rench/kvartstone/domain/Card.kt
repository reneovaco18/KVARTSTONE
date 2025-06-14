package com.rench.kvartstone.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// FIX: Make this class Parcelable so its children can be.
@Parcelize
sealed class Card(
    open val id: Int,
    open val name: String,
    open val manaCost: Int,
    open val imageResName: String, // Standardize on this String property
    open val imageUri: String? = null
) : Parcelable {
    override fun toString(): String = name
}
