# TrackFast App Reference

TrackFast is a GPL-3.0-only, offline-first Android fasting tracker. The package id is `com.blinkingblue.trackfast`, minSdk is 26, targetSdk is 36, and app version `1.0.1` maps to versionCode `2`.

## Release Invariants

- No `INTERNET` or `ACCESS_NETWORK_STATE` permission.
- No accounts, analytics, ads, cloud sync, Firebase, Google Play Services, telemetry, remote config, crash reporting, social features, or health/cloud integrations.
- Storage is local Room plus DataStore. Import/export is user initiated through Android's Storage Access Framework.
- Only one active fast may exist at a time.
- Fasting and weight timestamps are stored as epoch milliseconds.
- Release builds enable minification and resource shrinking.
- Release artifacts omit Google Play dependency metadata from the APK signing block.

## Architecture

- `app/src/main/java/com/blinkingblue/trackfast/TrackFastApplication.kt`: creates notification channels and owns the app container.
- `TrackFastContainer.kt`: wires Room, DataStore, repository, backup codec, reminder scheduler, notification controller, and widget updater.
- `ui/TrackFast.kt`: Jetpack Compose Material 3 UI for timer, plans, trends, history, settings, dialogs, date picker, and time picker, plus a theme-aware color palette (light and dark) provided through a CompositionLocal.
- `ui/TrackFastViewModel.kt`: UI state, user actions, import/export launch data, notifications, widget refreshes, and validation messages.
- `data/*`: Room entities/DAOs/database plus settings DataStore.
- `domain/*`: timer math, stats, the History streak calendar grid, weight projection, built-in plans, fasting phase labels, and domain models.
- `backup/BackupCodec.kt`: JSON backup schema v3 and CSV session export.
- `notify/*`: local reminder broadcasts, boot restoration, ongoing status notification, milestone notifications, and the daily fast reminder.
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
- daily fast reminder settings (on/off plus chosen time)
- milestone alert settings (on/off plus selected milestone percentages)
- theme mode (follow system, dark, light)
- weight unit
- target weight in kg

## Screens

- Timer: active or idle state, plan chip, progress ring, phase label, elapsed seconds, remaining minutes, start/end action, and editable active fast times. A stage bar under the ring splits the window into the fasting phases in proportion to the hours each covers: idle it previews the split of the remembered plan ("3 stages over 16h"), and while a fast runs passed stages fill, the current one fills part way, and the caption names the next stage. Once the fast runs past its planned window the bar re-bases to the stage in progress (instead of sitting full), and past 24h it draws the final stage up to the next whole hour. Tapping the bar opens a stages view listing every phase from 0-4h to 24h+ with its hours, a short description, and the phase in progress (the full list shows regardless of the chosen duration). Starting a fast opens a start dialog that accepts a backdated start time (quick 30 min/1 h/2 h/3 h shortcuts or a date-time wheel) before the fast is created; the post-start pencil edit remains available.
- Fasts: predefined plan picker (built-in plans plus a custom duration card). Choosing a plan only selects it and returns to the Timer tab; fasts are always started from Timer.
- Trends: week/month/year fasting charts and weight charts, with consolidated labels for longer ranges.
- History: a GitHub-style streak calendar across the top covering the last 12 rolling months, horizontally scrollable (it opens at the current date) with month captions, Mon/Wed/Fri row labels, and a Less/More legend. Each day cell takes its colour from the best fast that started on that day in one of three theme purple shades (plan met = strongest, near = mid, short = lightest) and stays empty when no fast starts that day. Below it: active and completed sessions grouped by month, ten per page with a "Show 10 more" button, manual fast logging, editing, and deletion. Each row's duration is coloured by how the fast compared with the plan it began with: green at 100% of the plan, yellow from 85%, and red below that.
- Settings: appearance (follow system / dark / light theme), weight unit, target weight, notification customization (milestone progress alerts on/off with per-milestone selection, the optional target reminder, and the optional daily fast reminder with an adjustable time), an unrestricted-background battery-optimization exemption prompt with OEM guidance, manual backup export/import, automatic daily backups to a chosen folder with an age-based retention window, and two-step local data deletion. The Settings column can scroll if content exceeds small viewports.

The dark theme uses a dedicated palette (deep navy surfaces, brightened brand indigo, light text) rather than an inverted light palette. System bar colors and icon contrast follow the in-app theme; on API 35+ the platform's enforced edge-to-edge behavior takes precedence. The home-screen widget keeps its wallpaper-agnostic design in both themes.

