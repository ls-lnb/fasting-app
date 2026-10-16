package com.blinkingblue.trackfast.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Reads and writes automatic backup files inside a user-chosen folder (SAF tree),
 * so the app needs no storage permission and only touches its own files.
 */
class AutoBackupStore(private val context: Context) {
    private fun folder(treeUri: Uri): DocumentFile? =
        DocumentFile.fromTreeUri(context, treeUri)?.takeIf { it.isDirectory && it.canWrite() }

    fun isWritable(treeUri: Uri): Boolean = folder(treeUri) != null

    /** Display name of the chosen folder, for the settings row. */
    fun folderName(treeUri: Uri): String? = DocumentFile.fromTreeUri(context, treeUri)?.name

    fun listNames(treeUri: Uri): List<String> =
        folder(treeUri)?.listFiles()?.mapNotNull { it.name } ?: emptyList()

    fun delete(treeUri: Uri, names: List<String>) {
        val dir = folder(treeUri) ?: return
        val victims = names.toSet()
        dir.listFiles().forEach { file ->
            if (file.name in victims) file.delete()
        }
    }

    /**
     * Writes [json] as [name], replacing any existing file with that name, and returns
     * the written file's size in bytes.
     */
    fun write(treeUri: Uri, name: String, json: String): Long {
        val dir = folder(treeUri) ?: error("Backup folder is not available.")
        // Replace rather than accumulate: one file per day.
        dir.listFiles().filter { it.name == name }.forEach { it.delete() }
        val file = dir.createFile("application/json", name) ?: error("Could not create the backup file.")
        context.contentResolver.openOutputStream(file.uri)?.bufferedWriter()?.use { writer ->
            writer.write(json)
        } ?: error("Could not write the backup file.")
        return file.length()
    }
}
