package com.blinkingblue.trackfast

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import com.blinkingblue.trackfast.domain.FastSession
import com.blinkingblue.trackfast.domain.UserSettings
import com.blinkingblue.trackfast.widget.TrackFastWidgetProvider

@RunWith(AndroidJUnit4::class)
class TrackFastWidgetProviderTest {
    @Test
    fun widgetProviderIsDiscoverableByAppWidgetManager() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val provider = TrackFastWidgetProvider()
        val manager = AppWidgetManager.getInstance(context)

        assertNotNull(provider)
        assertNotNull(manager)
    }

    @Test
    fun idleWidgetRendersStartState() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val provider = TrackFastWidgetProvider()
        val root = provider
            .buildViewsForTest(context, active = null, settings = UserSettings())
            .apply(context, FrameLayout(context))

        assertNotNull(root.findViewById<View>(R.id.widget_root).background)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.widget_idle_panel).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.widget_active_panel).visibility)
        assertTrue(root.findViewById<TextView>(R.id.widget_idle_title).text.contains("Ready"))
        assertTrue(root.findViewById<TextView>(R.id.widget_idle_detail).text.contains("16:8"))
        assertTrue(root.findViewById<TextView>(R.id.widget_start).text.contains("Start"))
    }

    @Test
    fun activeWidgetRendersProgressState() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val provider = TrackFastWidgetProvider()
        val now = System.currentTimeMillis()
        val active = FastSession(
            id = 1L,
            planId = "16_8",
            planName = "16:8",
            targetSeconds = 16L * 60L * 60L,
            startEpochMillis = now - 8L * 60L * 60L * 1_000L,
            endEpochMillis = null,
            createdEpochMillis = now,
            updatedEpochMillis = now,
        )

        val root = provider
            .buildViewsForTest(context, active = active, settings = UserSettings())
            .apply(context, FrameLayout(context))

        assertEquals(View.GONE, root.findViewById<View>(R.id.widget_idle_panel).visibility)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.widget_active_panel).visibility)
        assertEquals("Stored glucose", root.findViewById<TextView>(R.id.widget_phase).text.toString())
        assertTrue(root.findViewById<TextView>(R.id.widget_status).text.contains("%"))
        assertEquals("08:00", root.findViewById<TextView>(R.id.widget_time).text.toString())
        assertTrue(root.findViewById<TextView>(R.id.widget_remaining).text.contains("remaining"))
        assertNotNull(root.findViewById<ImageView>(R.id.widget_ring).drawable)
    }

    @Test
    fun widgetBroadcastActionsStartAndEndFast() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<TrackFastApplication>()
        val context = app.applicationContext
        app.container.repository.deleteAllLocalData()

        context.sendBroadcast(
            Intent(context, TrackFastWidgetProvider::class.java)
                .setAction(TrackFastWidgetProvider.ACTION_START_DEFAULT),
        )
        waitUntil { app.container.repository.activeSession.first() != null }

        context.sendBroadcast(
            Intent(context, TrackFastWidgetProvider::class.java)
                .setAction(TrackFastWidgetProvider.ACTION_END_FAST),
        )
        waitUntil { app.container.repository.activeSession.first() == null }

        val sessions = app.container.repository.allSessionsSnapshot()
        assertEquals(1, sessions.size)
        assertNotNull(sessions.single().endEpochMillis)
    }

    private suspend fun waitUntil(timeoutMillis: Long = 5_000L, condition: suspend () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            delay(100L)
        }
        fail("Condition was not met within $timeoutMillis ms.")
    }
}
