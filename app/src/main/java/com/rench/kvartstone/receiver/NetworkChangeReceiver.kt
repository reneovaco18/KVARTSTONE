package com.rench.kvartstone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import androidx.work.*
import com.rench.kvartstone.worker.CardSyncWorker
import com.rench.kvartstone.core.Constants

class NetworkChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NetworkReceiver", "Network state changed")

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isConnected = cm.activeNetworkInfo?.isConnectedOrConnecting == true

        if (isConnected) {
            Log.d("NetworkReceiver", "Network connected - triggering sync")
            triggerImmediateSync(context)
        }
    }

    private fun triggerImmediateSync(context: Context) {
        val work = OneTimeWorkRequestBuilder<CardSyncWorker>()
            .addTag(Constants.WORK_TAG_SYNC)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueue(work)
    }
}

