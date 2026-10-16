package com.blinkingblue.trackfast

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blinkingblue.trackfast.ui.AppTab
import com.blinkingblue.trackfast.ui.TrackFast
import com.blinkingblue.trackfast.ui.TrackFastViewModel

class MainActivity : ComponentActivity() {
    private var refreshExternalSurfaces: (() -> Unit)? = null
    private var appViewModel: TrackFastViewModel? = null
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) refreshExternalSurfaces?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as TrackFastApplication).container
        setContent {
            val viewModel: TrackFastViewModel = viewModel(
                factory = TrackFastViewModel.factory(
                    application = application as TrackFastApplication,
                    repository = container.repository,
                    settingsStore = container.settingsStore,
                    reminderScheduler = container.reminderScheduler,
                    autoBackupScheduler = container.autoBackupScheduler,
                ),
            )
            appViewModel = viewModel
            refreshExternalSurfaces = viewModel::refreshExternalSurfaces
            // Notification taps can carry a tab to open; applied once per launch.
            val launchIntent = remember { intent }
            LaunchedEffect(viewModel, launchIntent) {
                requestTabFrom(launchIntent, viewModel)
            }
            TrackFast(viewModel)
        }
        requestNotificationPermissionIfNeeded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        appViewModel?.let { viewModel -> requestTabFrom(intent, viewModel) }
    }

    override fun onDestroy() {
        refreshExternalSurfaces = null
        appViewModel = null
        super.onDestroy()
    }

    /** Opens the tab a notification asked for, if its extra names a known one. */
    private fun requestTabFrom(intent: Intent?, viewModel: TrackFastViewModel) {
        val tabName = intent?.getStringExtra(EXTRA_OPEN_TAB) ?: return
        AppTab.entries.firstOrNull { it.name == tabName }?.let(viewModel::requestTab)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        /** Extra carrying the [AppTab] name that a notification tap should open. */
        const val EXTRA_OPEN_TAB = "com.blinkingblue.trackfast.OPEN_TAB"
    }
}
