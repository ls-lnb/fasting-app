package org.myfastingapp.app

import org.junit.Assert.assertEquals
import org.junit.Test
import org.myfastingapp.app.domain.FastPlans
import org.myfastingapp.app.domain.FastSession
import org.myfastingapp.app.domain.customSplitLabel

/** Custom fasts display as a duration split ("15:9") instead of "Custom". */
class CustomSplitLabelTest {
    @Test
    fun durationBelowThirtyMinutesRoundsDown() {
        assertEquals("15:9", customSplitLabel(15L * 60L + 29L))
    }

    @Test
    fun durationAtThirtyMinutesRoundsUp() {
        assertEquals("16:8", customSplitLabel(15L * 60L + 30L))
    }

    @Test
    fun wholeHoursMatchBuiltInPlanNaming() {
        assertEquals("13:11", customSplitLabel(13L * 60L))
        assertEquals("16:8", customSplitLabel(16L * 60L))
        assertEquals("18:6", customSplitLabel(18L * 60L))
    }

    @Test
    fun exactDayIsFullCycle() {
        assertEquals("24:0", customSplitLabel(24L * 60L))
    }

    @Test
    fun multiDayFastUsesExtendedCycle() {
        assertEquals("25:23", customSplitLabel(25L * 60L))
        assertEquals("26:22", customSplitLabel(26L * 60L + 10L))
    }

    @Test
    fun minimumFastRoundsToAtLeastOneHour() {
        assertEquals("1:23", customSplitLabel(30L))
    }

    @Test
    fun activeCustomSessionUsesPlannedTarget() {
        val session = session(planId = FastPlans.CUSTOM_ID, planName = "Custom", targetSeconds = 16 * 3600L, end = null)

        assertEquals("16:8", session.displayPlanName)
    }

    @Test
    fun completedCustomSessionUsesActualDuration() {
        val session = session(
            planId = FastPlans.CUSTOM_ID,
            planName = "Custom",
            targetSeconds = 16 * 3600L,
            end = 15L * 3600_000L + 9L * 60_000L,
        )

        assertEquals("15:9", session.displayPlanName)
    }

    @Test
    fun builtInAndManualSessionsKeepTheirNames() {
        assertEquals(
            "16:8",
            session(planId = "16_8", planName = "16:8", targetSeconds = 16 * 3600L, end = 16 * 3600_000L).displayPlanName,
        )
        assertEquals(
            "Morning fast",
            session(planId = "manual", planName = "Morning fast", targetSeconds = 3600L, end = 3600_000L).displayPlanName,
        )
    }

    private fun session(
        planId: String,
        planName: String,
        targetSeconds: Long,
        end: Long?,
    ) = FastSession(
        id = 1L,
        planId = planId,
        planName = planName,
        targetSeconds = targetSeconds,
        startEpochMillis = 0L,
        endEpochMillis = end,
        createdEpochMillis = 0L,
        updatedEpochMillis = 0L,
    )
}
