package com.blinkingblue.trackfast

import org.junit.Assert.assertEquals
import org.junit.Test
import com.blinkingblue.trackfast.backup.AutoBackupScheduler
import com.blinkingblue.trackfast.domain.AutoBackup
import java.time.LocalDateTime
import java.time.ZoneId

/** The daily backup alarm is aimed at the next 03:00 local time. */
class AutoBackupScheduleTest {
    private val zone: ZoneId = ZoneId.of("UTC")

    private fun at(hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 10, 13, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun nextRun(hour: Int, minute: Int = 0): LocalDateTime =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(AutoBackupScheduler.nextRunAt(at(hour, minute), zone)), zone)

    @Test
    fun beforeTheRunHourSchedulesToday() {
        assertEquals(LocalDateTime.of(2026, 10, 13, AutoBackup.RUN_HOUR, 0), nextRun(0))
        assertEquals(LocalDateTime.of(2026, 10, 13, AutoBackup.RUN_HOUR, 0), nextRun(AutoBackup.RUN_HOUR - 1, 59))
    }

    @Test
    fun atOrAfterTheRunHourSchedulesTomorrow() {
        assertEquals(LocalDateTime.of(2026, 10, 14, AutoBackup.RUN_HOUR, 0), nextRun(AutoBackup.RUN_HOUR))
        assertEquals(LocalDateTime.of(2026, 10, 14, AutoBackup.RUN_HOUR, 0), nextRun(AutoBackup.RUN_HOUR, 1))
        assertEquals(LocalDateTime.of(2026, 10, 14, AutoBackup.RUN_HOUR, 0), nextRun(23, 30))
    }

    @Test
    fun theRunHourIsThreeInTheMorning() {
        assertEquals(3, AutoBackup.RUN_HOUR)
    }
}
