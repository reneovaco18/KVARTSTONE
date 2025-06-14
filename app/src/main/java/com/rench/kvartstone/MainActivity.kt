package com.rench.kvartstone

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rench.kvartstone.data.AppDatabase
import com.rench.kvartstone.data.repositories.CardRepository
import com.rench.kvartstone.data.repositories.DeckRepository
import com.rench.kvartstone.data.repositories.HeroPowerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // Declare properties to hold the repository instances
    private lateinit var cardRepository: CardRepository
    private lateinit var deckRepository: DeckRepository
    private lateinit var heroPowerRepository: HeroPowerRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "Activity created, initializing database connection")

        // 1. Get the KvartstoneApplication instance
        val application = application as KvartstoneApplication

        // 2. Access the singleton repositories from the application instance
        cardRepository = application.cardRepository
        deckRepository = application.deckRepository
        heroPowerRepository = application.heroPowerRepository

        // 3. Initialize database with proper waiting and verification
        initializeDatabaseWithVerification()
    }

    private fun initializeDatabaseWithVerification() {
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Starting database initialization process")

                // Show loading indicator to user
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Initializing game data...", Toast.LENGTH_SHORT).show()
                }

                withContext(Dispatchers.IO) {
                    // Wait for database initialization to complete (with timeout)
                    val initSuccess = AppDatabase.waitForInitialization(30)

                    if (!initSuccess) {
                        Log.e("MainActivity", "Database initialization timed out")
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Database initialization failed", Toast.LENGTH_LONG).show()
                        }
                        return@withContext
                    }

                    // Verify database is properly initialized
                    val isInitialized = verifyDatabaseInitialization()

                    if (isInitialized) {
                        Log.d("MainActivity", "Database verification successful")
                        runOnUiThread {
                            onDatabaseReady()
                        }
                    } else {
                        Log.e("MainActivity", "Database verification failed")
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Game data verification failed", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error during database initialization", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to initialize game data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun verifyDatabaseInitialization(): Boolean {
        return try {
            // Check that essential data exists
            val cards = cardRepository.getAllCardsAsEntity().firstOrNull()
            val decks = deckRepository.allDecks.firstOrNull()

            val cardCount = cards?.size ?: 0
            val deckCount = decks?.size ?: 0

            Log.d("MainActivity", "Database verification - Cards: $cardCount, Decks: $deckCount")

            // Consider database properly initialized if we have both cards and decks
            val isValid = cardCount > 0 && deckCount > 0

            if (isValid) {
                Log.d("MainActivity", "✓ Database contains valid game data")

                // Log some details about the data for debugging
                cards?.take(3)?.forEach { card ->
                    Log.d("MainActivity", "Sample card: ID=${card.id}, Name='${card.name}', Type=${card.type}")
                }

                decks?.take(2)?.forEach { deck ->
                    Log.d("MainActivity", "Sample deck: ID=${deck.id}, Name='${deck.name}', Cards=${deck.cards.size}")
                }
            } else {
                Log.w("MainActivity", "✗ Database verification failed - insufficient data")
            }

            isValid
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during database verification", e)
            false
        }
    }

    private fun onDatabaseReady() {
        Log.d("MainActivity", "Database is ready, starting game initialization")

        // Show success message
        Toast.makeText(this, "Game data loaded successfully!", Toast.LENGTH_SHORT).show()

        // Here you can proceed with your game initialization
        // For example, loading the main game UI or menu
        initializeGameUI()
    }

    private fun initializeGameUI() {
        // This is where you would typically:
        // 1. Load your game fragments
        // 2. Set up navigation
        // 3. Initialize game state
        // 4. Show main menu or game board

        Log.d("MainActivity", "Game UI initialization complete")

        // Example: Start observing data for UI updates
        observeGameData()
    }

    private fun observeGameData() {
        // Example of how to observe data changes
        lifecycleScope.launch {
            cardRepository.allCards.collect { cards ->
                Log.d("MainActivity", "Cards updated: ${cards.size} cards available")
                // Update UI with new card data
            }
        }

        lifecycleScope.launch {
            deckRepository.allDecks.collect { decks ->
                Log.d("MainActivity", "Decks updated: ${decks.size} decks available")
                // Update UI with new deck data
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "Activity destroyed")
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "Activity paused")
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "Activity resumed")

        // Verify database is still accessible after resume
        lifecycleScope.launch {
            try {
                val cardCount = cardRepository.getCardCount()
                val deckCount = deckRepository.getDeckCount()
                Log.d("MainActivity", "On resume - Cards: $cardCount, Decks: $deckCount")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking database on resume", e)
            }
        }
    }
}