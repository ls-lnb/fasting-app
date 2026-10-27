package com.blinkingblue.trackfast.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.blinkingblue.trackfast.TrackFastApplication
import com.blinkingblue.trackfast.data.FastRepository
import com.blinkingblue.trackfast.data.SettingsStore
import com.blinkingblue.trackfast.backup.AutoBackupScheduler
import com.blinkingblue.trackfast.domain.FastPlan
import com.blinkingblue.trackfast.domain.FastSession
import com.blinkingblue.trackfast.domain.FastStats
import com.blinkingblue.trackfast.domain.StatsCalculator
import com.blinkingblue.trackfast.domain.ThemeMode
import com.blinkingblue.trackfast.domain.UserSettings
import com.blinkingblue.trackfast.domain.WeightEntry
import com.blinkingblue.trackfast.domain.WeightTrend
import com.blinkingblue.trackfast.domain.WeightTrendCalculator
import com.blinkingblue.trackfast.domain.WeightUnit
import com.blinkingblue.trackfast.notify.FastReminderScheduler
import com.blinkingblue.trackfast.widget.TrackFastWidgetProvider

data class TrackFastUiState(
    val sessions: List<FastSession> = emptyList(),
    val activeSession: FastSession? = null,
    val weights: List<WeightEntry> = emptyList(),
    val settings: UserSettings = UserSettings(),
    val stats: FastStats = FastStats(),
    val weightTrend: WeightTrend = WeightTrend(),
) {
    val selectedPlan: FastPlan = settings.defaultPlan
}

