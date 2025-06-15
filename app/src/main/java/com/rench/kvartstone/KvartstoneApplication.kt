package com.rench.kvartstone

import android.app.Application
import androidx.work.*
import com.rench.kvartstone.data.AppDatabase
import com.rench.kvartstone.data.repositories.CardRepository
import com.rench.kvartstone.data.repositories.DeckRepository
import com.rench.kvartstone.data.repositories.HeroPowerRepository
import com.rench.kvartstone.notification.NotificationHelper
import com.rench.kvartstone.worker.CardSyncWorker
import com.rench.kvartstone.alarm.AlarmScheduler
import com.rench.kvartstone.core.Constants
import java.util.concurrent.TimeUnit

class KvartstoneApplication : Application() {


    val database by lazy { AppDatabase.getDatabase(this) }


    val cardRepository by lazy { CardRepository(this) }
    val deckRepository by lazy { DeckRepository(this) }
    val heroPowerRepository by lazy { HeroPowerRepository(this) }

    override fun onCreate() {
        super.onCreate()


        initializeNotifications()


        initializeAlarms()


        initializeBackgroundWork()
    }


    private fun initializeNotifications() {
        NotificationHelper.createChannels(this)
    }


    private fun initializeAlarms() {
        AlarmScheduler.scheduleDaily(this)
    }


    private fun initializeBackgroundWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<CardSyncWorker>(
            6, TimeUnit.HOURS,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(Constants.WORK_TAG_SYNC)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.WORK_TAG_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )
    }


    companion object {
        fun getInstance(context: android.content.Context): KvartstoneApplication {
            return context.applicationContext as KvartstoneApplication
        }
    }
}
