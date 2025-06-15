package com.rench.kvartstone.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rench.kvartstone.data.entities.CardEntity
import com.rench.kvartstone.data.repositories.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class CardSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("CardSyncWorker", "Starting card sync…")

            val newCards = fetchNewCardsFromApi()
            CardRepository(applicationContext).insertAll(newCards)

            Log.d("CardSyncWorker", "Card sync completed")
            Result.success()
        } catch (e: Exception) {
            Log.e("CardSyncWorker", "Sync failed", e)
            Result.retry()
        }
    }


    private suspend fun fetchNewCardsFromApi(): List<CardEntity> {
        delay(2000)
        return emptyList()
    }
}
