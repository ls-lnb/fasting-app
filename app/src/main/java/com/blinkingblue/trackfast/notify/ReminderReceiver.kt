package com.blinkingblue.trackfast.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.blinkingblue.trackfast.TrackFastApplication
import com.blinkingblue.trackfast.domain.StartReminder
import com.blinkingblue.trackfast.widget.TrackFastWidgetProvider

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FAST_REMINDER,
            ACTION_FAST_MILESTONE,
            ACTION_FAST_NOTIFICATION_UPDATE,
            ACTION_DAILY_FAST_REMINDER,
            ACTION_NOTIFICATION_END,
            -> handleAsync(context, intent)
        }
    }

    private fun handleAsync(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TrackFastApplication
                val repository = app.container.repository
                val settingsStore = app.container.settingsStore
                val notificationController = FastNotificationController(context)

                when (intent.action) {
                    ACTION_NOTIFICATION_END -> repository.endActiveFast()
                    ACTION_FAST_MILESTONE -> {
                        val percent = intent.getIntExtra(EXTRA_MILESTONE_PERCENT, 0)
                        val active = repository.activeSession.first()
                        val settings = settingsStore.settings.first()
                        if (
                            active != null &&
                            active.id == intent.getLongExtra(EXTRA_SESSION_ID, -1L) &&
                            settings.milestoneAlertsEnabled &&
                            percent in settings.milestonePercents
                        ) {
                            notificationController.showMilestone(active, percent)
                        }
                    }
                    ACTION_FAST_REMINDER -> {
                        val active = repository.activeSession.first()
                        notificationController.showTargetReminder(active)
                    }
                    ACTION_DAILY_FAST_REMINDER -> {
                        val settings = settingsStore.settings.first()
                        val active = repository.activeSession.first()
                        if (StartReminder.shouldRemind(settings.startReminderEnabled, active != null)) {
                            notificationController.showStartReminder(settings)
                        }
                    }
                    ACTION_FAST_NOTIFICATION_UPDATE -> Unit
                }

                repository.repairActivePlanTarget()
                val active = repository.activeSession.first()
                val settings = settingsStore.settings.first()
                app.container.reminderScheduler.schedule(active, settings)
                TrackFastWidgetProvider.updateWidgets(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FAST_REMINDER = "com.blinkingblue.trackfast.notify.FAST_REMINDER"
        const val ACTION_DAILY_FAST_REMINDER = "com.blinkingblue.trackfast.notify.DAILY_FAST_REMINDER"
        const val ACTION_FAST_MILESTONE = "com.blinkingblue.trackfast.notify.FAST_MILESTONE"
        const val ACTION_FAST_NOTIFICATION_UPDATE = "com.blinkingblue.trackfast.notify.FAST_NOTIFICATION_UPDATE"
        const val ACTION_NOTIFICATION_END = "com.blinkingblue.trackfast.notify.END_FAST"
        const val EXTRA_SESSION_ID = "session_id"
        const val EXTRA_MILESTONE_PERCENT = "milestone_percent"
    }
}
