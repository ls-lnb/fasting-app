package org.myfastingapp.app.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import org.myfastingapp.app.domain.FastSession
import org.myfastingapp.app.domain.StartReminder
import org.myfastingapp.app.domain.UserSettings

class FastReminderScheduler(private val context: Context) {
    private val alarmManager: AlarmManager?
        get() = context.getSystemService()

    private val notificationController = FastNotificationController(context)

    fun schedule(session: FastSession?, settings: UserSettings, nowEpochMillis: Long = System.currentTimeMillis()) {
        cancelScheduledAlarms()
        notificationController.showOngoing(session, settings, nowEpochMillis)
        // Armed each day regardless of the session, so the nudge survives
        // the fast ending; the receiver re-checks whether a fast is running then.
        scheduleStartReminder(settings, nowEpochMillis)
        if (session == null) return

        planFastAlarms(session, settings, nowEpochMillis).forEach { alarm ->
            val action = when (alarm.kind) {
                FastAlarmKind.MILESTONE -> ReminderReceiver.ACTION_FAST_MILESTONE
                FastAlarmKind.PHASE_UPDATE -> ReminderReceiver.ACTION_FAST_NOTIFICATION_UPDATE
                FastAlarmKind.TARGET_REMINDER -> ReminderReceiver.ACTION_FAST_REMINDER
            }
            val requestCode = when (alarm.kind) {
                FastAlarmKind.MILESTONE -> REQUEST_MILESTONE_BASE + requireNotNull(alarm.milestonePercent)
                FastAlarmKind.PHASE_UPDATE -> REQUEST_PHASE_UPDATE_BASE + requireNotNull(alarm.phaseHour)
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
        cancelRequest(REQUEST_START_REMINDER, ReminderReceiver.ACTION_DAILY_FAST_REMINDER)
        MILESTONES.forEach { cancelRequest(REQUEST_MILESTONE_BASE + it, ReminderReceiver.ACTION_FAST_MILESTONE) }
        PHASE_HOUR_MARKS.forEach { cancelRequest(REQUEST_PHASE_UPDATE_BASE + it, ReminderReceiver.ACTION_FAST_NOTIFICATION_UPDATE) }
    }

    private fun scheduleStartReminder(settings: UserSettings, nowEpochMillis: Long) {
        val triggerAt = planStartReminder(settings, nowEpochMillis) ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_DAILY_FAST_REMINDER)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_START_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Inexact on purpose, like the nightly backup: no exact-alarm permission.
        alarmManager?.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
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
        const val REQUEST_START_REMINDER = 3501
    }
}

internal enum class FastAlarmKind {
    MILESTONE,
    PHASE_UPDATE,
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

/** When the next nightly start reminder fires, or null while the option is off. */
internal fun planStartReminder(settings: UserSettings, nowEpochMillis: Long): Long? =
    if (settings.startReminderEnabled) {
        StartReminder.nextTriggerAt(
            nowEpochMillis = nowEpochMillis,
            hour = settings.startReminderHour,
            minute = settings.startReminderMinute,
        )
    } else {
        null
    }
