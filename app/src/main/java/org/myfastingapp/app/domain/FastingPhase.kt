package org.myfastingapp.app.domain

import kotlin.math.floor

data class FastingPhase(
    val title: String,
    val windowLabel: String,
    val body: String,
    val detail: String,
    val colorArgb: Int,
)

/** One stage of a fasting window, with the hours it covers in that window. */
data class FastingStage(
    val phase: FastingPhase,
    val startHour: Double,
    val endHour: Double,
) {
    val hours: Double = (endHour - startHour).coerceAtLeast(0.0)

    /** Fraction of this stage already fasted at [elapsedMillis], clamped to 0f..1f. */
    fun fillAt(elapsedMillis: Long): Float {
        val elapsedHours = elapsedMillis / 3_600_000.0
        return ((elapsedHours - startHour) / hours.coerceAtLeast(0.001)).coerceIn(0.0, 1.0).toFloat()
    }

    /** Hour range within the window, e.g. "0-4h" or "12-16h". */
    val label: String
        get() = "${hourText(startHour)}-${hourText(endHour)}h"
}

object FastingPhases {
    const val FOOD_ENERGY = "Food energy"
    const val STORED_GLUCOSE = "Stored glucose"
    const val FAT_SWITCH = "Fat switch"
    const val FAT_BURNING = "Fat burning"
    const val EXTENDED_FAST = "Extended fast"

    /** Stage start hours matching [forElapsed]; each stage runs until the next entry. */
    private val STAGE_BOUNDS = doubleArrayOf(0.0, 4.0, 12.0, 18.0, 24.0)

    /** All stages in order, for the stages info view. */
    val all: List<FastingPhase> = listOf(
        forElapsed(0L),
        forElapsed(4L * 3_600_000L),
        forElapsed(12L * 3_600_000L),
        forElapsed(18L * 3_600_000L),
        forElapsed(24L * 3_600_000L),
    )

    /**
     * The fasting stages inside a window of [plannedSeconds], proportional to the
     * hours each covers, for the stage progress bar. A stage the window cuts short
     * ends at the window instead of its natural boundary.
     */
    fun stagesForWindow(plannedSeconds: Long): List<FastingStage> {
        val windowHours = plannedSeconds.coerceAtLeast(60L) / 3_600.0
        val stages = mutableListOf<FastingStage>()
        STAGE_BOUNDS.forEachIndexed { index, startHour ->
            if (startHour >= windowHours) return@forEachIndexed
            val endHour = minOf(STAGE_BOUNDS.getOrNull(index + 1) ?: windowHours, windowHours)
            if (endHour <= startHour) return@forEachIndexed
            stages += FastingStage(
                phase = forElapsed((startHour * 3_600_000.0).toLong()),
                startHour = startHour,
                endHour = endHour,
            )
        }
        return stages
    }

    /**
     * Window the stage bar spans: the plan itself, and once the fast runs past the
     * plan, the end of the stage in progress instead - so the bar shows the stages
     * completed so far plus the one in progress, rather than sitting full. The
     * open-ended final stage (24h+) is drawn up to the next whole hour.
     */
    fun stageBarWindowSeconds(plannedSeconds: Long, elapsedMillis: Long): Long {
        val plan = plannedSeconds.coerceAtLeast(60L)
        if (elapsedMillis <= plan * 1_000L) return plan
        val elapsedHours = elapsedMillis / 3_600_000.0
        val endHours = STAGE_BOUNDS.firstOrNull { it > elapsedHours } ?: (floor(elapsedHours) + 1.0)
        return maxOf(plan, (endHours * 3_600.0).toLong())
    }

    /** The stage that follows the one in progress, or null once the last stage is reached. */
    fun nextForElapsed(elapsedMillis: Long): FastingPhase? {
        val elapsedHours = elapsedMillis / 3_600_000.0
        val boundary = STAGE_BOUNDS.firstOrNull { it > elapsedHours } ?: return null
        return forElapsed((boundary * 3_600_000.0).toLong())
    }

    fun forElapsed(elapsedMillis: Long): FastingPhase {
        val elapsedHours = elapsedMillis / 3_600_000.0
        return when {
            elapsedHours < 4.0 -> FastingPhase(
                title = FOOD_ENERGY,
                windowLabel = "0-4h",
                body = "Using your last meal.",
                detail = "Still running on energy from your last meal while insulin settles back.",
                colorArgb = 0xFF2D9CDB.toInt(),
            )
            elapsedHours < 12.0 -> FastingPhase(
                title = STORED_GLUCOSE,
                windowLabel = "4-12h",
                body = "Tapping stored fuel.",
                detail = "Liver glycogen is the main fuel; hunger and habit cues are strongest here.",
                colorArgb = 0xFF3230B8.toInt(),
            )
            elapsedHours < 18.0 -> FastingPhase(
                title = FAT_SWITCH,
                windowLabel = "12-18h",
                body = "Fat use is rising.",
                detail = "Glycogen runs low, so fat use rises and ketones start to build up.",
                colorArgb = 0xFF7B3FC8.toInt(),
            )
            elapsedHours < 24.0 -> FastingPhase(
                title = FAT_BURNING,
                windowLabel = "18-24h",
                body = "More energy from fat.",
                detail = "Fat is a major fuel source; ketones keep climbing and hunger often calms.",
                colorArgb = 0xFFE58B2A.toInt(),
            )
            else -> FastingPhase(
                title = EXTENDED_FAST,
                windowLabel = "24h+",
                body = "Past your target.",
                detail = "A longer fast on fat and ketones. Keep fluids and electrolytes up.",
                colorArgb = 0xFFC44536.toInt(),
            )
        }
    }
}

private fun hourText(hours: Double): String =
    if (hours % 1.0 == 0.0) hours.toInt().toString() else "%.1f".format(hours)
