package org.myfastingapp.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import org.myfastingapp.app.data.SettingsStore
import org.myfastingapp.app.domain.FastSession
import org.myfastingapp.app.domain.UserSettings

class FastReminderScheduler(
    private val context: Context,
    private val settingsStore: SettingsStore,
) {
    private val alarmManager: AlarmManager?
        get() = context.getSystemService()

    private val notificationController = FastNotificationController(context)

    suspend fun schedule(session: FastSession?, settings: UserSettings, nowEpochMillis: Long = System.currentTimeMillis()) {
        cancelScheduledAlarms()
        notificationController.showOngoing(session, settings, nowEpochMillis)
        if (session == null) return

        backfillMissedMilestones(session, settings, nowEpochMillis)

        planFastAlarms(session, settings, nowEpochMillis).forEach { alarm ->
            val action = when (alarm.kind) {
                FastAlarmKind.MILESTONE -> ReminderReceiver.ACTION_FAST_MILESTONE
                FastAlarmKind.PHASE_UPDATE -> ReminderReceiver.ACTION_FAST_NOTIFICATION_UPDATE
                FastAlarmKind.REFRESH -> ReminderReceiver.ACTION_FAST_NOTIFICATION_UPDATE
                FastAlarmKind.TARGET_REMINDER -> ReminderReceiver.ACTION_FAST_REMINDER
            }
            val requestCode = when (alarm.kind) {
                FastAlarmKind.MILESTONE -> REQUEST_MILESTONE_BASE + requireNotNull(alarm.milestonePercent)
                FastAlarmKind.PHASE_UPDATE -> REQUEST_PHASE_UPDATE_BASE + requireNotNull(alarm.phaseHour)
                FastAlarmKind.REFRESH -> REQUEST_REFRESH
                FastAlarmKind.TARGET_REMINDER -> REQUEST_TARGET_REMINDER
            }
            scheduleAlarm(
                action = action,
                requestCode = requestCode,
                triggerAt = alarm.triggerAtEpochMillis,
                sessionId = session.id,
                milestone = alarm.milestonePercent,
                wakeDevice = alarm.wakeDevice,
            )
        }
    }

    fun cancel() {
        cancelScheduledAlarms()
    }

    /**
     * Posts milestone alerts whose threshold passed but that were never delivered
     * (alarms can be deferred by doze or battery saver). Claiming goes through
     * DataStore so each milestone fires at most once per session; thresholds
     * passed longer than [BACKFILL_WINDOW_MILLIS] ago are skipped as stale.
     */
    private suspend fun backfillMissedMilestones(
        session: FastSession,
        settings: UserSettings,
        nowEpochMillis: Long,
    ) {
        val candidates = MILESTONES.filter { milestone ->
            if (milestone !in settings.activeMilestonePercents) return@filter false
            val passedAt = session.startEpochMillis + ((session.targetSeconds * 1_000L * milestone) / 100L)
            passedAt <= nowEpochMillis && nowEpochMillis - passedAt <= BACKFILL_WINDOW_MILLIS
        }
        settingsStore.claimMilestoneNotifications(session.id, candidates).forEach { percent ->
            notificationController.showMilestone(session, percent, nowEpochMillis)
        }
    }

    private fun scheduleAlarm(
        action: String,
        requestCode: Int,
        triggerAt: Long,
        sessionId: Long,
        milestone: Int? = null,
        wakeDevice: Boolean,
    ) {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .putExtra(ReminderReceiver.EXTRA_SESSION_ID, sessionId)
        if (milestone != null) {
            intent.putExtra(ReminderReceiver.EXTRA_MILESTONE_PERCENT, milestone)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val alarmType = if (wakeDevice) AlarmManager.RTC_WAKEUP else AlarmManager.RTC
        alarmManager?.set(alarmType, triggerAt, pendingIntent)
    }

    private fun cancelScheduledAlarms() {
        cancelRequest(REQUEST_TARGET_REMINDER, ReminderReceiver.ACTION_FAST_REMINDER)
        cancelRequest(REQUEST_REFRESH, ReminderReceiver.ACTION_FAST_NOTIFICATION_UPDATE)
        MILESTONES.forEach { cancelRequest(REQUEST_MILESTONE_BASE + it, ReminderReceiver.ACTION_FAST_MILESTONE) }
        PHASE_HOUR_MARKS.forEach { cancelRequest(REQUEST_PHASE_UPDATE_BASE + it, ReminderReceiver.ACTION_FAST_NOTIFICATION_UPDATE) }
    }

    private fun cancelRequest(requestCode: Int, action: String) {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager?.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private companion object {
        const val REQUEST_TARGET_REMINDER = 3101
        const val REQUEST_MILESTONE_BASE = 3300
        const val REQUEST_PHASE_UPDATE_BASE = 3400
        const val REQUEST_REFRESH = 3500
    }
}

internal enum class FastAlarmKind {
    MILESTONE,
    PHASE_UPDATE,
    REFRESH,
    TARGET_REMINDER,
}

internal data class PlannedFastAlarm(
    val kind: FastAlarmKind,
    val triggerAtEpochMillis: Long,
    val wakeDevice: Boolean,
    val milestonePercent: Int? = null,
    val phaseHour: Int? = null,
)

internal fun planFastAlarms(
    session: FastSession,
    settings: UserSettings,
    nowEpochMillis: Long,
): List<PlannedFastAlarm> {
    val alarms = buildList {
        // Self-rearming periodic refresh: keeps the ongoing notification's progress
        // current even when milestone/phase alarms are deferred by doze or battery
        // saver. Inexact (no exact-alarm permission); the receiver re-plans the next
        // tick when it fires, and any later schedule() call re-arms it again.
        add(
            PlannedFastAlarm(
                kind = FastAlarmKind.REFRESH,
                triggerAtEpochMillis = nowEpochMillis + PERIODIC_REFRESH_INTERVAL_MILLIS,
                wakeDevice = true,
            ),
        )
        MILESTONES.forEach { milestone ->
            if (milestone !in settings.activeMilestonePercents) return@forEach
            val triggerAt = session.startEpochMillis + ((session.targetSeconds * 1_000L * milestone) / 100L)
            if (triggerAt > nowEpochMillis) {
                add(
                    PlannedFastAlarm(
                        kind = FastAlarmKind.MILESTONE,
                        triggerAtEpochMillis = triggerAt,
                        wakeDevice = true,
                        milestonePercent = milestone,
                    ),
                )
            }
        }
        PHASE_HOUR_MARKS.forEach { hour ->
            val triggerAt = session.startEpochMillis + hour * 3_600_000L
            if (triggerAt > nowEpochMillis) {
                add(
                    PlannedFastAlarm(
                        kind = FastAlarmKind.PHASE_UPDATE,
                        triggerAtEpochMillis = triggerAt,
                        wakeDevice = false,
                        phaseHour = hour,
                    ),
                )
            }
        }
        if (settings.remindersEnabled) {
            val targetEnd = session.startEpochMillis + session.targetSeconds * 1_000L
            val triggerAt = targetEnd - settings.reminderLeadMinutes * 60_000L
            if (triggerAt > nowEpochMillis) {
                add(
                    PlannedFastAlarm(
                        kind = FastAlarmKind.TARGET_REMINDER,
                        triggerAtEpochMillis = triggerAt,
                        wakeDevice = true,
                    ),
                )
            }
        }
    }
    return alarms.sortedBy(PlannedFastAlarm::triggerAtEpochMillis)
}

private val MILESTONES = UserSettings.MILESTONE_OPTIONS
private val PHASE_HOUR_MARKS = listOf(4, 12, 18, 24)

/** How often the ongoing notification refreshes in the background while fasting. */
internal val PERIODIC_REFRESH_INTERVAL_MILLIS = 15L * 60_000L

/** Passed milestones older than this are not backfilled (avoids stale bursts). */
internal val BACKFILL_WINDOW_MILLIS = 12L * 60L * 60_000L
