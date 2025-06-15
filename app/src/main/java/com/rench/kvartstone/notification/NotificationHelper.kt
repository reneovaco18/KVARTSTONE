// com/rench/kvartstone/notification/NotificationHelper.kt
package com.rench.kvartstone.notification


import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rench.kvartstone.R

import com.rench.kvartstone.core.Constants
import androidx.core.app.TaskStackBuilder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import com.rench.kvartstone.MainActivity

object NotificationHelper {
    private val SMALL_ICON = R.drawable.ic_notification_small

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


            val gameChannel = NotificationChannel(
                Constants.CHANNEL_ID_GAME,
                "Game Events",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for game events like your turn"
                enableLights(true)
                enableVibration(true)
            }


            val reminderChannel = NotificationChannel(
                Constants.CHANNEL_ID_REMINDERS,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily game reminders"
            }

            notificationManager.createNotificationChannels(listOf(gameChannel, reminderChannel))
        }
    }

    fun showYourTurn(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        val notif = NotificationCompat.Builder(context, Constants.CHANNEL_ID_GAME)
            .setSmallIcon(SMALL_ICON)
            .setContentTitle("Your Turn!")
            .setContentText("It's your turn in the card game!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(Constants.NOTIF_ID_TURN, notif)
    }

    fun showDailyReminder(context: Context) {
        val pending = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, Constants.CHANNEL_ID_REMINDERS)
            .setSmallIcon(SMALL_ICON)
            .setContentTitle("Time to play!")
            .setContentText("Don't forget your daily card game.")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        NotificationManagerCompat.from(context)
            .notify(Constants.NOTIF_ID_REMINDER, notif)
    }
}
