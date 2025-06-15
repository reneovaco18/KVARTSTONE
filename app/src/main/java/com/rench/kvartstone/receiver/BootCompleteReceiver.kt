
package com.rench.kvartstone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.rench.kvartstone.worker.CardSyncWorker
import com.rench.kvartstone.core.Constants
import java.util.concurrent.TimeUnit

class BootCompleteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Log.d("BootReceiver", "Device booted - scheduling periodic work")

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<CardSyncWorker>(
                6, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .addTag(Constants.WORK_TAG_SYNC)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.WORK_TAG_SYNC,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWork
            )
        }
    }
}