All other app-owned screens are designed to fit without vertical scrolling on the target phone viewport used for release testing.

## Automatic Backup

Automatic backups are opt-in in Settings: a folder is chosen through the Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`), and the app keeps a persisted read/write grant for it, so no storage permission is requested. A single inexact `AlarmManager` alarm aims to run daily at 03:00 local time (the system may defer it; the alarm is re-armed after each run and on boot), writing one JSON file per day named `trackfast-autobackup_<yyyy-MM-dd>.json`; re-running on the same day replaces that day's file. No work library, wake lock, or exact-alarm permission is used.

Retention is counted in days (3, 7, 14, or 30, default 7) and applies only to automatic files: a run deletes automatic backups dated before `today - (retention - 1)`. Manual exports use a different name shape (`trackfast-backup_<yyyyMMdd_HHmm>.json`) and are never deleted by retention, nor are unrelated files in the folder. Turning the setting on, or choosing a folder, triggers an immediate run, and the result (success or failure with its time) is shown under the retention chips.

## Fasting Phases

The main timer and notification use short, simple phase labels:

- 0-4h: Food energy
- 4-12h: Stored glucose
- 12-18h: Fat switch
- 18-24h: Fat burning
- 24h+: Extended fast

The language is intentionally modest and avoids medical claims. It describes broad energy-use changes, not guaranteed personal outcomes. Each phase also carries a longer write-up for the in-app stages view (opened from the timer's stage bar).

## Backup Format

JSON backup schema version: `3`.

The backup envelope includes:

- `schemaVersion`
- `exportedAtEpochMillis`
- `settings` (plan, reminder, daily fast reminder, milestone alert, theme mode, weight unit, target weight, and automatic-backup enabled/retention fields; new fields have defaults so older schema-3 backups still import)
- `sessions`
- `weights`

The automatic-backup folder (a device-specific SAF tree URI) is deliberately not part of a backup: it is re-chosen per device. Imports keep the folder already configured on the device.

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

TrackFast uses two notification channels:

- `fasting_status`: low-importance ongoing notification while fasting.
- `fasting_alerts`: progress milestone, target reminder, and daily fast reminder notifications.

Milestones are scheduled for 25%, 50%, 75%, 90%, 95%, and 100% of the active fast target. Scheduling uses OS-managed local alarms/reminders and does not request exact alarm special access.

Milestone progress alerts are user-configurable in Settings: a master switch turns them off entirely, and individual milestone percentages can be selected. Only selected milestones are scheduled as alarms, and the receiver re-checks the settings before posting so a disabled milestone never notifies. Backdated fasts only schedule milestones that fall in the future, so past milestones are not replayed. The optional target reminder is controlled separately, and the low-importance status notification exists only while a fast is running: with no active fast it is cancelled rather than replaced by an idle "ready" notification, so opening the app while not fasting posts nothing.

The optional daily fast reminder is the day-time counterpart of the target reminder: off by default, and once switched on it posts at the chosen time (default 22:00, adjustable in Settings down to the minute) only when no fast is running, so it never asks you to start a fast that has already begun. It is scheduled with one inexact `RTC_WAKEUP` alarm that is re-armed after every alarm run and on boot, in the same spirit as the nightly backup: no exact-alarm permission and no wake locks, so Doze may deliver it a little late with the screen off. Tapping it opens the app on the Timer tab (the intent carries the target tab and re-uses a running instance instead of stacking a second one); it deliberately has no action buttons.

The app does not poll in the background. The one-second timer runs only while
the timer screen is visible and the activity is resumed. Notifications and the
widget show minute-precision snapshots and refresh only after user actions or
meaningful fast events. Milestones may wake the device; phase-only updates use
non-waking alarms, and target reminders are optional.

## Widget

The widget has two states:

- Idle: app name, prompt to start, default-plan start button, and open-app control.
- Active: progress ring bitmap, elapsed/remaining status, phase label, start/end/open controls.

The widget uses RemoteViews for launcher compatibility and a rendered bitmap ring for visual parity with the app. Its root uses an opaque cream background with a subtle border so text remains readable across light, dark, and photographic wallpapers. It does not run a continuously ticking chronometer or request periodic polling.

## Stress Testing

Large-history behavior is covered by tests that round-trip hundreds of fast and weight records through backup parsing. Release builds do not include a sample-data action.
