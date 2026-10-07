package org.myfastingapp.app.domain

data class FastSession(
    val id: Long,
    val planId: String,
    val planName: String,
    val targetSeconds: Long,
    val startEpochMillis: Long,
    val endEpochMillis: Long?,
    val createdEpochMillis: Long,
    val updatedEpochMillis: Long,
) {
    val isActive: Boolean = endEpochMillis == null
    val targetMinutes: Int = (targetSeconds / 60L).toInt()
    val targetMillis: Long = targetSeconds.coerceAtLeast(1L) * 1_000L

    fun durationMillis(nowMillis: Long): Long {
        val effectiveEnd = endEpochMillis ?: nowMillis
        return (effectiveEnd - startEpochMillis).coerceAtLeast(0L)
    }

    /**
     * Duration the fast was planned for: a built-in plan's nominal length, or the
     * stored target for custom, manual, and imported sessions.
     */
    val plannedSeconds: Long
        get() = FastPlans.builtInById(planId)?.fastingMinutes?.times(60L) ?: targetSeconds

    /**
     * How the fast compares with the plan it began with, used for history
     * colouring: met the plan, reached at least 85% of it, or fell short.
     */
    fun planOutcome(nowMillis: Long = System.currentTimeMillis()): FastPlanOutcome {
        val plannedMillis = plannedSeconds.coerceAtLeast(1L) * 1_000L
        val actualMillis = durationMillis(nowMillis)
        return when {
            actualMillis >= plannedMillis -> FastPlanOutcome.Met
            actualMillis * 100L >= plannedMillis * 85L -> FastPlanOutcome.Near
            else -> FastPlanOutcome.Short
        }
    }

    /**
     * Display name for the session. Custom fasts show the duration split of the
     * plan they began with (e.g. "15:9") instead of "Custom"; a fast that ran
     * longer than planned keeps its planned label, since the actual elapsed time
     * is shown separately.
     */
    val displayPlanName: String
        get() = if (planId == FastPlans.CUSTOM_ID) customSplitLabel(targetMinutes.toLong()) else planName
}

/** Result of a fast against the plan it began with. */
enum class FastPlanOutcome {
    /** Planned duration reached or exceeded. */
    Met,

    /** At least 85% of the planned duration reached. */
    Near,

    /** Less than 85% of the planned duration. */
    Short,
}
