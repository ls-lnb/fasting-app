package org.myfastingapp.app.backup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import org.myfastingapp.app.domain.AutoBackup
import java.time.Instant
import java.time.ZoneId

/**
 * Schedules the daily automatic backup with a single inexact alarm, matching the
 * app's reminder scheduling: no exact-alarm permission, no wake locks, no work
 * library. Each run re-arms the next day from [AutoBackupReceiver].
 */
class AutoBackupScheduler(private val context: Context) {
    private val alarmManager: AlarmManager?
        get() = context.getSystemService()

    fun sync(enabled: Boolean) {
        if (enabled) {
            scheduleNext()
        } else {
            cancel()
        }
    }

    fun scheduleNext(nowEpochMillis: Long = System.currentTimeMillis()) {
        val pendingIntent = pendingIntent(PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        // Inexact on purpose: a backup does not need an exact wake-up.
        alarmManager?.set(AlarmManager.RTC, nextRunAt(nowEpochMillis), pendingIntent)
    }

    fun cancel() {
        val pendingIntent = pendingIntent(PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun pendingIntent(flag: Int): PendingIntent? = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, AutoBackupReceiver::class.java).setAction(AutoBackupReceiver.ACTION_AUTO_BACKUP),
        flag or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        private const val REQUEST_CODE = 3601

        /** Next occurrence of [AutoBackup.RUN_HOUR] local time, today or tomorrow. */
        fun nextRunAt(nowEpochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
            val now = Instant.ofEpochMilli(nowEpochMillis).atZone(zone)
            val today = now.toLocalDate().atTime(AutoBackup.RUN_HOUR, 0).atZone(zone)
            val next = if (today.toInstant().isAfter(now.toInstant())) today else today.plusDays(1)
            return next.toInstant().toEpochMilli()
        }
    }
}
