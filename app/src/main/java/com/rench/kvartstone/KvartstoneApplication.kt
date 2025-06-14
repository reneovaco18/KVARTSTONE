package com.rench.kvartstone

import android.app.Application
import com.rench.kvartstone.data.AppDatabase
import com.rench.kvartstone.data.repositories.CardRepository
import com.rench.kvartstone.data.repositories.DeckRepository
import com.rench.kvartstone.data.repositories.HeroPowerRepository

class KvartstoneApplication : Application() {
    /**
     * The single instance of the database, created lazily when first accessed.
     */
    val database by lazy { AppDatabase.getDatabase(this) }

    /**
     * Lazily created singleton instances of each repository.
     * They use the application context, which is safe and prevents memory leaks.
     */
    val cardRepository by lazy { CardRepository(this) }
    val deckRepository by lazy { DeckRepository(this) }
    val heroPowerRepository by lazy { HeroPowerRepository(this) }
}
