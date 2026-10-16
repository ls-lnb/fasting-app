package com.blinkingblue.trackfast.backup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.blinkingblue.trackfast.TrackFastApplication

/** Runs the daily automatic backup, then re-arms the alarm for the next day. */
class AutoBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_AUTO_BACKUP) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? TrackFastApplication ?: return@launch
                app.container.autoBackupRunner.run()
                val settings = app.container.settingsStore.settings.first()
                app.container.autoBackupScheduler.sync(
                    settings.autoBackupEnabled && settings.autoBackupTreeUri != null,
                )
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_AUTO_BACKUP = "com.blinkingblue.trackfast.backup.AUTO_BACKUP"
    }
}
