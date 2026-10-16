package com.blinkingblue.trackfast.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.blinkingblue.trackfast.MainActivity
import com.blinkingblue.trackfast.TrackFastApplication
import com.blinkingblue.trackfast.R
import com.blinkingblue.trackfast.domain.FastSession
import com.blinkingblue.trackfast.domain.FastingPhases
import com.blinkingblue.trackfast.domain.TimerMath
import com.blinkingblue.trackfast.domain.UserSettings
import com.blinkingblue.trackfast.ui.AppTab

class FastNotificationController(private val context: Context) {
    /**
     * Live progress notification for the running fast. With [session] null there is
     * nothing persistent to show, so any earlier status notification is cancelled
     * instead of being swapped for an idle "ready" one.
     */
    @SuppressLint("MissingPermission")
    fun showOngoing(session: FastSession?, nowEpochMillis: Long = System.currentTimeMillis()) {
        if (session == null) {
            NotificationManagerCompat.from(context).cancel(ONGOING_NOTIFICATION_ID)
            return
        }
        if (!canPostNotifications()) return

        val progress = TimerMath.progress(session.startEpochMillis, session.targetSeconds, nowEpochMillis)
        val phase = FastingPhases.forElapsed(progress.elapsedMillis)
        val remainingText = if (progress.targetReached) {
            "Extended by ${TimerMath.formatDuration(progress.elapsedMillis - progress.targetMillis)}"
        } else {
            "${TimerMath.formatDuration(progress.remainingMillis)} remaining"
        }
        val notification = NotificationCompat.Builder(context, TrackFastApplication.FASTING_STATUS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_trackfast)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setLocalOnly(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentTitle("${TimerMath.formatProgressPercent(progress.progressFraction)} - ${phase.title}")
            .setContentText("${TimerMath.formatDuration(progress.elapsedMillis)} elapsed - $remainingText")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${session.displayPlanName}: ${TimerMath.formatDuration(progress.elapsedMillis)} elapsed, $remainingText. ${phase.body}",
                ),
            )
            .setProgress(100, progress.progressPercent.coerceAtMost(100), false)
            .setColor(phase.colorArgb)
            .setShowWhen(false)
            .addAction(R.drawable.ic_stat_trackfast, "End fast", actionIntent(ReminderReceiver.ACTION_NOTIFICATION_END, REQUEST_END))
            .build()

        NotificationManagerCompat.from(context).notify(ONGOING_NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    fun showMilestone(session: FastSession, percent: Int, nowEpochMillis: Long = System.currentTimeMillis()) {
        if (!canPostNotifications()) return
        val progress = TimerMath.progress(session.startEpochMillis, session.targetSeconds, nowEpochMillis)
        val phase = FastingPhases.forElapsed(progress.elapsedMillis)
        val notification = NotificationCompat.Builder(context, TrackFastApplication.FASTING_ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_trackfast)
            .setContentTitle("$percent% fast progress")
            .setContentText("${phase.title}: ${TimerMath.formatDuration(progress.elapsedMillis)} elapsed.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${session.displayPlanName} is $percent% complete. ${phase.body}"))
            .setColor(phase.colorArgb)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setLocalOnly(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(MILESTONE_NOTIFICATION_BASE_ID + percent, notification)
    }

    @SuppressLint("MissingPermission")
    fun showTargetReminder(session: FastSession?, nowEpochMillis: Long = System.currentTimeMillis()) {
        if (!canPostNotifications()) return
        val text = session?.let {
            val progress = TimerMath.progress(it.startEpochMillis, it.targetSeconds, nowEpochMillis)
            "${TimerMath.formatProgressPercent(progress.progressFraction)} complete - target is almost here."
        } ?: "Your fasting target is almost here."
        val notification = NotificationCompat.Builder(context, TrackFastApplication.FASTING_ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_trackfast)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setLocalOnly(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(FAST_REMINDER_NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    fun showStartReminder(settings: UserSettings) {
        if (!canPostNotifications()) return
        val plan = settings.defaultPlan
        val notification = NotificationCompat.Builder(context, TrackFastApplication.FASTING_ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_trackfast)
            .setContentTitle("Time to start your fast")
            .setContentText("Start ${plan.displayLabel} - ${TimerMath.formatMinutes(plan.fastingMinutes)} fasting.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Your ${plan.displayLabel} window is ready to begin. Tap to open the timer and start. Everything stays on this device.",
                ),
            )
            .setContentIntent(openAppIntent(targetTab = AppTab.Timer))
            .setAutoCancel(true)
            .setLocalOnly(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(START_REMINDER_NOTIFICATION_ID, notification)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * PendingIntent into [MainActivity]. With [targetTab] the intent carries the
     * tab to open and re-uses a running instance (CLEAR_TOP + SINGLE_TOP) instead
     * of stacking a second one, and gets its own request code so the extras of
     * the plain open-app intent are never overwritten.
     */
    private fun openAppIntent(targetTab: AppTab? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        if (targetTab != null) {
            intent.putExtra(MainActivity.EXTRA_OPEN_TAB, targetTab.name)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            if (targetTab == null) REQUEST_OPEN_APP else REQUEST_OPEN_TAB,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ONGOING_NOTIFICATION_ID = 4001
        const val FAST_REMINDER_NOTIFICATION_ID = 4101
        const val START_REMINDER_NOTIFICATION_ID = 4102
        private const val MILESTONE_NOTIFICATION_BASE_ID = 4200
        private const val REQUEST_END = 4302
        private const val REQUEST_OPEN_APP = 0
        private const val REQUEST_OPEN_TAB = 4303
    }
}
