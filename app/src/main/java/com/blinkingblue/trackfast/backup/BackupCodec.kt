package com.blinkingblue.trackfast.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.blinkingblue.trackfast.domain.AutoBackup
import com.blinkingblue.trackfast.domain.FastPlans
import com.blinkingblue.trackfast.domain.FastSession
import com.blinkingblue.trackfast.domain.StartReminder
import com.blinkingblue.trackfast.domain.ThemeMode
import com.blinkingblue.trackfast.domain.UserSettings
import com.blinkingblue.trackfast.domain.WeightEntry
import com.blinkingblue.trackfast.domain.WeightUnit
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val backupFileNameFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")

/** Default export name, e.g. trackfast-backup_20261007_0640.json */
fun backupFileName(now: LocalDateTime = LocalDateTime.now()): String =
    "trackfast-backup_${now.format(backupFileNameFormatter)}.json"

class BackupCodec(
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    },
) {
    fun encode(
        settings: UserSettings,
        sessions: List<FastSession>,
        weights: List<WeightEntry>,
        exportedAtEpochMillis: Long,
    ): String {
        val envelope = BackupEnvelope(
            exportedAtEpochMillis = exportedAtEpochMillis,
            settings = SettingsBackup(
                defaultPlanId = settings.defaultPlanId,
                customFastingMinutes = settings.customFastingMinutes,
                remindersEnabled = settings.remindersEnabled,
                reminderLeadMinutes = settings.reminderLeadMinutes,
                startReminderEnabled = settings.startReminderEnabled,
                startReminderHour = StartReminder.coerceHour(settings.startReminderHour),
                startReminderMinute = StartReminder.coerceMinute(settings.startReminderMinute),
                weightUnit = settings.weightUnit.storageValue,
                targetWeightKg = settings.targetWeightKg,
                milestoneAlertsEnabled = settings.milestoneAlertsEnabled,
                milestonePercents = settings.milestonePercents.sorted(),
                themeMode = settings.themeMode.storageValue,
                autoBackupEnabled = settings.autoBackupEnabled,
                autoBackupRetentionDays = AutoBackup.coerceRetentionDays(settings.autoBackupRetentionDays),
            ),
            sessions = sessions.sortedBy { it.startEpochMillis }.map { it.toBackup() },
            weights = weights.sortedBy { it.recordedEpochMillis }.map { it.toBackup() },
        )
        return json.encodeToString(BackupEnvelope.serializer(), envelope)
    }

    fun decode(rawJson: String): ImportedBackup {
        val envelope = json.decodeFromString(BackupEnvelope.serializer(), rawJson)
        require(envelope.schemaVersion == CURRENT_SCHEMA_VERSION) {
            "Unsupported backup schema: ${envelope.schemaVersion}"
        }
        val settings = UserSettings(
            defaultPlanId = envelope.settings.defaultPlanId,
            customFastingMinutes = envelope.settings.customFastingMinutes.coerceIn(30, 7 * 24 * 60),
            remindersEnabled = envelope.settings.remindersEnabled,
            reminderLeadMinutes = envelope.settings.reminderLeadMinutes.coerceIn(0, 24 * 60),
            startReminderEnabled = envelope.settings.startReminderEnabled,
            startReminderHour = StartReminder.coerceHour(envelope.settings.startReminderHour),
            startReminderMinute = StartReminder.coerceMinute(envelope.settings.startReminderMinute),
            weightUnit = WeightUnit.fromStorage(envelope.settings.weightUnit),
            targetWeightKg = envelope.settings.targetWeightKg?.coerceIn(20.0, 500.0),
            milestoneAlertsEnabled = envelope.settings.milestoneAlertsEnabled,
            milestonePercents = envelope.settings.milestonePercents
                .filter { it in UserSettings.MILESTONE_OPTIONS }
                .toSet(),
            themeMode = ThemeMode.fromStorage(envelope.settings.themeMode),
            autoBackupEnabled = envelope.settings.autoBackupEnabled,
            autoBackupRetentionDays = AutoBackup.coerceRetentionDays(envelope.settings.autoBackupRetentionDays),
        )
        val sessions = envelope.sessions.map { it.validated() }
        val weights = envelope.weights.map { it.validated() }
        return ImportedBackup(settings, sessions, weights)
    }

    fun encodeCsv(sessions: List<FastSession>): String {
        val header = listOf(
            "id",
            "plan_id",
            "plan_name",
            "target_seconds",
            "start_iso",
            "end_iso",
            "duration_seconds",
        ).joinToString(",")

        val rows = sessions
            .sortedBy { it.startEpochMillis }
            .map { session ->
                val end = session.endEpochMillis
                listOf(
                    session.id.toString(),
                    session.planId,
                    session.planName,
                    session.targetSeconds.toString(),
                    Instant.ofEpochMilli(session.startEpochMillis).toString(),
                    end?.let { Instant.ofEpochMilli(it).toString() }.orEmpty(),
                    end?.let { ((it - session.startEpochMillis) / 1_000L).coerceAtLeast(0L).toString() }.orEmpty(),
                ).joinToString(",") { it.csvEscape() }
            }
        return (listOf(header) + rows).joinToString("\n") + "\n"
    }

    private fun FastSession.toBackup(): SessionBackup = SessionBackup(
        planId = planId,
        planName = planName,
        targetSeconds = targetSeconds,
        startEpochMillis = startEpochMillis,
        endEpochMillis = endEpochMillis,
        createdEpochMillis = createdEpochMillis,
        updatedEpochMillis = updatedEpochMillis,
    )

    private fun WeightEntry.toBackup(): WeightBackup = WeightBackup(
        weightKg = weightKg,
        recordedEpochMillis = recordedEpochMillis,
        createdEpochMillis = createdEpochMillis,
    )

    private fun String.csvEscape(): String {
        val needsQuotes = any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return this
        return "\"" + replace("\"", "\"\"") + "\""
    }
}

