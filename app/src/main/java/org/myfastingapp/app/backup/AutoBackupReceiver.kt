package org.myfastingapp.app.backup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.myfastingapp.app.MyFastingAppApplication

/** Runs the daily automatic backup, then re-arms the alarm for the next day. */
class AutoBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_AUTO_BACKUP) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? MyFastingAppApplication ?: return@launch
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
        const val ACTION_AUTO_BACKUP = "org.myfastingapp.app.backup.AUTO_BACKUP"
    }
}
