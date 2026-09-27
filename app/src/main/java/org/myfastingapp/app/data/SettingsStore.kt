package org.myfastingapp.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.myfastingapp.app.domain.FastPlans
import org.myfastingapp.app.domain.ThemeMode
import org.myfastingapp.app.domain.UserSettings
import org.myfastingapp.app.domain.WeightUnit

private val Context.myFastingAppSettings by preferencesDataStore("myfastingapp_settings")

class SettingsStore(private val context: Context) {
    val settings: Flow<UserSettings> = context.myFastingAppSettings.data.map { preferences ->
        UserSettings(
            defaultPlanId = preferences[Keys.DEFAULT_PLAN_ID] ?: FastPlans.DEFAULT_ID,
            customFastingMinutes = preferences[Keys.CUSTOM_FASTING_MINUTES] ?: FastPlans.DEFAULT_CUSTOM_MINUTES,
            remindersEnabled = preferences[Keys.REMINDERS_ENABLED] ?: false,
            reminderLeadMinutes = preferences[Keys.REMINDER_LEAD_MINUTES] ?: 15,
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
        )
    }

    suspend fun setDefaultPlan(planId: String) {
        context.myFastingAppSettings.edit { it[Keys.DEFAULT_PLAN_ID] = planId }
    }

    suspend fun setCustomFastingMinutes(minutes: Int) {
        context.myFastingAppSettings.edit {
            it[Keys.DEFAULT_PLAN_ID] = FastPlans.CUSTOM_ID
            it[Keys.CUSTOM_FASTING_MINUTES] = minutes.coerceIn(30, 7 * 24 * 60)
        }
    }

    suspend fun setReminders(enabled: Boolean, leadMinutes: Int) {
        context.myFastingAppSettings.edit {
            it[Keys.REMINDERS_ENABLED] = enabled
            it[Keys.REMINDER_LEAD_MINUTES] = leadMinutes.coerceIn(0, 24 * 60)
        }
    }

    suspend fun setMilestoneAlerts(enabled: Boolean, percents: Set<Int>) {
        context.myFastingAppSettings.edit {
            it[Keys.MILESTONE_ALERTS_ENABLED] = enabled
            it[Keys.MILESTONE_PERCENTS] = percents
                .filter { percent -> percent in UserSettings.MILESTONE_OPTIONS }
                .sorted()
                .joinToString(",")
        }
    }

    suspend fun setWeightUnit(unit: WeightUnit) {
        context.myFastingAppSettings.edit {
            it[Keys.WEIGHT_UNIT] = unit.storageValue
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.myFastingAppSettings.edit {
            it[Keys.THEME_MODE] = mode.storageValue
        }
    }

    suspend fun setTargetWeightKg(weightKg: Double?) {
        context.myFastingAppSettings.edit {
            if (weightKg == null) {
                it.remove(Keys.TARGET_WEIGHT_KG)
            } else {
                it[Keys.TARGET_WEIGHT_KG] = weightKg.coerceIn(20.0, 500.0)
            }
        }
    }

    suspend fun replace(settings: UserSettings) {
        context.myFastingAppSettings.edit {
            it[Keys.DEFAULT_PLAN_ID] = settings.defaultPlanId
            it[Keys.CUSTOM_FASTING_MINUTES] = settings.customFastingMinutes.coerceIn(30, 7 * 24 * 60)
            it[Keys.REMINDERS_ENABLED] = settings.remindersEnabled
            it[Keys.REMINDER_LEAD_MINUTES] = settings.reminderLeadMinutes.coerceIn(0, 24 * 60)
            it[Keys.WEIGHT_UNIT] = settings.weightUnit.storageValue
            it[Keys.MILESTONE_ALERTS_ENABLED] = settings.milestoneAlertsEnabled
            it[Keys.MILESTONE_PERCENTS] = settings.milestonePercents
                .filter { percent -> percent in UserSettings.MILESTONE_OPTIONS }
                .sorted()
                .joinToString(",")
            it[Keys.THEME_MODE] = settings.themeMode.storageValue
            if (settings.targetWeightKg == null) {
                it.remove(Keys.TARGET_WEIGHT_KG)
            } else {
                it[Keys.TARGET_WEIGHT_KG] = settings.targetWeightKg.coerceIn(20.0, 500.0)
            }
        }
    }

    suspend fun reset() {
        context.myFastingAppSettings.edit { it.clear() }
    }

    private object Keys {
        val DEFAULT_PLAN_ID = stringPreferencesKey("default_plan_id")
        val CUSTOM_FASTING_MINUTES = intPreferencesKey("custom_fasting_minutes")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_LEAD_MINUTES = intPreferencesKey("reminder_lead_minutes")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val TARGET_WEIGHT_KG = doublePreferencesKey("target_weight_kg")
        val MILESTONE_ALERTS_ENABLED = booleanPreferencesKey("milestone_alerts_enabled")
        val MILESTONE_PERCENTS = stringPreferencesKey("milestone_percents")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