data class ImportedBackup(
    val settings: UserSettings,
    val sessions: List<SessionBackup>,
    val weights: List<WeightBackup>,
)

@Serializable
data class BackupEnvelope(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtEpochMillis: Long,
    val settings: SettingsBackup = SettingsBackup(),
    val sessions: List<SessionBackup> = emptyList(),
    val weights: List<WeightBackup> = emptyList(),
)

private const val CURRENT_SCHEMA_VERSION = 3

@Serializable
data class SettingsBackup(
    val defaultPlanId: String = FastPlans.DEFAULT_ID,
    val customFastingMinutes: Int = FastPlans.DEFAULT_CUSTOM_MINUTES,
    val remindersEnabled: Boolean = false,
    val reminderLeadMinutes: Int = 15,
    val startReminderEnabled: Boolean = false,
    val startReminderHour: Int = StartReminder.DEFAULT_HOUR,
    val startReminderMinute: Int = StartReminder.DEFAULT_MINUTE,
    val weightUnit: String = WeightUnit.LB.storageValue,
    val targetWeightKg: Double? = null,
    val milestoneAlertsEnabled: Boolean = true,
    val milestonePercents: List<Int> = UserSettings.MILESTONE_OPTIONS,
    val themeMode: String = ThemeMode.SYSTEM.storageValue,
    val autoBackupEnabled: Boolean = false,
    val autoBackupRetentionDays: Int = AutoBackup.DEFAULT_RETENTION_DAYS,
)

@Serializable
data class SessionBackup(
    val planId: String,
    val planName: String,
    val targetSeconds: Long,
    val startEpochMillis: Long,
    val endEpochMillis: Long? = null,
    val createdEpochMillis: Long,
    val updatedEpochMillis: Long,
) {
    fun validated(): SessionBackup {
        require(planId.isNotBlank()) { "Session plan id is required." }
        require(planName.isNotBlank()) { "Session plan name is required." }
        require(targetSeconds in (30 * 60L)..(7 * 24 * 60 * 60L)) { "Invalid target duration." }
        require(startEpochMillis > 0L) { "Invalid start time." }
        endEpochMillis?.let { require(it > startEpochMillis) { "End time must be after start time." } }
        return this
    }
}

@Serializable
data class WeightBackup(
    val weightKg: Double,
    val recordedEpochMillis: Long,
    val createdEpochMillis: Long,
) {
    fun validated(): WeightBackup {
        require(weightKg in 20.0..500.0) { "Invalid weight." }
        require(recordedEpochMillis > 0L) { "Invalid weight date." }
        require(createdEpochMillis > 0L) { "Invalid weight created date." }
        return this
    }
}
