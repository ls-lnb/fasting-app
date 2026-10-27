package com.blinkingblue.trackfast.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.blinkingblue.trackfast.domain.AutoBackup
import com.blinkingblue.trackfast.domain.FastPlans
import com.blinkingblue.trackfast.domain.StartReminder
import com.blinkingblue.trackfast.domain.ThemeMode
import com.blinkingblue.trackfast.domain.UserSettings
import com.blinkingblue.trackfast.domain.WeightUnit

private val Context.trackFastSettings by preferencesDataStore("trackfast_settings")

class SettingsStore(private val context: Context) {
    val settings: Flow<UserSettings> = context.trackFastSettings.data.map { preferences ->
        UserSettings(
            defaultPlanId = preferences[Keys.DEFAULT_PLAN_ID] ?: FastPlans.DEFAULT_ID,
            customFastingMinutes = preferences[Keys.CUSTOM_FASTING_MINUTES] ?: FastPlans.DEFAULT_CUSTOM_MINUTES,
            remindersEnabled = preferences[Keys.REMINDERS_ENABLED] ?: false,
            reminderLeadMinutes = preferences[Keys.REMINDER_LEAD_MINUTES] ?: 15,
            startReminderEnabled = preferences[Keys.START_REMINDER_ENABLED] ?: false,
            startReminderHour = StartReminder.coerceHour(
                preferences[Keys.START_REMINDER_HOUR] ?: StartReminder.DEFAULT_HOUR,
            ),
            startReminderMinute = StartReminder.coerceMinute(
                preferences[Keys.START_REMINDER_MINUTE] ?: StartReminder.DEFAULT_MINUTE,
            ),
            weightUnit = WeightUnit.fromStorage(preferences[Keys.WEIGHT_UNIT]),
            targetWeightKg = preferences[Keys.TARGET_WEIGHT_KG],
            milestoneAlertsEnabled = preferences[Keys.MILESTONE_ALERTS_ENABLED] ?: true,
            milestonePercents = preferences[Keys.MILESTONE_PERCENTS]
                ?.split(',')
                ?.mapNotNull { value -> value.trim().toIntOrNull() }
                ?.filter { percent -> percent in UserSettings.MILESTONE_OPTIONS }
                ?.toSet()
                ?: UserSettings.MILESTONE_OPTIONS.toSet(),
            themeMode = ThemeMode.fromStorage(preferences[Keys.THEME_MODE]),
            dynamicColorEnabled = preferences[Keys.DYNAMIC_COLOR_ENABLED] ?: false,
            autoBackupEnabled = preferences[Keys.AUTO_BACKUP_ENABLED] ?: false,
            autoBackupRetentionDays = AutoBackup.coerceRetentionDays(
                preferences[Keys.AUTO_BACKUP_RETENTION_DAYS] ?: AutoBackup.DEFAULT_RETENTION_DAYS,
            ),
            autoBackupTreeUri = preferences[Keys.AUTO_BACKUP_TREE_URI],
            autoBackupFolderName = preferences[Keys.AUTO_BACKUP_FOLDER_NAME],
            autoBackupLastRunEpochMillis = preferences[Keys.AUTO_BACKUP_LAST_RUN],
            autoBackupLastSucceeded = preferences[Keys.AUTO_BACKUP_LAST_SUCCEEDED] ?: true,
        )
    }

    suspend fun setDefaultPlan(planId: String) {
        context.trackFastSettings.edit { it[Keys.DEFAULT_PLAN_ID] = planId }
    }

    suspend fun setCustomFastingMinutes(minutes: Int) {
        context.trackFastSettings.edit {
            it[Keys.DEFAULT_PLAN_ID] = FastPlans.CUSTOM_ID
            it[Keys.CUSTOM_FASTING_MINUTES] = minutes.coerceIn(30, 7 * 24 * 60)
        }
    }

    suspend fun setReminders(enabled: Boolean, leadMinutes: Int) {
        context.trackFastSettings.edit {
            it[Keys.REMINDERS_ENABLED] = enabled
            it[Keys.REMINDER_LEAD_MINUTES] = leadMinutes.coerceIn(0, 24 * 60)
        }
    }

