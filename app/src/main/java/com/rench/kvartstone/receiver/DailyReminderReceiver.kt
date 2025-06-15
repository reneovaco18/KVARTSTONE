
package com.rench.kvartstone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rench.kvartstone.notification.NotificationHelper
import com.rench.kvartstone.core.Constants

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Constants.ALARM_ACTION == intent.action) {
            NotificationHelper.showDailyReminder(context)
        }
    }
}
