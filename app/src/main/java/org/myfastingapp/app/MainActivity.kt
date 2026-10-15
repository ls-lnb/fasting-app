package org.myfastingapp.app

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
import org.myfastingapp.app.ui.AppTab
import org.myfastingapp.app.ui.MyFastingApp
import org.myfastingapp.app.ui.MyFastingAppViewModel

class MainActivity : ComponentActivity() {
    private var refreshExternalSurfaces: (() -> Unit)? = null
    private var appViewModel: MyFastingAppViewModel? = null
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) refreshExternalSurfaces?.invoke()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as MyFastingAppApplication).container
        setContent {
            val viewModel: MyFastingAppViewModel = viewModel(
                factory = MyFastingAppViewModel.factory(
                    application = application as MyFastingAppApplication,
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
            MyFastingApp(viewModel)
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
    private fun requestTabFrom(intent: Intent?, viewModel: MyFastingAppViewModel) {
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
        const val EXTRA_OPEN_TAB = "org.myfastingapp.app.OPEN_TAB"
    }
}
