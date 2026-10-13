package org.myfastingapp.app.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import org.myfastingapp.app.domain.AutoBackup

/**
 * Keeps the daily automatic backup work in step with settings: one periodic run aimed
 * at [AutoBackup.RUN_HOUR] local time, rescheduled whenever the setting changes.
 */
class AutoBackupScheduler(private val context: Context) {
    fun sync(enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(Duration.ofDays(1))
            .setInitialDelay(initialDelay())
            .build()
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Runs a backup now (used right after a folder is chosen or the setting is turned on). */
    fun runNow() {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<AutoBackupWorker>().build())
    }

    private fun initialDelay(): Duration {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(AutoBackup.RUN_HOUR, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "auto_backup_daily"
        private const val IMMEDIATE_WORK_NAME = "auto_backup_now"
    }
}