    suspend fun setStartReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.trackFastSettings.edit {
            it[Keys.START_REMINDER_ENABLED] = enabled
            it[Keys.START_REMINDER_HOUR] = StartReminder.coerceHour(hour)
            it[Keys.START_REMINDER_MINUTE] = StartReminder.coerceMinute(minute)
        }
    }

    suspend fun setMilestoneAlerts(enabled: Boolean, percents: Set<Int>) {
        context.trackFastSettings.edit {
            it[Keys.MILESTONE_ALERTS_ENABLED] = enabled
            it[Keys.MILESTONE_PERCENTS] = percents
                .filter { percent -> percent in UserSettings.MILESTONE_OPTIONS }
                .sorted()
                .joinToString(",")
        }
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.trackFastSettings.edit {
            it[Keys.WEIGHT_UNIT] = unit.storageValue
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.trackFastSettings.edit {
            it[Keys.THEME_MODE] = mode.storageValue
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.trackFastSettings.edit {
            it[Keys.DYNAMIC_COLOR_ENABLED] = enabled
        }
    }

    suspend fun setAutoBackup(enabled: Boolean, treeUri: String?, folderName: String?, retentionDays: Int) {
        context.trackFastSettings.edit {
            it[Keys.AUTO_BACKUP_ENABLED] = enabled
            it[Keys.AUTO_BACKUP_RETENTION_DAYS] = AutoBackup.coerceRetentionDays(retentionDays)
            if (treeUri == null) {
                it.remove(Keys.AUTO_BACKUP_TREE_URI)
                it.remove(Keys.AUTO_BACKUP_FOLDER_NAME)
            } else {
                it[Keys.AUTO_BACKUP_TREE_URI] = treeUri
                if (folderName == null) {
                    it.remove(Keys.AUTO_BACKUP_FOLDER_NAME)
                } else {
                    it[Keys.AUTO_BACKUP_FOLDER_NAME] = folderName
                }
            }
        }
    }

    suspend fun setAutoBackupLastRun(epochMillis: Long, succeeded: Boolean) {
        context.trackFastSettings.edit {
            it[Keys.AUTO_BACKUP_LAST_RUN] = epochMillis
            it[Keys.AUTO_BACKUP_LAST_SUCCEEDED] = succeeded
        }
    }

    suspend fun setTargetWeightKg(weightKg: Double?) {
        context.trackFastSettings.edit {
            if (weightKg == null) {
                it.remove(Keys.TARGET_WEIGHT_KG)
            } else {
                it[Keys.TARGET_WEIGHT_KG] = weightKg.coerceIn(20.0, 500.0)
            }
        }
    }

    suspend fun replace(settings: UserSettings) {
        context.trackFastSettings.edit {
            it[Keys.DEFAULT_PLAN_ID] = settings.defaultPlanId
            it[Keys.CUSTOM_FASTING_MINUTES] = settings.customFastingMinutes.coerceIn(30, 7 * 24 * 60)
            it[Keys.REMINDERS_ENABLED] = settings.remindersEnabled
            it[Keys.REMINDER_LEAD_MINUTES] = settings.reminderLeadMinutes.coerceIn(0, 24 * 60)
            it[Keys.START_REMINDER_ENABLED] = settings.startReminderEnabled
            it[Keys.START_REMINDER_HOUR] = StartReminder.coerceHour(settings.startReminderHour)
            it[Keys.START_REMINDER_MINUTE] = StartReminder.coerceMinute(settings.startReminderMinute)
            it[Keys.WEIGHT_UNIT] = settings.weightUnit.storageValue
            it[Keys.MILESTONE_ALERTS_ENABLED] = settings.milestoneAlertsEnabled
            it[Keys.MILESTONE_PERCENTS] = settings.milestonePercents
                .filter { percent -> percent in UserSettings.MILESTONE_OPTIONS }
                .sorted()
                .joinToString(",")
            it[Keys.THEME_MODE] = settings.themeMode.storageValue
            it[Keys.DYNAMIC_COLOR_ENABLED] = settings.dynamicColorEnabled
            it[Keys.AUTO_BACKUP_ENABLED] = settings.autoBackupEnabled
            it[Keys.AUTO_BACKUP_RETENTION_DAYS] = AutoBackup.coerceRetentionDays(settings.autoBackupRetentionDays)
            if (settings.targetWeightKg == null) {
                it.remove(Keys.TARGET_WEIGHT_KG)
            } else {
                it[Keys.TARGET_WEIGHT_KG] = settings.targetWeightKg.coerceIn(20.0, 500.0)
            }
        }
    }

    suspend fun reset() {
        context.trackFastSettings.edit { it.clear() }
    }

    private object Keys {
        val DEFAULT_PLAN_ID = stringPreferencesKey("default_plan_id")
        val CUSTOM_FASTING_MINUTES = intPreferencesKey("custom_fasting_minutes")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_LEAD_MINUTES = intPreferencesKey("reminder_lead_minutes")
        val START_REMINDER_ENABLED = booleanPreferencesKey("start_reminder_enabled")
        val START_REMINDER_HOUR = intPreferencesKey("start_reminder_hour")
        val START_REMINDER_MINUTE = intPreferencesKey("start_reminder_minute")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val TARGET_WEIGHT_KG = doublePreferencesKey("target_weight_kg")
        val MILESTONE_ALERTS_ENABLED = booleanPreferencesKey("milestone_alerts_enabled")
        val MILESTONE_PERCENTS = stringPreferencesKey("milestone_percents")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_RETENTION_DAYS = intPreferencesKey("auto_backup_retention_days")
        val AUTO_BACKUP_TREE_URI = stringPreferencesKey("auto_backup_tree_uri")
        val AUTO_BACKUP_FOLDER_NAME = stringPreferencesKey("auto_backup_folder_name")
        val AUTO_BACKUP_LAST_RUN = longPreferencesKey("auto_backup_last_run")
        val AUTO_BACKUP_LAST_SUCCEEDED = booleanPreferencesKey("auto_backup_last_succeeded")
    }
}
