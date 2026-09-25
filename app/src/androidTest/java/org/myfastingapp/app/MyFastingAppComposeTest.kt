package org.myfastingapp.app

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MyFastingAppComposeTest {
    @get:Rule(order = 0)
    val notificationPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardShowsPrimaryAction() {
        composeRule.onNodeWithText("MyFastingApp").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("Start fast").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("End fast now").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun startFlowOffersBackdatingBeforeTheFastBegins() {
        composeRule.waitUntil(timeoutMillis = 5_000L) {
            composeRule.onAllNodesWithText("Start fast").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithText("End fast now").fetchSemanticsNodes().isNotEmpty()
        }
        val idle = composeRule.onAllNodesWithText("Start fast").fetchSemanticsNodes().isNotEmpty()
        if (!idle) {
            // A fast is already active in this run, so the start dialog is not reachable.
            assertTrue(composeRule.onAllNodesWithText("End fast now").fetchSemanticsNodes().isNotEmpty())
            return
        }

        composeRule.onNodeWithText("Start fast").performClick()

        composeRule.onNodeWithText("Start now, or backdate if the fast already began.").assertIsDisplayed()
        composeRule.onNodeWithText("2 h").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
    }
}
