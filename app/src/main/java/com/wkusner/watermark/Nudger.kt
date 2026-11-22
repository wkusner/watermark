package com.wkusner.watermark

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object Nudger {
    private const val CH_GENTLE = "gentle"
    private const val CH_FIRM = "firm"
    const val NOTIF_ID = 1

    /** Minutes between Rising Tide steps, and between repeats when Rising Tide is off. */
    private const val ESCALATE_AFTER_MIN = 20
    private const val REPEAT_AFTER_MIN = 45

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_GENTLE, "Gentle nudges", NotificationManager.IMPORTANCE_DEFAULT).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_FIRM, "Firm nudges (Rising Tide)", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
            }
        )
    }

    /** Called every ~15 minutes by NudgeWorker. */
    fun check(ctx: Context) {
        val s = Store(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java)
        val now = System.currentTimeMillis()

        // Bedtime mode / Do Not Disturb: stay silent.
        if (nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return
        if (now < s.snoozeUntil) return

        val t = LocalTime.now()
        val nowMin = t.hour * 60 + t.minute
        val wakeToday = LocalDate.now()
            .atTime(s.wakeMin / 60, s.wakeMin % 60)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val sinceDrinkMin = (now - maxOf(s.lastDrinkAt, wakeToday)) / 60_000

        val due = Pace.isDue(
            totalMl = s.totalMl(), goalMl = s.goalMl, glassMl = s.glassMl,
            wakeMin = s.wakeMin, cutoffMin = s.cutoffMin, nowMin = nowMin,
            minutesSinceDrink = sinceDrinkMin, recentWindowMin = s.recentWindowMin,
        )
        if (!due) {
            if (s.lastNudgeAt != 0L) { s.lastNudgeAt = 0L; s.escalation = 0 }
            NotificationManagerCompat.from(ctx).cancel(NOTIF_ID)
            return
        }

        val sinceNudgeMin = (now - s.lastNudgeAt) / 60_000
        val level = when {
            s.lastNudgeAt == 0L -> 0
            s.risingTide && sinceNudgeMin >= ESCALATE_AFTER_MIN -> minOf(s.escalation + 1, 2)
            !s.risingTide && sinceNudgeMin >= REPEAT_AFTER_MIN -> 0
            else -> return
        }
        s.escalation = level
        s.lastNudgeAt = now
        post(ctx, level, s.totalMl(), s.goalMl)
    }

    @SuppressLint("MissingPermission")
    private fun post(ctx: Context, level: Int, total: Int, goal: Int) {
        val nmc = NotificationManagerCompat.from(ctx)
        if (!nmc.areNotificationsEnabled()) return

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val drink = PendingIntent.getBroadcast(
            ctx, 1, Intent(ctx, ActionReceiver::class.java).setAction(ActionReceiver.DRINK), flags
        )
        val snooze = PendingIntent.getBroadcast(
            ctx, 2, Intent(ctx, ActionReceiver::class.java).setAction(ActionReceiver.SNOOZE), flags
        )
        val open = PendingIntent.getActivity(
            ctx, 3, Intent(ctx, NudgeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags
        )
        val text = when (level) {
            0 -> "Time for some water"
            1 -> "Still behind today. A glass?"
            else -> "Water break. Log a glass to stop these."
        }
        val n = NotificationCompat.Builder(ctx, if (level == 0) CH_GENTLE else CH_FIRM)
            .setSmallIcon(R.drawable.ic_water)
            .setContentTitle("$total / $goal ml")
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (level == 0) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "Drank", drink)
            .addAction(0, "Snooze 30m", snooze)
            .apply { if (level >= 2) setFullScreenIntent(open, true) }
            .build()
        nmc.notify(NOTIF_ID, n)
    }

    /** Log a drink from anywhere (tile, complication, app, notification) and refresh everything. */
    fun drink(ctx: Context) {
        Store(ctx).addDrink()
        NotificationManagerCompat.from(ctx).cancel(NOTIF_ID)
        refresh(ctx)
    }

    fun refresh(ctx: Context, includeTile: Boolean = true) {
        if (includeTile) TileService.getUpdater(ctx).requestUpdate(WatermarkTileService::class.java)
        ComplicationDataSourceUpdateRequester
            .create(ctx, ComponentName(ctx, WaterComplicationService::class.java))
            .requestUpdateAll()
    }
}
