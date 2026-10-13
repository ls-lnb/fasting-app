package org.myfastingapp.app.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import org.myfastingapp.app.MyFastingAppApplication
import org.myfastingapp.app.domain.AutoBackup
import java.time.LocalDate

/**
 * Writes one JSON backup per day into the folder chosen in settings, then deletes
 * automatic backups older than the retention window. Manual exports are never touched.
 */
class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as? MyFastingAppApplication)?.container ?: return Result.success()
        val settings = container.settingsStore.settings.first()
        val treeUri = settings.autoBackupTreeUri
        if (!settings.autoBackupEnabled || treeUri == null) return Result.success()

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
        return Result.success()
    }
}