class TrackFastViewModel(
    private val application: TrackFastApplication,
    private val repository: FastRepository,
    private val settingsStore: SettingsStore,
    private val reminderScheduler: FastReminderScheduler,
    private val autoBackupScheduler: AutoBackupScheduler,
) : ViewModel() {
    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    /**
     * Tab requested from outside the UI (a notification tap), held until the
     * root composable picks it up and switches tabs.
     */
    private val _pendingTab = MutableStateFlow<AppTab?>(null)
    internal val pendingTab: StateFlow<AppTab?> = _pendingTab

    val uiState: StateFlow<TrackFastUiState> = combine(
        repository.sessions,
        repository.weights,
        settingsStore.settings,
    ) { sessions, weights, settings ->
        TrackFastUiState(
            sessions = sessions,
            activeSession = sessions.firstOrNull { it.isActive },
            weights = weights,
            settings = settings,
            stats = StatsCalculator.calculate(sessions),
            weightTrend = WeightTrendCalculator.calculate(weights, settings.targetWeightKg),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackFastUiState())

    init {
        refreshExternalSurfaces()
    }

    fun refreshExternalSurfaces() {
        viewModelScope.launch {
            refreshReminderAndWidget()
        }
    }

    fun startFast(plan: FastPlan = uiState.value.selectedPlan, startEpochMillis: Long? = null) {
        viewModelScope.launch {
            runCatching {
                repository.startFast(plan, startEpochMillis = startEpochMillis)
                refreshReminderAndWidget()
            }.onFailure { showMessage(it.message ?: "Could not start fast.") }
        }
    }

    fun endFast() {
        viewModelScope.launch {
            runCatching {
                repository.endActiveFast()
                refreshReminderAndWidget()
            }.onFailure { showMessage(it.message ?: "Could not end fast.") }
        }
    }

    fun deleteFast(id: Long) {
        viewModelScope.launch {
            repository.deleteFast(id)
            refreshReminderAndWidget()
        }
    }

    fun editFast(id: Long, startEpochMillis: Long, endEpochMillis: Long) {
        viewModelScope.launch {
            runCatching {
                repository.editFast(id, startEpochMillis, endEpochMillis)
                refreshReminderAndWidget()
            }.onFailure { showMessage(it.message ?: "Could not edit fast.") }
        }
    }

    fun addCompletedFast(planName: String, targetSeconds: Long, startEpochMillis: Long, endEpochMillis: Long) {
        viewModelScope.launch {
            runCatching {
                repository.addCompletedFast(planName, targetSeconds, startEpochMillis, endEpochMillis)
                refreshReminderAndWidget()
            }.onSuccess {
                showMessage("Fast logged.")
            }.onFailure { showMessage(it.message ?: "Could not log fast.") }
        }
    }

    fun addWeight(weightKg: Double, recordedEpochMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            runCatching {
                repository.addWeight(weightKg, recordedEpochMillis)
            }.onFailure { showMessage(it.message ?: "Could not save weight.") }
        }
    }

    fun deleteWeight(id: Long) {
        viewModelScope.launch {
            repository.deleteWeight(id)
        }
    }

    fun selectPlan(planId: String) {
        viewModelScope.launch {
            settingsStore.setDefaultPlan(planId)
        }
    }

    fun setCustomPlan(minutes: Int) {
        viewModelScope.launch {
            settingsStore.setCustomFastingMinutes(minutes)
        }
    }

    internal fun requestTab(tab: AppTab) {
        _pendingTab.value = tab
    }

    internal fun consumePendingTab() {
        _pendingTab.value = null
    }

    fun setReminders(enabled: Boolean, leadMinutes: Int) {
        viewModelScope.launch {
            settingsStore.setReminders(enabled, leadMinutes)
            refreshReminderAndWidget()
        }
    }

    fun setStartReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsStore.setStartReminder(enabled, hour, minute)
            refreshReminderAndWidget()
        }
    }

    fun setMilestoneAlerts(enabled: Boolean, percents: Set<Int>) {
        viewModelScope.launch {
            settingsStore.setMilestoneAlerts(enabled, percents)
            refreshReminderAndWidget()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsStore.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setDynamicColor(enabled)
        }
    }

    fun setWeightUnit(unit: WeightUnit) {
        viewModelScope.launch {
            settingsStore.setWeightUnit(unit)
        }
    }

    fun setTargetWeightKg(weightKg: Double?) {
        viewModelScope.launch {
            runCatching {
                settingsStore.setTargetWeightKg(weightKg)
            }.onSuccess {
                showMessage("Target weight saved.")
            }.onFailure { showMessage(it.message ?: "Could not save target weight.") }
        }
    }

    suspend fun exportJson(): String {
        return repository.exportBackupJson(settingsStore.settings.first())
    }

    suspend fun exportCsv(): String {
        return repository.exportSessionsCsv()
    }

    fun importJson(rawJson: String) {
        viewModelScope.launch {
            runCatching {
                repository.importBackupJson(rawJson)
                refreshReminderAndWidget()
                showMessage("Backup imported.")
            }.onFailure { showMessage(it.message ?: "Could not import backup.") }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            settingsStore.setAutoBackup(
                enabled = enabled,
                treeUri = settings.autoBackupTreeUri,
                folderName = settings.autoBackupFolderName,
                retentionDays = settings.autoBackupRetentionDays,
            )
            autoBackupScheduler.sync(enabled && settings.autoBackupTreeUri != null)
            if (enabled && settings.autoBackupTreeUri != null) application.container.autoBackupRunner.run()
        }
    }

    fun setAutoBackupFolder(treeUri: String, folderName: String?, enable: Boolean) {
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            settingsStore.setAutoBackup(
                enabled = enable,
                treeUri = treeUri,
                folderName = folderName,
                retentionDays = settings.autoBackupRetentionDays,
            )
            autoBackupScheduler.sync(enable)
            if (enable) {
                application.container.autoBackupRunner.run()
                showMessage("Daily backup on. First backup saved to the chosen folder.")
            } else {
                showMessage("Backup folder set.")
            }
        }
    }

    fun setAutoBackupRetentionDays(days: Int) {
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            settingsStore.setAutoBackup(
                enabled = settings.autoBackupEnabled,
                treeUri = settings.autoBackupTreeUri,
                folderName = settings.autoBackupFolderName,
                retentionDays = days,
            )
            // Prune straight away so a shorter window takes effect now.
            if (settings.autoBackupEnabled && settings.autoBackupTreeUri != null) application.container.autoBackupRunner.run()
        }
    }

    fun deleteAllLocalData() {
        viewModelScope.launch {
            runCatching {
                repository.deleteAllLocalData()
                refreshReminderAndWidget()
            }.onSuccess {
                showMessage("Local data deleted from this device.")
            }.onFailure { showMessage(it.message ?: "Could not delete local data.") }
        }
    }

    private fun showMessage(message: String) {
        viewModelScope.launch { _messages.emit(message) }
    }

    private suspend fun refreshReminderAndWidget() {
        repository.repairActivePlanTarget()
        val active = repository.activeSession.first()
        val settings = settingsStore.settings.first()
        reminderScheduler.schedule(active, settings)
        TrackFastWidgetProvider.updateWidgets(application)
    }

    companion object {
        fun factory(
            application: TrackFastApplication,
            repository: FastRepository,
            settingsStore: SettingsStore,
            reminderScheduler: FastReminderScheduler,
            autoBackupScheduler: AutoBackupScheduler,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TrackFastViewModel(application, repository, settingsStore, reminderScheduler, autoBackupScheduler) as T
            }
        }
    }
}
