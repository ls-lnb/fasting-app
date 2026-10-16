package com.blinkingblue.trackfast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.blinkingblue.trackfast.domain.AutoBackup
import java.time.LocalDate

/** Automatic backups are named per day and pruned by age; manual exports are never pruned. */
class AutoBackupPolicyTest {
    private val today = LocalDate.of(2026, 10, 7)

    @Test
    fun automaticFilesAreNamedByDay() {
        assertEquals("trackfast-autobackup_2026-10-07.json", AutoBackup.fileName(today))
    }

    @Test
    fun onlyAutomaticNamesAreRecognised() {
        assertTrue(AutoBackup.isAutoBackup("trackfast-autobackup_2026-10-07.json"))
        assertEquals(today, AutoBackup.dateOf("trackfast-autobackup_2026-10-07.json"))

        assertFalse(AutoBackup.isAutoBackup("trackfast-backup_20261007_0645.json"))
        assertFalse(AutoBackup.isAutoBackup("trackfast-autobackup_2026-10-07.txt"))
        assertFalse(AutoBackup.isAutoBackup("holiday-photo.jpg"))
        assertNull(AutoBackup.dateOf("trackfast-autobackup_notadate.json"))
    }

    @Test
    fun retentionKeepsTodayPlusTheDaysBefore() {
        val names = (1..10).map { day -> AutoBackup.fileName(today.minusDays(day.toLong())) }
        val expired = AutoBackup.expired(names, today, retentionDays = 7)

        // 7 days kept means today plus the six days before it: Oct 1-7 survive, Sep 30-27 go.
        assertEquals(
            listOf("2026-09-30", "2026-09-29", "2026-09-28", "2026-09-27").map { AutoBackup.fileName(LocalDate.parse(it)) },
            expired,
        )
    }

    @Test
    fun manualExportsAreNeverExpiredHoweverOld() {
        val manual = listOf(
            "trackfast-backup_20200101_0900.json",
            "trackfast-backup_20261007_0645.json",
        )
        val expired = AutoBackup.expired(manual + AutoBackup.fileName(today.minusDays(90)), today, retentionDays = 3)

        assertEquals(listOf(AutoBackup.fileName(today.minusDays(90))), expired)
    }

    @Test
    fun unrelatedFilesAreLeftAlone() {
        assertTrue(AutoBackup.expired(listOf("notes.txt", "backup.zip"), today, retentionDays = 1).isEmpty())
    }

    @Test
    fun retentionWindowIsClampedToSaneValues() {
        assertEquals(AutoBackup.MIN_RETENTION_DAYS, AutoBackup.coerceRetentionDays(0))
        assertEquals(AutoBackup.MAX_RETENTION_DAYS, AutoBackup.coerceRetentionDays(9_999))
        assertEquals(30, AutoBackup.coerceRetentionDays(30))
    }

    @Test
    fun sameDayFilesAreReplacedRatherThanAccumulated() {
        val morning = AutoBackup.fileName(today)
        val names = listOf(morning, AutoBackup.fileName(today.minusDays(1)), "trackfast-backup_20261007_0700.json")

        assertEquals(listOf(morning), AutoBackup.sameDay(names, today))
    }
}
