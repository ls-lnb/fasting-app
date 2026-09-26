package org.myfastingapp.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.myfastingapp.app.backup.BackupCodec
import org.myfastingapp.app.domain.FastSession
import org.myfastingapp.app.domain.ThemeMode
import org.myfastingapp.app.domain.UserSettings
import org.myfastingapp.app.notify.FastAlarmKind
import org.myfastingapp.app.notify.PERIODIC_REFRESH_INTERVAL_MILLIS
import org.myfastingapp.app.notify.planFastAlarms

/**
 * Guards the exact path used when importing a backup written by the original
 * (pre-customization) APK: settings without milestone/theme fields must decode
 * to enabled milestone alerts, and the scheduler must plan every milestone for
 * the imported active fast.
 */
class ImportScheduleRegressionTest {
    private val codec = BackupCodec()

    // Faithful to the original app's export: schema 3, settings without
    // milestonePercents / milestoneAlertsEnabled / themeMode fields.
    private val originalApkBackup = """
        {
          "schemaVersion": 3,
          "exportedAtEpochMillis": 1789900000000,
          "settings": {
            "defaultPlanId": "16_8",
            "customFastingMinutes": 960,
            "remindersEnabled": false,
            "reminderLeadMinutes": 15,
            "weightUnit": "lb",
            "targetWeightKg": 75.5
          },
          "sessions": [
            {
              "planId": "16_8",
              "planName": "16:8",
              "targetSeconds": 57600,
              "startEpochMillis": 1789900000000,
              "endEpochMillis": null,
              "createdEpochMillis": 1789900000000,
              "updatedEpochMillis": 1789900000000
            },
            {
              "planId": "18_6",
              "planName": "18:6",
              "targetSeconds": 64800,
              "startEpochMillis": 1789800000000,
              "endEpochMillis": 1789864800000,
              "createdEpochMillis": 1789800000000,
              "updatedEpochMillis": 1789864800000
            }
          ],
          "weights": []
        }
    """.trimIndent()

    @Test
    fun originalApkBackupEnablesMilestoneAlertsAndThemeDefault() {
        val settings = codec.decode(originalApkBackup).settings

        assertTrue(settings.milestoneAlertsEnabled)
        assertEquals(UserSettings.MILESTONE_OPTIONS.toSet(), settings.milestonePercents)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals("16_8", settings.defaultPlanId)
        assertEquals(75.5, settings.targetWeightKg!!, 0.001)
    }

    @Test
    fun schedulerPlansAllMilestonesForImportedActiveFast() {
        val backup = codec.decode(originalApkBackup)
        val imported = backup.sessions.single { it.endEpochMillis == null }
        val session = FastSession(
            id = 1L,
            planId = imported.planId,
            planName = imported.planName,
            targetSeconds = imported.targetSeconds,
            startEpochMillis = imported.startEpochMillis,
            endEpochMillis = null,
            createdEpochMillis = imported.createdEpochMillis,
            updatedEpochMillis = imported.updatedEpochMillis,
        )
        // One hour into the fast: every milestone lies in the future.
        val alarms = planFastAlarms(session, backup.settings, nowEpochMillis = session.startEpochMillis + 3_600_000L)

        assertEquals(
            listOf(25, 50, 75, 90, 95, 100),
            alarms.filter { it.kind == FastAlarmKind.MILESTONE }.map { it.milestonePercent },
        )
        assertEquals(
            listOf(4, 12, 18, 24),
            alarms.filter { it.kind == FastAlarmKind.PHASE_UPDATE }.map { it.phaseHour },
        )
        // The periodic refresh tick is planned regardless of milestone settings.
        val refresh = alarms.single { it.kind == FastAlarmKind.REFRESH }
        assertEquals(
            session.startEpochMillis + 3_600_000L + PERIODIC_REFRESH_INTERVAL_MILLIS,
            refresh.triggerAtEpochMillis,
        )
    }

    @Test
    fun alreadyPassedMilestonesAreNotReplayedForImportedFast() {
        val backup = codec.decode(originalApkBackup)
        val imported = backup.sessions.single { it.endEpochMillis == null }
        val session = FastSession(
            id = 1L,
            planId = imported.planId,
            planName = imported.planName,
            targetSeconds = imported.targetSeconds,
            startEpochMillis = imported.startEpochMillis,
            endEpochMillis = null,
            createdEpochMillis = imported.createdEpochMillis,
            updatedEpochMillis = imported.updatedEpochMillis,
        )
        // 10h into a 16h fast: 25% and 50% are in the past and intentionally
        // never fire (this is expected behavior, not a regression).
        val alarms = planFastAlarms(session, backup.settings, nowEpochMillis = session.startEpochMillis + 10 * 3_600_000L)

        assertEquals(
            listOf(75, 90, 95, 100),
            alarms.filter { it.kind == FastAlarmKind.MILESTONE }.map { it.milestonePercent },
        )
    }
}
