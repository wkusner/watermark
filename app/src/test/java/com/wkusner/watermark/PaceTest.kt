package com.wkusner.watermark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaceTest {
    private val wake = 6 * 60 + 30
    private val cutoff = 20 * 60

    @Test fun expectedIsHalfAtMidday() {
        val mid = (wake + cutoff) / 2
        assertEquals(1000, Pace.expectedMl(2000, wake, cutoff, mid))
    }

    @Test fun quietBeforeWakeAndAfterCutoff() {
        assertTrue(Pace.isQuietHours(wake, cutoff, 5 * 60))
        assertTrue(Pace.isQuietHours(wake, cutoff, 21 * 60))
        assertFalse(Pace.isQuietHours(wake, cutoff, 12 * 60))
    }

    @Test fun noNudgeWhenOnPace() {
        assertFalse(Pace.isDue(1000, 2000, 250, wake, cutoff, 13 * 60 + 15, 120, 90))
    }

    @Test fun nudgeWhenBehindAndDry() {
        assertTrue(Pace.isDue(250, 2000, 250, wake, cutoff, 13 * 60, 120, 90))
    }

    @Test fun noNudgeRightAfterADrink() {
        assertFalse(Pace.isDue(250, 2000, 250, wake, cutoff, 13 * 60, 20, 90))
    }

    @Test fun neverAtNight() {
        assertFalse(Pace.isDue(0, 2000, 250, wake, cutoff, 23 * 60, 600, 90))
    }
}
