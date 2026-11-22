package com.wkusner.watermark

import android.content.Context
import java.time.LocalDate

/** All state lives on the watch in SharedPreferences. Resets the daily total at midnight. */
class Store(context: Context) {
    private val p = context.applicationContext
        .getSharedPreferences("watermark", Context.MODE_PRIVATE)

    // Settings
    var goalMl: Int
        get() = p.getInt("goal", 2000)
        set(v) = p.edit().putInt("goal", v.coerceIn(500, 5000)).apply()
    var glassMl: Int
        get() = p.getInt("glass", 250)
        set(v) = p.edit().putInt("glass", v.coerceIn(50, 1000)).apply()
    var wakeMin: Int
        get() = p.getInt("wake", 6 * 60 + 30)
        set(v) = p.edit().putInt("wake", v.coerceIn(0, 24 * 60 - 1)).apply()
    var cutoffMin: Int
        get() = p.getInt("cutoff", 20 * 60)
        set(v) = p.edit().putInt("cutoff", v.coerceIn(0, 24 * 60 - 1)).apply()
    var recentWindowMin: Int
        get() = p.getInt("recent", 90)
        set(v) = p.edit().putInt("recent", v.coerceIn(15, 240)).apply()
    var risingTide: Boolean
        get() = p.getBoolean("rising", true)
        set(v) = p.edit().putBoolean("rising", v).apply()

    // Nudge bookkeeping
    var escalation: Int
        get() = p.getInt("esc", 0)
        set(v) = p.edit().putInt("esc", v).apply()
    var lastNudgeAt: Long
        get() = p.getLong("lastNudge", 0L)
        set(v) = p.edit().putLong("lastNudge", v).apply()
    var snoozeUntil: Long
        get() = p.getLong("snooze", 0L)
        set(v) = p.edit().putLong("snooze", v).apply()
    val lastDrinkAt: Long
        get() = p.getLong("lastDrink", 0L)

    fun totalMl(): Int {
        rollover()
        return p.getInt("total", 0)
    }

    fun addDrink(ml: Int = glassMl) {
        rollover()
        val log = p.getString("log", "") ?: ""
        p.edit()
            .putInt("total", p.getInt("total", 0) + ml)
            .putString("log", if (log.isEmpty()) "$ml" else "$log,$ml")
            .putLong("lastDrink", System.currentTimeMillis())
            .putInt("esc", 0)
            .putLong("lastNudge", 0L)
            .commit()
    }

    /** Removes the most recent drink today. Returns false if there was nothing to undo. */
    fun undo(): Boolean {
        rollover()
        val parts = (p.getString("log", "") ?: "").split(",").filter { it.isNotBlank() }
        if (parts.isEmpty()) return false
        val last = parts.last().toIntOrNull() ?: 0
        p.edit()
            .putInt("total", (p.getInt("total", 0) - last).coerceAtLeast(0))
            .putString("log", parts.dropLast(1).joinToString(","))
            .commit()
        return true
    }

    private fun rollover() {
        val today = LocalDate.now().toString()
        if (p.getString("day", null) != today) {
            p.edit()
                .putString("day", today)
                .putInt("total", 0)
                .putString("log", "")
                .putInt("esc", 0)
                .putLong("lastNudge", 0L)
                .commit()
        }
    }
}
