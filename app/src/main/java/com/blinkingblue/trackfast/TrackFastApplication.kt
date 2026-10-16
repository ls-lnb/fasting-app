package com.blinkingblue.trackfast

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TrackFastApplication : Application() {
    val container: TrackFastContainer by lazy { TrackFastContainer(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        syncAutoBackupSchedule()
    }

    /** Re-arms the daily backup alarm; the alarm is also re-armed on boot and after each run. */
    private fun syncAutoBackupSchedule() {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            val settings = container.settingsStore.settings.first()
            container.autoBackupScheduler.sync(settings.autoBackupEnabled && settings.autoBackupTreeUri != null)
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService<NotificationManager>() ?: return
        val statusChannel = NotificationChannel(
            FASTING_STATUS_CHANNEL_ID,
            getString(R.string.notification_channel_status),
            NotificationManager.IMPORTANCE_LOW,
        )
        val alertChannel = NotificationChannel(
            FASTING_ALERTS_CHANNEL_ID,
            getString(R.string.notification_channel_alerts),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannels(listOf(statusChannel, alertChannel))
    }

    companion object {
        const val FASTING_STATUS_CHANNEL_ID = "fasting_status"
        const val FASTING_ALERTS_CHANNEL_ID = "fasting_alerts"
    }
}
