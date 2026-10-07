package org.myfastingapp.app.domain

data class FastPlan(
    val id: String,
    val name: String,
    val fastingMinutes: Int,
    val eatingMinutes: Int? = null,
) {
    val label: String
        get() = eatingMinutes?.let { "$name (${fastingMinutes / 60}:${it / 60})" } ?: name

    /** Display name; custom plans show their duration split, e.g. "15:9". */
    val displayLabel: String
        get() = if (id == FastPlans.CUSTOM_ID) customSplitLabel(fastingMinutes.toLong()) else name
}

/**
 * Split label for a custom fast derived from its duration, e.g. 15h09 ->
 * "15:9", matching the built-in plan naming (fasting hours : remaining hours
 * of the cycle). The duration is rounded to the nearest hour (30 minutes and
 * above rounds up); multi-day fasts use a 48-hour cycle, e.g. 25h -> "25:23".
 */
fun customSplitLabel(totalMinutes: Long): String {
    val roundedHours = ((totalMinutes.coerceAtLeast(0L) + 30L) / 60L).coerceAtLeast(1L)
    val cycleHours = ((roundedHours + 23L) / 24L) * 24L
    return "$roundedHours:${cycleHours - roundedHours}"
}

object FastPlans {
    const val CUSTOM_ID = "custom"
    const val DEFAULT_ID = "16_8"
    const val DEFAULT_CUSTOM_MINUTES = 16 * 60

    val builtIns = listOf(
        FastPlan("13_11", "13:11", 13 * 60, 11 * 60),
        FastPlan(DEFAULT_ID, "16:8", 16 * 60, 8 * 60),
        FastPlan("18_6", "18:6", 18 * 60, 6 * 60),
        FastPlan("20_4", "20:4", 20 * 60, 4 * 60),
        FastPlan("omad", "OMAD", 23 * 60, 60),
        FastPlan("24h", "24h", 24 * 60),
    )

    fun builtInById(id: String): FastPlan? = builtIns.firstOrNull { it.id == id }

    fun resolve(id: String, customFastingMinutes: Int): FastPlan {
        return builtInById(id)
            ?: FastPlan(CUSTOM_ID, "Custom", customFastingMinutes.coerceIn(30, 7 * 24 * 60))
    }
}
