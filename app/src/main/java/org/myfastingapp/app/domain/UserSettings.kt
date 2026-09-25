package org.myfastingapp.app.domain

data class UserSettings(
    val defaultPlanId: String = FastPlans.DEFAULT_ID,
    val customFastingMinutes: Int = FastPlans.DEFAULT_CUSTOM_MINUTES,
    val remindersEnabled: Boolean = false,
    val reminderLeadMinutes: Int = 15,
    val weightUnit: WeightUnit = WeightUnit.LB,
    val targetWeightKg: Double? = null,
    val milestoneAlertsEnabled: Boolean = true,
    val milestonePercents: Set<Int> = MILESTONE_OPTIONS.toSet(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
) {
    val defaultPlan: FastPlan
        get() = FastPlans.resolve(defaultPlanId, customFastingMinutes)

    val activeMilestonePercents: Set<Int>
        get() = if (milestoneAlertsEnabled) milestonePercents else emptySet()

    companion object {
        /** Milestone percentages offered for progress notifications, in ascending order. */
        val MILESTONE_OPTIONS: List<Int> = listOf(25, 50, 75, 90, 95, 100)
    }
}

enum class WeightUnit(val storageValue: String, val label: String) {
    LB("lb", "lb"),
    KG("kg", "kg");

    companion object {
        fun fromStorage(value: String?): WeightUnit {
            return entries.firstOrNull { it.storageValue == value } ?: LB
        }
    }
}

enum class ThemeMode(val storageValue: String, val label: String, val description: String) {
    SYSTEM("system", "Follow system", "Matches your phone's light or dark setting."),
    DARK("dark", "Dark", "Always use the dark theme."),
    LIGHT("light", "Light", "Always use the light theme.");

    companion object {
        fun fromStorage(value: String?): ThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}
