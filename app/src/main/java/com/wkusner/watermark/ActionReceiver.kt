package com.wkusner.watermark

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

/** Handles "Drank" and "Snooze" from notifications, and taps on the complication. */
class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val DRINK = "com.wkusner.watermark.DRINK"
        const val SNOOZE = "com.wkusner.watermark.SNOOZE"
        private const val SNOOZE_MIN = 30
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            DRINK -> Nudger.drink(ctx)
            SNOOZE -> {
                Store(ctx).snoozeUntil = System.currentTimeMillis() + SNOOZE_MIN * 60_000L
                NotificationManagerCompat.from(ctx).cancel(Nudger.NOTIF_ID)
            }
        }
    }
}
