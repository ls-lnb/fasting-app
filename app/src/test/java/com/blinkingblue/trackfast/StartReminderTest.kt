package com.blinkingblue.trackfast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.blinkingblue.trackfast.domain.StartReminder
import java.time.LocalDateTime
import java.time.ZoneId

class StartReminderTest {
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun firesLaterTheSameDayWhileTheTimeIsStillAhead() {
        val trigger = StartReminder.nextTriggerAt(
            nowEpochMillis = at(day = 15, hour = 21, minute = 0),
            hour = 22,
            minute = 0,
            zone = zone,
        )

        assertEquals(at(day = 15, hour = 22, minute = 0), trigger)
    }

    @Test
    fun rollsToTomorrowOnceTheTimeHasPassed() {
        // Exactly on the hour counts as passed, so the next fire is tomorrow.
        val trigger = StartReminder.nextTriggerAt(
            nowEpochMillis = at(day = 15, hour = 22, minute = 0),
            hour = 22,
            minute = 0,
            zone = zone,
        )

        assertEquals(at(day = 16, hour = 22, minute = 0), trigger)
    }

    @Test
    fun honoursAMinuteLevelTime() {
        val trigger = StartReminder.nextTriggerAt(
            nowEpochMillis = at(day = 15, hour = 21, minute = 45),
            hour = 22,
            minute = 30,
            zone = zone,
        )

        assertEquals(at(day = 15, hour = 22, minute = 30), trigger)
    }

    @Test
    fun defaultsToTenInTheEvening() {
        val trigger = StartReminder.nextTriggerAt(
            nowEpochMillis = at(day = 15, hour = 8, minute = 0),
            zone = zone,
        )

        assertEquals(at(day = 15, hour = 22, minute = 0), trigger)
    }

    @Test
    fun outOfRangeTimesAreClamped() {
        val trigger = StartReminder.nextTriggerAt(
            nowEpochMillis = at(day = 15, hour = 8, minute = 0),
            hour = 25,
            minute = -10,
            zone = zone,
        )

        assertEquals(at(day = 15, hour = 23, minute = 0), trigger)
    }

    @Test
    fun onlyRemindsWhenSwitchedOnAndNotAlreadyFasting() {
        assertTrue(StartReminder.shouldRemind(startReminderEnabled = true, hasActiveFast = false))
        assertFalse(StartReminder.shouldRemind(startReminderEnabled = true, hasActiveFast = true))
        assertFalse(StartReminder.shouldRemind(startReminderEnabled = false, hasActiveFast = false))
    }

    private fun at(day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 10, day, hour, minute).atZone(zone).toInstant().toEpochMilli()
}
