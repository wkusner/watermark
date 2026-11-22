package com.wkusner.watermark

/**
 * The need-based rule, kept pure so it is easy to test and tune.
 * Times are minutes after midnight.
 */
object Pace {

    /** How much should have been drunk by now: the goal spread evenly from wake to cutoff. */
    fun expectedMl(goalMl: Int, wakeMin: Int, cutoffMin: Int, nowMin: Int): Int {
        if (cutoffMin <= wakeMin) return 0
        val f = ((nowMin - wakeMin).toDouble() / (cutoffMin - wakeMin)).coerceIn(0.0, 1.0)
        return (goalMl * f).toInt()
    }

    fun isQuietHours(wakeMin: Int, cutoffMin: Int, nowMin: Int): Boolean =
        nowMin < wakeMin || nowMin >= cutoffMin

    /**
     * Nudge only when: awake hours, goal not met, at least a glass behind pace,
     * and nothing logged within the recent window.
     */
    fun isDue(
        totalMl: Int,
        goalMl: Int,
        glassMl: Int,
        wakeMin: Int,
        cutoffMin: Int,
        nowMin: Int,
        minutesSinceDrink: Long,
        recentWindowMin: Int,
    ): Boolean {
        if (isQuietHours(wakeMin, cutoffMin, nowMin)) return false
        if (totalMl >= goalMl) return false
        val behind = expectedMl(goalMl, wakeMin, cutoffMin, nowMin) - totalMl
        if (behind < glassMl) return false
        return minutesSinceDrink >= recentWindowMin
    }
}
