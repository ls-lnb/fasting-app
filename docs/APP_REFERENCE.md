# MyFastingApp App Reference

MyFastingApp is a GPL-3.0-only, offline-first Android fasting tracker. The package id is `org.myfastingapp.app`, minSdk is 26, targetSdk is 36, and app version `1.0.1` maps to versionCode `2`.

## Release Invariants

- No `INTERNET` or `ACCESS_NETWORK_STATE` permission.
- No accounts, analytics, ads, cloud sync, Firebase, Google Play Services, telemetry, remote config, crash reporting, social features, or health/cloud integrations.
- Storage is local Room plus DataStore. Import/export is user initiated through Android's Storage Access Framework.
- Only one active fast may exist at a time.
- Fasting and weight timestamps are stored as epoch milliseconds.
- Release builds enable minification and resource shrinking.
- Release artifacts omit Google Play dependency metadata from the APK signing block.

## Architecture

- `app/src/main/java/org/myfastingapp/app/MyFastingAppApplication.kt`: creates notification channels and owns the app container.
- `MyFastingAppContainer.kt`: wires Room, DataStore, repository, backup codec, reminder scheduler, notification controller, and widget updater.
- `ui/MyFastingApp.kt`: Jetpack Compose Material 3 UI for timer, plans, trends, history, settings, dialogs, date picker, and time picker, plus a theme-aware color palette (light and dark) provided through a CompositionLocal.
- `ui/MyFastingAppViewModel.kt`: UI state, user actions, import/export launch data, notifications, widget refreshes, and validation messages.
- `data/*`: Room entities/DAOs/database plus settings DataStore.
- `domain/*`: timer math, stats, weight projection, built-in plans, fasting phase labels, and domain models.
- `backup/BackupCodec.kt`: JSON backup schema v3 and CSV session export.
- `notify/*`: local reminder broadcasts, boot restoration, ongoing status notification, and milestone notifications.
- `widget/*`: classic RemoteViews home-screen widget and bitmap progress ring renderer.

## Data Model

`FastSession` contains:

- `id`
- `planId`
- `planName`
- `targetSeconds`
- `startEpochMillis`
- `endEpochMillis`
- `createdEpochMillis`
- `updatedEpochMillis`

`endEpochMillis == null` means the fast is active. Active fast editing changes start time and planned target duration while keeping the session active. Completed fast editing requires end time after start time and blocks overlapping sessions.

`WeightEntry` contains:

- `id`
- `weightKg`
- `recordedEpochMillis`
- `createdEpochMillis`

Weights are stored in kg. The UI can display kg or lb based on settings.

`UserSettings` contains:

- default plan id
- custom fasting minutes
- reminder settings
- milestone alert settings (on/off plus selected milestone percentages)
- theme mode (follow system, dark, light)
- weight unit
- target weight in kg

## Screens

- Timer: active or idle state, plan chip, progress ring, phase label, elapsed seconds, remaining minutes, start/end action, and editable active fast times. Starting a fast opens a start dialog that accepts a backdated start time (quick 30 min/1 h/2 h/3 h shortcuts or a date-time wheel) before the fast is created; the post-start pencil edit remains available.
- Fasts: built-in plans and custom duration picker.
- Trends: week/month/year fasting charts and weight charts, with consolidated labels for longer ranges.
- History: active and completed sessions, manual fast logging, editing, and deletion.
- Settings: appearance (follow system / dark / light theme), weight unit, target weight, notification customization (milestone progress alerts on/off with per-milestone selection, and the optional target reminder), backup/import/export, and two-step local data deletion. The Settings column can scroll if content exceeds small viewports.

The dark theme uses a dedicated palette (deep navy surfaces, brightened brand indigo, light text) rather than an inverted light palette. System bar colors and icon contrast follow the in-app theme; on API 35+ the platform's enforced edge-to-edge behavior takes precedence. The home-screen widget keeps its wallpaper-agnostic design in both themes.

All other app-owned screens are designed to fit without vertical scrolling on the target phone viewport used for release testing.

## Fasting Phases

The main timer and notification use short, simple phase labels:

- 0-4h: Food energy
- 4-12h: Stored glucose
- 12-18h: Fat switch
- 18-24h: Fat burning
- 24h+: Extended fast

The language is intentionally modest and avoids medical claims. It describes broad energy-use changes, not guaranteed personal outcomes.

## Backup Format

JSON backup schema version: `3`.

The backup envelope includes:

- `schemaVersion`
- `exportedAtEpochMillis`
- `settings` (plan, reminder, milestone alert, theme mode, weight unit, and target weight fields; new fields have defaults so older schema-3 backups still import)
- `sessions`
- `weights`

Imports validate schema version, one-active-fast rule, session duration bounds, end-after-start, no overlaps, weight range, and timestamp sanity. Import replaces local sessions, weights, and settings.

CSV export is sessions-only and includes:

- `id`
- `plan_id`
- `plan_name`
- `target_seconds`
- `start_iso`
- `end_iso`
- `duration_seconds`

## Notifications

MyFastingApp uses two notification channels:

- `fasting_status`: low-importance ongoing notification while fasting.
- `fasting_alerts`: progress milestone and target reminder notifications.

Milestones are scheduled for 25%, 50%, 75%, 90%, 95%, and 100% of the active fast target. Scheduling uses OS-managed local alarms/reminders and does not request exact alarm special access.

Milestone progress alerts are user-configurable in Settings: a master switch turns them off entirely, and individual milestone percentages can be selected. Only selected milestones are scheduled as alarms, and the receiver re-checks the settings before posting so a disabled milestone never notifies. Backdated fasts only schedule milestones that fall in the future, so past milestones are not replayed. The optional target reminder and the low-importance ongoing status notification are controlled separately.

The app does not run a foreground service or a continuously polling worker. The
one-second timer runs only while the timer screen is visible and the activity is
resumed.

While a fast is active, a self-rearming **inexact 15-minute refresh alarm**
(`RTC_WAKEUP`, no exact-alarm permission) reposts the ongoing status
notification, so its progress stays current even when milestone or phase alarms
are deferred by doze or battery saver. Milestone alerts missed while the device
was asleep are **backfilled on the next refresh**: claims are deduplicated per
session in DataStore so each milestone fires at most once, and thresholds passed
more than 12 hours earlier are skipped as stale. The widget shows
minute-precision snapshots refreshed after user actions and meaningful fast
events. Milestones and the refresh tick may wake the device; phase-only updates
use non-waking alarms, and target reminders are optional.

## Widget

The widget has two states:

- Idle: app name, prompt to start, default-plan start button, and open-app control.
- Active: progress ring bitmap, elapsed/remaining status, phase label, start/end/open controls.

The widget uses RemoteViews for launcher compatibility and a rendered bitmap ring for visual parity with the app. Its root uses an opaque cream background with a subtle border so text remains readable across light, dark, and photographic wallpapers. It does not run a continuously ticking chronometer or request periodic polling.

## Stress Testing

Large-history behavior is covered by tests that round-trip hundreds of fast and weight records through backup parsing. Release builds do not include a sample-data action.
