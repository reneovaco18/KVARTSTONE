package com.rench.kvartstone.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.rench.kvartstone.notification.NotificationHelper

class GameNotificationService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "SHOW_TURN_NOTIFICATION" -> {
                NotificationHelper.showYourTurn(this)
            }
            "SHOW_DAILY_REMINDER" -> {
                NotificationHelper.showDailyReminder(this)
            }
        }

        stopSelf()
        return START_NOT_STICKY
    }
}
