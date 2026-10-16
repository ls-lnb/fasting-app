package com.blinkingblue.trackfast

import android.content.Context
import androidx.room.Room
import com.blinkingblue.trackfast.backup.AutoBackupRunner
import com.blinkingblue.trackfast.backup.AutoBackupScheduler
import com.blinkingblue.trackfast.backup.AutoBackupStore
import com.blinkingblue.trackfast.backup.BackupCodec
import com.blinkingblue.trackfast.data.FastRepository
import com.blinkingblue.trackfast.data.TrackFastDatabase
import com.blinkingblue.trackfast.data.SettingsStore
import com.blinkingblue.trackfast.notify.FastReminderScheduler

class TrackFastContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: TrackFastDatabase = Room.databaseBuilder(
        appContext,
        TrackFastDatabase::class.java,
        "trackfast.db",
    )
        .build()

    val settingsStore = SettingsStore(appContext)
    val backupCodec = BackupCodec()
    val repository = FastRepository(database.fastSessionDao(), database.weightEntryDao(), settingsStore, backupCodec)
    val reminderScheduler = FastReminderScheduler(appContext)
    val autoBackupStore = AutoBackupStore(appContext)
    val autoBackupRunner = AutoBackupRunner(appContext)
    val autoBackupScheduler = AutoBackupScheduler(appContext)
}
