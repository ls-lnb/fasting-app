package org.myfastingapp.app.domain

import java.time.Instant
import java.time.ZoneId

/**
 * Optional nightly nudge to begin the user's fast, offered alongside the target
 * reminder. The alarm is inexact on purpose (same stance as the nightly auto
 * backup: no exact-alarm permission, no wake locks), and it only posts while no
 * fast is running - a fast already in progress needs no reminder to start.
 */
object StartReminder {
    const val DEFAULT_HOUR = 22
    const val DEFAULT_MINUTE = 0

    fun coerceHour(hour: Int): Int = hour.coerceIn(0, 23)

    fun coerceMinute(minute: Int): Int = minute.coerceIn(0, 59)

    /** Posts only when the reminder is switched on and no fast is running. */
    fun shouldRemind(startReminderEnabled: Boolean, hasActiveFast: Boolean): Boolean =
        startReminderEnabled && !hasActiveFast

    /** Next occurrence of the local reminder time, later today or tomorrow. */
    fun nextTriggerAt(
        nowEpochMillis: Long,
        hour: Int = DEFAULT_HOUR,
        minute: Int = DEFAULT_MINUTE,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val now = Instant.ofEpochMilli(nowEpochMillis).atZone(zone)
        val today = now.toLocalDate()
            .atTime(coerceHour(hour), coerceMinute(minute))
            .atZone(zone)
        val next = if (today.toInstant().isAfter(now.toInstant())) today else today.plusDays(1)
        return next.toInstant().toEpochMilli()
    }
}
