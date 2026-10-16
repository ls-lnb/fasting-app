package com.blinkingblue.trackfast.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.first
import com.blinkingblue.trackfast.TrackFastApplication
import com.blinkingblue.trackfast.domain.AutoBackup
import java.time.LocalDate

/**
 * Writes today's automatic backup into the chosen folder and deletes automatic
 * backups outside the retention window. Manual exports are never touched.
 */
class AutoBackupRunner(private val context: Context) {
    suspend fun run(): Boolean {
        val container = (context.applicationContext as? TrackFastApplication)?.container ?: return false
        val settings = container.settingsStore.settings.first()
        val treeUri = settings.autoBackupTreeUri
        if (!settings.autoBackupEnabled || treeUri == null) return false

        val now = System.currentTimeMillis()
        val succeeded = runCatching {
            val json = container.repository.exportBackupJson(settings)
            val store = container.autoBackupStore
            val folder = Uri.parse(treeUri)
            val today = LocalDate.now()
            store.write(folder, AutoBackup.fileName(today), json)
            store.delete(
                folder,
                AutoBackup.expired(
                    names = store.listNames(folder),
                    today = today,
                    retentionDays = settings.autoBackupRetentionDays,
                ),
            )
        }.isSuccess

        container.settingsStore.setAutoBackupLastRun(now, succeeded)
        return succeeded
    }
}
