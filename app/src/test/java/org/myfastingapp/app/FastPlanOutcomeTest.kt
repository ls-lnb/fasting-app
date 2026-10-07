package org.myfastingapp.app

import org.junit.Assert.assertEquals
import org.junit.Test
import org.myfastingapp.app.domain.FastPlanOutcome
import org.myfastingapp.app.domain.FastPlans
import org.myfastingapp.app.domain.FastSession

/** History colouring compares each fast with the plan it began with. */
class FastPlanOutcomeTest {
    private val hourSeconds = 3_600L
    private val hourMillis = 3_600_000L

    @Test
    fun reachingThePlanIsMet() {
        assertEquals(
            FastPlanOutcome.Met,
            session(targetSeconds = 16 * hourSeconds, end = 16 * hourMillis).planOutcome(),
        )
    }

    @Test
    fun overrunningThePlanStaysMet() {
        assertEquals(
            FastPlanOutcome.Met,
            session(targetSeconds = 16 * hourSeconds, end = 17 * hourMillis).planOutcome(),
        )
    }

    @Test
    fun exactlyEightyFivePercentIsNear() {
        // 85% of a 16h plan is 13h36m.
        assertEquals(
            FastPlanOutcome.Near,
            session(targetSeconds = 16 * hourSeconds, end = 13 * hourMillis + 36 * 60_000L).planOutcome(),
        )
    }

    @Test
    fun justBelowEightyFivePercentIsShort() {
        assertEquals(
            FastPlanOutcome.Short,
            session(targetSeconds = 16 * hourSeconds, end = 13 * hourMillis + 35 * 60_000L).planOutcome(),
        )
    }

    @Test
    fun shorterAndLongerPlansScaleWithTheirOwnTarget() {
        assertEquals(
            FastPlanOutcome.Met,
            session(targetSeconds = 13 * hourSeconds, end = 13 * hourMillis).planOutcome(),
        )
        assertEquals(
            FastPlanOutcome.Short,
            session(targetSeconds = 13 * hourSeconds, end = 10 * hourMillis).planOutcome(),
        )
        assertEquals(
            FastPlanOutcome.Near,
            session(targetSeconds = 24 * hourSeconds, end = 21 * hourMillis).planOutcome(),
        )
    }

    @Test
    fun builtInPlanUsesItsNominalDuration() {
        // A legacy row whose stored target drifted still compares against the plan.
        val session = session(planId = "16_8", targetSeconds = 14 * hourSeconds, end = 16 * hourMillis)

        assertEquals(16 * hourSeconds, session.plannedSeconds)
        assertEquals(FastPlanOutcome.Met, session.planOutcome())
    }

    @Test
    fun activeFastTracksProgressTowardsItsPlan() {
        val active = session(targetSeconds = 16 * hourSeconds, end = null)

        assertEquals(FastPlanOutcome.Short, active.planOutcome(10 * hourMillis))
        assertEquals(FastPlanOutcome.Near, active.planOutcome(14 * hourMillis))
        assertEquals(FastPlanOutcome.Met, active.planOutcome(16 * hourMillis))
    }

    private fun session(
        planId: String = FastPlans.CUSTOM_ID,
        targetSeconds: Long,
        end: Long?,
    ) = FastSession(
        id = 1L,
        planId = planId,
        planName = "Custom",
        targetSeconds = targetSeconds,
        startEpochMillis = 0L,
        endEpochMillis = end,
        createdEpochMillis = 0L,
        updatedEpochMillis = 0L,
    )
}
