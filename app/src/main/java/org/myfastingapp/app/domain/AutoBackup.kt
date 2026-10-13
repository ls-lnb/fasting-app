package org.myfastingapp.app.domain

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Naming and retention rules for automatic daily backups.
 *
 * Automatic files are named `<prefix>_<yyyy-MM-dd>.json` so they are visually distinct
 * from manual exports (`myfastingapp-backup_<yyyyMMdd_HHmm>.json`). Retention only ever
 * deletes automatic files: manual exports are always kept, whatever their age.
 */
object AutoBackup {
    const val MANUAL_PREFIX = "myfastingapp-backup_"
    const val AUTO_PREFIX = "myfastingapp-autobackup_"

    /** Retention choices offered in settings, in days. */
    val RETENTION_OPTIONS: List<Int> = listOf(3, 7, 14, 30)
    const val DEFAULT_RETENTION_DAYS: Int = 7
    const val MIN_RETENTION_DAYS: Int = 1
    const val MAX_RETENTION_DAYS: Int = 365

    /** Hour of day the daily run is aimed at (the system may run it a little later). */
    const val RUN_HOUR: Int = 3

    private val FILE_NAME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun fileName(date: LocalDate): String = "$AUTO_PREFIX${date.format(FILE_NAME_FORMAT)}.json"

    /** Date encoded in an automatic backup's file name, or null if it is not one. */
    fun dateOf(name: String): LocalDate? {
        if (!name.startsWith(AUTO_PREFIX) || !name.endsWith(".json")) return null
        val raw = name.removePrefix(AUTO_PREFIX).removeSuffix(".json")
        return runCatching { LocalDate.parse(raw, FILE_NAME_FORMAT) }.getOrNull()
    }

    fun isAutoBackup(name: String): Boolean = dateOf(name) != null

    fun coerceRetentionDays(days: Int): Int = days.coerceIn(MIN_RETENTION_DAYS, MAX_RETENTION_DAYS)

    /**
     * Automatic backups that fall outside the retention window and should be deleted.
     * [retentionDays] counts today plus the days before it, so 7 keeps a week of files.
     * Names that are not automatic backups (manual exports, other files) are never returned.
     */
    fun expired(names: List<String>, today: LocalDate, retentionDays: Int): List<String> {
        val oldestKept = today.minusDays(coerceRetentionDays(retentionDays).toLong() - 1L)
        return names.filter { name ->
            val date = dateOf(name) ?: return@filter false
            date.isBefore(oldestKept)
        }
    }

    /** Automatic backups for the same day as [date], which a new run replaces. */
    fun sameDay(names: List<String>, date: LocalDate): List<String> = names.filter { dateOf(it) == date }
}
