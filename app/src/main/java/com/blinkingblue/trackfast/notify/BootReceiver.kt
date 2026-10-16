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
import com.blinkingblue.trackfast.widget.TrackFastWidgetProvider

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TrackFastApplication
                app.container.repository.repairActivePlanTarget()
                val active = app.container.repository.activeSession.first()
                val settings = app.container.settingsStore.settings.first()
                app.container.reminderScheduler.schedule(active, settings)
                app.container.autoBackupScheduler.sync(
                    settings.autoBackupEnabled && settings.autoBackupTreeUri != null,
                )
                TrackFastWidgetProvider.updateWidgets(context)
            } finally {
                pending.finish()
            }
        }
    }
}
