package org.myfastingapp.app.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** One cell of the streak calendar: a date and how the fast started on it went. */
data class StreakDay(
    val date: LocalDate,
    /** Outcome of the best fast starting this date, or null when no fast started. */
    val outcome: FastPlanOutcome?,
) {
    val hasRecord: Boolean get() = outcome != null
}

/** Month caption drawn above the grid, anchored to the week column the month opens in. */
data class StreakMonthLabel(
    /** Index of the week column the caption sits above. */
    val columnIndex: Int,
    /** Columns the caption may span before the next caption starts. */
    val columnsWide: Int,
    val text: String,
)

/**
 * GitHub-style contribution grid: week columns of seven days each, Monday first,
 * plus the month captions that align with those columns.
 */
data class StreakCalendarGrid(
    val weeks: List<List<StreakDay>>,
    val monthLabels: List<StreakMonthLabel>,
)

/**
 * Builds the rolling streak calendar shown at the top of the History tab.
 *
 * The window is the last [WINDOW_MONTHS] calendar months, from the first day of the
 * earliest month through today. A day is coloured by the best fast that *started*
 * on it (plan met beats near beats short); days without a fast starting on them,
 * and day padding outside the window, stay empty.
 */
object StreakCalendarBuilder {
    const val WINDOW_MONTHS = 12

    private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM")

    fun build(
        sessions: List<FastSession>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zoneId),
        nowMillis: Long = System.currentTimeMillis(),
        windowMonths: Int = WINDOW_MONTHS,
    ): StreakCalendarGrid {
        val rangeStart = YearMonth.from(today).minusMonths((windowMonths - 1).toLong()).atDay(1)
        val rangeEnd = today
        val outcomes = bestOutcomesByStart(sessions, zoneId, rangeStart, rangeEnd, nowMillis)

        val gridStart = rangeStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val gridEnd = rangeEnd.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val weeks = mutableListOf<List<StreakDay>>()
        var weekStart = gridStart
        while (!weekStart.isAfter(gridEnd)) {
            weeks += (0L..6L).map { offset ->
                val date = weekStart.plusDays(offset)
                StreakDay(date, outcomes[date])
            }
            weekStart = weekStart.plusDays(7)
        }

        return StreakCalendarGrid(
            weeks = weeks,
            monthLabels = monthLabels(
                gridStart = weeks.first().first().date,
                rangeStart = rangeStart,
                rangeEnd = rangeEnd,
                columns = weeks.size,
            ),
        )
    }

    /** Best outcome per local start date, skipping fasts that began outside the window. */
    private fun bestOutcomesByStart(
        sessions: List<FastSession>,
        zoneId: ZoneId,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        nowMillis: Long,
    ): Map<LocalDate, FastPlanOutcome> {
        val best = mutableMapOf<LocalDate, FastPlanOutcome>()
        sessions.forEach { session ->
            val date = Instant.ofEpochMilli(session.startEpochMillis).atZone(zoneId).toLocalDate()
            if (date.isBefore(rangeStart) || date.isAfter(rangeEnd)) return@forEach
            val outcome = session.planOutcome(nowMillis)
            val current = best[date]
            if (current == null || outcome.intensity > current.intensity) {
                best[date] = outcome
            }
        }
        return best
    }

    /**
     * One caption per month in the window: the first lands on the opening column,
     * each later one lands on the column containing that month's first day, and the
     * width runs up to the column of the following caption.
     */
    private fun monthLabels(
        gridStart: LocalDate,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        columns: Int,
    ): List<StreakMonthLabel> {
        val anchors = mutableListOf<LocalDate>()
        var cursor = rangeStart
        while (!cursor.isAfter(rangeEnd)) {
            anchors += cursor
            cursor = cursor.plusMonths(1)
        }

        fun columnOf(date: LocalDate): Int =
            ChronoUnit.DAYS.between(gridStart, date).toInt() / 7

        return anchors.mapIndexed { index, anchor ->
            val columnIndex = columnOf(anchor)
            val endColumn =
                if (index + 1 < anchors.size) columnOf(anchors[index + 1]) else columns
            StreakMonthLabel(
                columnIndex = columnIndex,
                columnsWide = (endColumn - columnIndex).coerceAtLeast(1),
                text = anchor.format(MONTH_FORMAT),
            )
        }
    }
}

/** Ordering used to let the strongest fast of a day win its cell colour. */
private val FastPlanOutcome.intensity: Int
    get() = when (this) {
        FastPlanOutcome.Short -> 0
        FastPlanOutcome.Near -> 1
        FastPlanOutcome.Met -> 2
    }
