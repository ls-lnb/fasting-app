package org.myfastingapp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.myfastingapp.app.domain.FastSession
import org.myfastingapp.app.domain.UserSettings
import org.myfastingapp.app.notify.FastAlarmKind
import org.myfastingapp.app.notify.planFastAlarms

class FastReminderSchedulerTest {
    @Test
    fun alarmPlanIncludesPeriodicRefreshMilestonesAndPhases() {
        val alarms = planFastAlarms(session(), UserSettings(), nowEpochMillis = HOUR_MILLIS)

        val milestones = alarms.filter { it.kind == FastAlarmKind.MILESTONE }
        val phaseUpdates = alarms.filter { it.kind == FastAlarmKind.PHASE_UPDATE }
        val refresh = alarms.single { it.kind == FastAlarmKind.REFRESH }

        assertEquals(listOf(25, 50, 75, 90, 95, 100), milestones.map { it.milestonePercent })
        assertTrue(milestones.all { it.wakeDevice })
        assertEquals(listOf(4, 12, 18, 24), phaseUpdates.map { it.phaseHour })
        assertTrue(phaseUpdates.all { !it.wakeDevice })
        assertFalse(alarms.any { it.kind == FastAlarmKind.TARGET_REMINDER })
        // Periodic refresh keeps the ongoing notification's progress current between events.
        assertEquals(HOUR_MILLIS + 15 * MINUTE_MILLIS, refresh.triggerAtEpochMillis)
        assertTrue(refresh.wakeDevice)
    }

    @Test
    fun periodicRefreshIsReplannedWhenOtherAlarmsElapsed() {
        val alarms = planFastAlarms(session(), UserSettings(), nowEpochMillis = 12 * HOUR_MILLIS)

        val refresh = alarms.single { it.kind == FastAlarmKind.REFRESH }
        assertEquals(12 * HOUR_MILLIS + 15 * MINUTE_MILLIS, refresh.triggerAtEpochMillis)
    }

    @Test
    fun optionalReminderAddsOneWakeupBeforeTarget() {
        val settings = UserSettings(remindersEnabled = true, reminderLeadMinutes = 30)
        val alarms = planFastAlarms(session(), settings, nowEpochMillis = HOUR_MILLIS)

        val reminder = alarms.single { it.kind == FastAlarmKind.TARGET_REMINDER }

        assertTrue(reminder.wakeDevice)
        assertEquals(15 * HOUR_MILLIS + 30 * MINUTE_MILLIS, reminder.triggerAtEpochMillis)
    }

    @Test
    fun milestoneAlertsCanBeTurnedOff() {
        val settings = UserSettings(milestoneAlertsEnabled = false)
        val alarms = planFastAlarms(session(), settings, nowEpochMillis = HOUR_MILLIS)

        assertTrue(alarms.none { it.kind == FastAlarmKind.MILESTONE })
        assertEquals(listOf(4, 12, 18, 24), alarms.filter { it.kind == FastAlarmKind.PHASE_UPDATE }.map { it.phaseHour })
    }

    @Test
    fun onlySelectedMilestonesAreScheduled() {
        val settings = UserSettings(milestonePercents = setOf(50, 90))
        val alarms = planFastAlarms(session(), settings, nowEpochMillis = HOUR_MILLIS)

        assertEquals(listOf(50, 90), alarms.filter { it.kind == FastAlarmKind.MILESTONE }.map { it.milestonePercent })
    }

    @Test
    fun elapsedEventsAreNotRescheduled() {
        val alarms = planFastAlarms(session(), UserSettings(), nowEpochMillis = 12 * HOUR_MILLIS)

        assertEquals(listOf(90, 95, 100), alarms.filter { it.kind == FastAlarmKind.MILESTONE }.map { it.milestonePercent })
        assertEquals(listOf(18, 24), alarms.filter { it.kind == FastAlarmKind.PHASE_UPDATE }.map { it.phaseHour })
    }

    private fun session() = FastSession(
        id = 1L,
        planId = "16_8",
        planName = "16:8",
        targetSeconds = 16 * 60 * 60L,
        startEpochMillis = 0L,
        endEpochMillis = null,
        createdEpochMillis = 0L,
        updatedEpochMillis = 0L,
    )

    private companion object {
        const val MINUTE_MILLIS = 60_000L
        const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    }
}
