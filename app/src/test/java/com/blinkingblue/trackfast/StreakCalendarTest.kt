package com.blinkingblue.trackfast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.blinkingblue.trackfast.domain.FastPlanOutcome
import com.blinkingblue.trackfast.domain.FastSession
import com.blinkingblue.trackfast.domain.StreakCalendarBuilder
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
class StreakCalendarTest {
    private val zone = ZoneId.of("America/New_York")
    private val today = LocalDate.of(2026, 10, 14)

    @Test
    fun coversTwelveRollingMonthsOfMondayFirstWeeks() {
        val grid = StreakCalendarBuilder.build(sessions = emptyList(), zoneId = zone, today = today)

        // Window: Nov 1 2025 through Oct 14 2026, padded to whole Monday-first weeks.
        assertEquals(51, grid.weeks.size)
        grid.weeks.forEach { week -> assertEquals(7, week.size) }
        assertEquals(LocalDate.of(2025, 10, 27), grid.weeks.first().first().date)
        assertEquals(LocalDate.of(2026, 10, 18), grid.weeks.last().last().date)
        assertTrue(grid.weeks.all { week -> week.first().date.dayOfWeek == DayOfWeek.MONDAY })
        assertTrue(grid.weeks.flatten().all { !it.hasRecord })
    }

    @Test
    fun captionsEveryMonthInTheWindow() {
        val grid = StreakCalendarBuilder.build(sessions = emptyList(), zoneId = zone, today = today)

        assertEquals(
            listOf("Nov", "Dec", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct"),
            grid.monthLabels.map { it.text },
        )
        assertEquals(0, grid.monthLabels.first().columnIndex)
        grid.monthLabels.zipWithNext { current, next ->
            assertEquals(current.columnIndex + current.columnsWide, next.columnIndex)
            assertTrue(next.columnIndex > current.columnIndex)
        }
        val last = grid.monthLabels.last()
        assertEquals(grid.weeks.size, last.columnIndex + last.columnsWide)
    }

    @Test
    fun fastIsCountedOnItsStartDateNotItsEnd() {
        // Starts late on Oct 13 and ends the next afternoon: only Oct 13 lights up.
        val start = LocalDateTime.of(2026, 10, 13, 23, 30).atZone(zone).toInstant().toEpochMilli()
        val grid = StreakCalendarBuilder.build(
            sessions = listOf(
                session(
                    startMillis = start,
                    endMillis = start + 16 * 60 * 60_000L,
                    targetSeconds = 16 * 60L * 60L,
                ),
            ),
            zoneId = zone,
            today = today,
        )

        val byDate = grid.weeks.flatten().associateBy { it.date }
        assertEquals(FastPlanOutcome.Met, byDate.getValue(LocalDate.of(2026, 10, 13)).outcome)
        assertNull(byDate.getValue(LocalDate.of(2026, 10, 14)).outcome)
    }

    @Test
    fun bestOutcomeWinsWhenSeveralFastsStartTheSameDay() {
        val day = LocalDateTime.of(2026, 9, 20, 6, 0).atZone(zone)
        val short = session(
            startMillis = day.toInstant().toEpochMilli(),
            endMillis = day.plusHours(8).toInstant().toEpochMilli(),
            targetSeconds = 16 * 60L * 60L,
        )
        val met = session(
            startMillis = day.plusHours(4).toInstant().toEpochMilli(),
            endMillis = day.plusHours(4).plusHours(16).toInstant().toEpochMilli(),
            targetSeconds = 16 * 60L * 60L,
        )

        val grid = StreakCalendarBuilder.build(
            sessions = listOf(short, met),
            zoneId = zone,
            today = today,
        )

        val cell = grid.weeks.flatten().first { it.date == LocalDate.of(2026, 9, 20) }
        assertEquals(FastPlanOutcome.Met, cell.outcome)
    }

    @Test
    fun fastsStartingBeforeTheWindowAreLeftOut() {
        val inWindow = LocalDateTime.of(2025, 11, 1, 5, 0).atZone(zone)
        val before = LocalDateTime.of(2025, 10, 31, 5, 0).atZone(zone)

        val grid = StreakCalendarBuilder.build(
            sessions = listOf(
                session(
                    startMillis = inWindow.toInstant().toEpochMilli(),
                    endMillis = inWindow.plusHours(16).toInstant().toEpochMilli(),
                    targetSeconds = 16 * 60L * 60L,
                ),
                session(
                    startMillis = before.toInstant().toEpochMilli(),
                    endMillis = before.plusHours(16).toInstant().toEpochMilli(),
                    targetSeconds = 16 * 60L * 60L,
                ),
            ),
            zoneId = zone,
            today = today,
        )

        val byDate = grid.weeks.flatten().associateBy { it.date }
        assertEquals(FastPlanOutcome.Met, byDate.getValue(LocalDate.of(2025, 11, 1)).outcome)
        assertNull(byDate.getValue(LocalDate.of(2025, 10, 31)).outcome)
        assertFalse(byDate.getValue(LocalDate.of(2025, 10, 31)).hasRecord)
    }

    @Test
    fun paddingDaysOutsideTheWindowStayEmptyEvenWithFastRecords() {
        val sessions = (0..6).map { offset ->
            val date = LocalDate.of(2026, 10, 15).plusDays(offset.toLong())
            val start = date.atTime(6, 0).atZone(zone)
            session(
                startMillis = start.toInstant().toEpochMilli(),
                endMillis = start.plusHours(16).toInstant().toEpochMilli(),
                targetSeconds = 16 * 60L * 60L,
            )
        }

        val grid = StreakCalendarBuilder.build(sessions = sessions, zoneId = zone, today = today)

        val afterToday = grid.weeks.flatten().filter { it.date.isAfter(today) }
        assertFalse(afterToday.isEmpty())
        assertTrue(afterToday.all { !it.hasRecord })
        assertEquals(
            LocalDate.of(2026, 10, 14),
            grid.weeks.flatten().last { !it.date.isAfter(today) }.date,
        )
    }

    @Test
    fun currentMonthCaptionSitsOverItsOpeningWeek() {
        val grid = StreakCalendarBuilder.build(sessions = emptyList(), zoneId = zone, today = today)

        val october = grid.monthLabels.last()
        assertEquals("Oct", october.text)
        val openingColumn = grid.weeks.indexOfFirst { week ->
            week.any { it.date == LocalDate.of(2026, 10, 1) }
        }
        assertEquals(openingColumn, october.columnIndex)
    }

    private fun session(startMillis: Long, endMillis: Long?, targetSeconds: Long): FastSession =
        FastSession(
            id = 0L,
            planId = "16_8",
            planName = "16:8",
            targetSeconds = targetSeconds,
            startEpochMillis = startMillis,
            endEpochMillis = endMillis,
            createdEpochMillis = startMillis,
            updatedEpochMillis = endMillis ?: startMillis,
        )
}
