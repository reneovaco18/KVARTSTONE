package com.rench.kvartstone

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rench.kvartstone.R
import com.rench.kvartstone.data.repositories.CardRepository
import com.rench.kvartstone.data.repositories.DeckRepository
import com.rench.kvartstone.data.repositories.HeroPowerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize database with default data
        lifecycleScope.launch(Dispatchers.IO) {
            CardRepository(application).initializeDefaultCards()
            DeckRepository(application).initializeDefaultDecks()
            HeroPowerRepository(application).initializeDefaultHeroPowers()
    }
}}
