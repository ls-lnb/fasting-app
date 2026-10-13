# MyFastingApp

MyFastingApp is a fully offline, GPLv3 Android fasting tracker. It has no accounts, no ads, no analytics, no cloud sync, no proprietary SDKs, and no network permission.

## Features

- Active fasting timer with built-in plans: 13:11, 16:8, 18:6, 20:4, OMAD, 24h, and custom duration. The Fasts tab is a picker: choosing a plan selects it (remembered as the default) and returns to the Timer tab, where fasts are always started.
- Start a fast now or backdate the start time from the Timer tab, with quick 30 min / 1 h / 2 h / 3 h backdate shortcuts and a full date-time picker.
- One active fast at a time, editable start/end times, editable history, streaks, totals, average duration, and longest fast.
- Evidence-based fasting phase labels on the main timer, plus a stage bar under the timer ring that splits the fasting window into the phases in proportion to their hours, fills as the fast runs, and re-bases to the stage in progress once the plan is beaten. Tapping it opens a stages view explaining every phase from 0-4h to 24h+.
- Local weight logging, target weight, kg/lb setting, trend graph, and simple on-device projection.
- Light, dark, and follow-system theme selectable in Settings, with a purpose-built dark palette.
- Week, month, and year trend views for fasting and weight.
- Home-screen widget with idle and active states, progress ring, and start/end/open controls.
- Low-power local status notification while fasting plus milestone notifications at 25%, 50%, 75%, 90%, 95%, and 100%, each toggleable or fully switchable off in Settings. An optional battery-optimization exemption prompt in Settings helps alerts arrive on time on aggressive OEM builds.
- History grouped by month, ten fasts per page with a "Show 10 more" button, manual fast logging, editing, and deletion. Each row is titled with the plan that fast began with, and its duration is coloured against that plan (green when met, amber from 85%, red below).
- Local JSON backup import/export with timestamped file names, CSV session export, and optional automatic daily backups to a folder you choose (keep 3, 7, 14, or 30 days of daily files; exports you saved by hand are never cleaned up). All of it goes through Android's Storage Access Framework.

## Build

Install JDK 17 and Android SDK Platform 36. Android Studio can provide both. From the repository root:

```powershell
.\gradlew.bat check assembleDebug assembleRelease bundleRelease
.\gradlew.bat connectedDebugAndroidTest
```

Current release outputs:

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Signed release APK: `app/build/outputs/apk/release/app-release.apk`
- Signed release bundle: `app/build/outputs/bundle/release/app-release.aab`

The release build enables code minification and resource shrinking. Release signing is configured through local ignored files: `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`) pointing at an ignored keystore such as `release/signing/myfastingapp-upload.jks`.

## Offline And Privacy Guarantees

The manifest intentionally omits `INTERNET` and `ACCESS_NETWORK_STATE`. App data stays in the local Room database and DataStore preferences unless the user explicitly exports a backup.

The only declared app permissions are local notification support, reboot handling for local reminders/widget refresh, and a user-initiated battery-optimization exemption request (prompted only when the user taps Allow in Settings). Automatic backups write only inside a folder the user picks through the Storage Access Framework, using a persisted grant, so no storage permission is declared and the daily job runs entirely on-device with an inexact alarm. MyFastingApp does not use Google Play Services, Firebase, analytics, crash reporting, ads, remote config, or health/cloud integrations.

## Project Docs

- `docs/APP_REFERENCE.md` explains architecture, data model, backup format, notifications, widget behavior, and release invariants.
- `docs/TEST_REPORT.md` records the current release-hardening test pass and screenshot walkthrough.
- `docs/RELEASE_CHECKLIST.md` documents the F-Droid and reproducible-build release flow.

## License

GPL-3.0-only. See `LICENSE`.
