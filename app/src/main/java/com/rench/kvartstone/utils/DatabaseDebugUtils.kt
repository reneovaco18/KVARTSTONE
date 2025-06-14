package com.rench.kvartstone.utils

import android.content.Context
import android.util.Log
import com.rench.kvartstone.data.AppDatabase
import com.rench.kvartstone.data.repositories.CardRepository
import com.rench.kvartstone.data.repositories.DeckRepository
import com.rench.kvartstone.data.repositories.HeroPowerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

object DatabaseDebugUtils {

    private const val TAG = "DatabaseDebug"

    suspend fun performHealthCheck(context: Context): DatabaseHealthReport {
        Log.d(TAG, "Starting comprehensive database health check")
        val report = DatabaseHealthReport()

        try {
            report.databaseFileExists = checkDatabaseFileExists(context)
            report.databaseConnectable = checkDatabaseConnectivity(context)

            if (report.databaseConnectable) {
                // These calls will now work because the methods are defined in the repositories
                val cardRepo = CardRepository(context)
                val deckRepo = DeckRepository(context)
                val heroPowerRepo = HeroPowerRepository(context)

                report.cardCount = cardRepo.getCardCount()
                report.deckCount = deckRepo.getDeckCount()
                report.heroPowerCount = heroPowerRepo.getActivePowerCount() // FIXED

                report.hasDefaultData = report.cardCount > 0 && report.deckCount > 0
                report.sampleDataValid = verifySampleData(cardRepo, deckRepo)
            }

            report.databaseFileInfo = getDatabaseFileInfo(context)
            Log.d(TAG, "Health check completed: ${report.getOverallHealth()}")

        } catch (e: Exception) {
            Log.e(TAG, "Error during health check", e)
            report.error = e.message
        }
        return report
    }

    private fun checkDatabaseFileExists(context: Context): Boolean {
        val dbPath = context.getDatabasePath("kvartstone_database")
        return dbPath.exists()
    }

    private suspend fun checkDatabaseConnectivity(context: Context): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(context).query("SELECT 1", null)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun verifySampleData(cardRepo: CardRepository, deckRepo: DeckRepository): Boolean {
        return try {
            // These calls to getAllCards() and getAllDecks() are now resolved
            val cards = cardRepo.allCards.firstOrNull() // FIXED
            val decks = deckRepo.allDecks.firstOrNull() // FIXED

            // **FIX FOR AMBIGUITY ERROR**: Use a 'let' block to handle the nullable list.
            // This creates a non-nullable scope ('cardList') for the 'any' function.
            val hasValidCards = cards?.let { cardList ->
                cardList.any { card -> card.name.isNotBlank() }
            } ?: false

            val hasValidDecks = decks?.let { deckList ->
                deckList.any { deck -> deck.name.isNotBlank() }
            } ?: false

            hasValidCards && hasValidDecks
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying sample data", e)
            false
        }
    }

    private fun getDatabaseFileInfo(context: Context): DatabaseFileInfo {
        val dbPath = context.getDatabasePath("kvartstone_database")
        return DatabaseFileInfo(
            path = dbPath.absolutePath,
            exists = dbPath.exists(),
            size = if (dbPath.exists()) dbPath.length() else 0,
            canRead = dbPath.canRead(),
            canWrite = dbPath.canWrite(),
            lastModified = if (dbPath.exists()) dbPath.lastModified() else 0
        )
    }

    suspend fun clearAllTables(context: Context): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                Log.w(TAG, "CLEARING ALL DATABASE TABLES...")
                AppDatabase.getDatabase(context).clearAllTables()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during clear all tables", e)
            false
        }
    }
}

// Data classes remain the same
data class DatabaseHealthReport(
    var databaseFileExists: Boolean = false,
    var databaseConnectable: Boolean = false,
    var cardCount: Int = 0,
    var deckCount: Int = 0,
    var heroPowerCount: Int = 0,
    var hasDefaultData: Boolean = false,
    var sampleDataValid: Boolean = false,
    var databaseFileInfo: DatabaseFileInfo? = null,
    var error: String? = null
) {
    fun getOverallHealth(): String {
        return when {
            error != null -> "ERROR: $error"
            !databaseFileExists -> "CRITICAL: Database file missing"
            !databaseConnectable -> "CRITICAL: Cannot connect to database"
            !hasDefaultData -> "WARNING: Initial data is missing (Cards: $cardCount, Decks: $deckCount)"
            !sampleDataValid -> "WARNING: Invalid sample data detected"
            else -> "HEALTHY: All checks passed"
        }
    }
}

data class DatabaseFileInfo(
    val path: String,
    val exists: Boolean,
    val size: Long,
    val canRead: Boolean,
    val canWrite: Boolean,
    val lastModified: Long
)
