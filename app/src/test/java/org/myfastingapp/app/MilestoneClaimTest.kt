package org.myfastingapp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.myfastingapp.app.data.claimMilestones

/** Dedupe behavior behind milestone notification backfill. */
class MilestoneClaimTest {
    @Test
    fun firstClaimReturnsAllCandidates() {
        val claim = claimMilestones(
            storedSessionId = null,
            storedShown = emptySet(),
            sessionId = 7L,
            candidates = listOf(25, 50),
        )

        assertEquals(listOf(25, 50), claim.pending)
        assertEquals(setOf("7:25", "7:50"), claim.shown)
        assertEquals(7L, claim.sessionId)
    }

    @Test
    fun secondClaimOnlyReturnsNewMilestones() {
        val first = claimMilestones(null, emptySet(), 7L, listOf(25, 50))
        val second = claimMilestones(first.sessionId, first.shown, 7L, listOf(25, 50, 75))

        assertEquals(listOf(75), second.pending)
        assertEquals(setOf("7:25", "7:50", "7:75"), second.shown)
    }

    @Test
    fun alreadyClaimedMilestonesAreNeverReturned() {
        val first = claimMilestones(null, emptySet(), 7L, listOf(25))
        val repeat = claimMilestones(first.sessionId, first.shown, 7L, listOf(25))

        assertTrue(repeat.pending.isEmpty())
        assertEquals(setOf("7:25"), repeat.shown)
    }

    @Test
    fun newSessionResetsPreviouslyShownMilestones() {
        val old = claimMilestones(null, emptySet(), 7L, listOf(25, 50, 75))
        val fresh = claimMilestones(old.sessionId, old.shown, 8L, listOf(25))

        assertEquals(listOf(25), fresh.pending)
        assertEquals(setOf("8:25"), fresh.shown)
    }

    @Test
    fun emptyCandidatesClaimNothing() {
        val claim = claimMilestones(null, emptySet(), 7L, emptyList())

        assertTrue(claim.pending.isEmpty())
        assertTrue(claim.shown.isEmpty())
        assertEquals(7L, claim.sessionId)
    }
}
