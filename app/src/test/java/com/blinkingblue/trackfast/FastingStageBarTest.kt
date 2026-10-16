package com.blinkingblue.trackfast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.blinkingblue.trackfast.domain.FastingPhases

/** The stage bar splits a fasting window proportionally across the fasting phases. */
class FastingStageBarTest {
    private val hourMillis = 3_600_000L
    private val hourSeconds = 3_600L

    @Test
    fun sixteenHourWindowCutsTheThirdStageShort() {
        val stages = FastingPhases.stagesForWindow(16 * hourSeconds)

        assertEquals(listOf("0-4h", "4-12h", "12-16h"), stages.map { it.label })
        assertEquals(listOf("Food energy", "Stored glucose", "Fat switch"), stages.map { it.phase.title })
        assertEquals(listOf(4.0, 8.0, 4.0), stages.map { it.hours })
    }

    @Test
    fun twentyFourHourWindowCoversFourWholeStages() {
        val stages = FastingPhases.stagesForWindow(24 * hourSeconds)

        assertEquals(listOf(4.0, 8.0, 6.0, 6.0), stages.map { it.hours })
        assertEquals("Fat burning", stages.last().phase.title)
        assertEquals(24.0, stages.sumOf { it.hours }, 0.001)
    }

    @Test
    fun windowBeyondADayGivesTheLastStageTheRemainder() {
        val stages = FastingPhases.stagesForWindow(30 * hourSeconds)

        assertEquals(listOf("0-4h", "4-12h", "12-18h", "18-24h", "24-30h"), stages.map { it.label })
        assertEquals("Extended fast", stages.last().phase.title)
    }

    @Test
    fun shortWindowKeepsOnlyTheStagesItReaches() {
        val stages = FastingPhases.stagesForWindow(8 * hourSeconds)

        assertEquals(listOf("0-4h", "4-8h"), stages.map { it.label })
        assertEquals(8.0, stages.sumOf { it.hours }, 0.001)
    }

    @Test
    fun stageFillsProportionallyWithElapsedTime() {
        val stages = FastingPhases.stagesForWindow(16 * hourSeconds)
        val storedGlucose = stages[1]

        assertEquals(0f, storedGlucose.fillAt(4 * hourMillis), 0.001f)
        assertEquals(0.5f, storedGlucose.fillAt(8 * hourMillis), 0.001f)
        assertEquals(1f, storedGlucose.fillAt(12 * hourMillis), 0.001f)
        assertEquals(1f, storedGlucose.fillAt(20 * hourMillis), 0.001f)
    }

    @Test
    fun stagesAheadOfTheProgressStayEmpty() {
        val stages = FastingPhases.stagesForWindow(24 * hourSeconds)

        stages.forEach { stage ->
            val fillAtSixHours = stage.fillAt(6 * hourMillis)
            if (stage.startHour >= 6.0) {
                assertEquals(0f, fillAtSixHours, 0.001f)
            }
        }
    }

    @Test
    fun nextStageFollowsTheCurrentOne() {
        // At 0h the first stage is current, so the next one is Stored glucose.
        assertEquals("Stored glucose", FastingPhases.nextForElapsed(0L)?.title)
        assertEquals("Stored glucose", FastingPhases.nextForElapsed(2 * hourMillis)?.title)
        assertEquals("Fat switch", FastingPhases.nextForElapsed(6 * hourMillis)?.title)
        assertEquals("Fat burning", FastingPhases.nextForElapsed(14 * hourMillis)?.title)
        assertEquals("Extended fast", FastingPhases.nextForElapsed(20 * hourMillis)?.title)
        assertNull(FastingPhases.nextForElapsed(26 * hourMillis))
    }

    @Test
    fun windowStaysThePlanWhileTheFastIsInsideIt() {
        val planned = 16 * hourSeconds

        assertEquals(planned, FastingPhases.stageBarWindowSeconds(planned, 0L))
        assertEquals(planned, FastingPhases.stageBarWindowSeconds(planned, 1 * hourMillis))
        assertEquals(planned, FastingPhases.stageBarWindowSeconds(planned, 15 * hourMillis))
        assertEquals(planned, FastingPhases.stageBarWindowSeconds(planned, 16 * hourMillis))
        // One second past the plan the bar re-bases to the stage in progress (12-18h),
        // so it stops sitting full for the rest of the fast.
        assertEquals(18 * hourSeconds, FastingPhases.stageBarWindowSeconds(planned, 16 * hourMillis + 1_000L))
    }

    @Test
    fun windowRebasesToTheStageInProgressAfterThePlan() {
        val planned = 16 * hourSeconds

        // 17h into a 16h plan: still in the 12-18h stage, so the bar spans 18h.
        assertEquals(18 * hourSeconds, FastingPhases.stageBarWindowSeconds(planned, 17 * hourMillis))
        // Past 18h: the 18-24h stage is in progress, so the bar spans 24h.
        assertEquals(24 * hourSeconds, FastingPhases.stageBarWindowSeconds(planned, 19 * hourMillis))
    }

    @Test
    fun openEndedFinalStageIsDrawnToTheNextHour() {
        val planned = 16 * hourSeconds

        assertEquals(25 * hourSeconds, FastingPhases.stageBarWindowSeconds(planned, 24 * hourMillis + 5 * 60_000L))
        assertEquals(26 * hourSeconds, FastingPhases.stageBarWindowSeconds(planned, 25 * hourMillis + 30 * 60_000L))
    }

    @Test
    fun rebasedWindowShowsOnlyCompletedAndCurrentStages() {
        val window = FastingPhases.stageBarWindowSeconds(16 * hourSeconds, 19 * hourMillis)
        val stages = FastingPhases.stagesForWindow(window)

        assertEquals(listOf("0-4h", "4-12h", "12-18h", "18-24h"), stages.map { it.label })
        assertEquals(1f, stages[0].fillAt(19 * hourMillis), 0.001f)
        assertEquals(1f, stages[1].fillAt(19 * hourMillis), 0.001f)
        assertEquals(1f, stages[2].fillAt(19 * hourMillis), 0.001f)
        // One hour into the 18-24h stage: part way, with nothing drawn ahead of it.
        assertEquals(1f / 6f, stages[3].fillAt(19 * hourMillis), 0.001f)
    }

    @Test
    fun everyPhaseHasInfoForTheStagesView() {
        assertEquals(5, FastingPhases.all.size)
        FastingPhases.all.forEach { phase ->
            assert(phase.title.isNotBlank())
            assert(phase.windowLabel.isNotBlank())
            assert(phase.body.isNotBlank())
            assert(phase.detail.length > 40) { "${phase.title} needs a brief write-up" }
            // Two lines at bodySmall in the stages view: longer text would be clipped.
            assert(phase.detail.length <= 90) { "${phase.title} write-up is too long for the stages view" }
        }
    }
}
