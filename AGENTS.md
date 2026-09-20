# AGENTS.md

Operating notes for AI agents working in this repository. Everything below was
verified on this machine on 2026-09-20 unless marked otherwise.

## What this project is

Task Notifier is a single-module native Android reminder app (Kotlin + XML
layouts). Reminders are stored in Room and delivered as notifications driven by
`AlarmManager` exact alarms. See [README.md](README.md) for the user-facing
feature list.

Application ID: `io.github.abdulmoizhussain.tasknotifier`
Current version: `1.0.0-beta.7` (`versionCode` 7)

## Build

The project is pinned to a **verified legacy toolchain**. Do not upgrade AGP,
Gradle, or Kotlin opportunistically — the upgrade is a tracked, separate piece
of work (see [docs/GRADLE_KOTLIN_UPGRADE_PLAN.md](docs/GRADLE_KOTLIN_UPGRADE_PLAN.md)).

| Setting | Value |
| --- | --- |
| Gradle JDK | **JDK 11** (JDK 17 is also installed — do not use it for Gradle) |
| Android Gradle Plugin | 7.1.3 |
| Gradle | 7.2 |
| Kotlin | 1.6.10 |
| compileSdk | 33 |
| targetSdk | 30 |
| minSdk | 16 |
| Java bytecode | Java 8 |

`local.properties` is git-ignored and must exist locally with `sdk.dir`
pointing at the Android SDK.

Build from PowerShell (the `.bat` wrapper needs Windows-style paths, so setting
`JAVA_HOME` from the Bash tool with a POSIX path fails):

```bash
$env:JAVA_HOME='C:\Program Files\Java\jdk-11'; .\gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

Expect four pre-existing Kotlin deprecation/unused-parameter warnings
(`MainActivity.kt`, `TaskStatusEnum.kt`). They are not regressions.

## Run

Use an **emulator**, not the physical device, unless explicitly told otherwise.

The bug under active investigation reproduces on a Samsung SM-A175F running
One UI 8.5 / Android 16 (API 36). An `android-36/google_apis/x86_64` system
image is installed locally, so an API 36 AVD can be created to match.

```bash
C:\Users\Abdul\AppData\Local\Android\Sdk\emulator\emulator.exe -list-avds
```

Install:

```bash
C:\Users\Abdul\AppData\Local\Android\Sdk\platform-tools\adb.exe -s emulator-5554 install -r -t app\build\outputs\apk\debug\app-debug.apk
```

Note: a stale `com.example.tasknotifier` package (the pre-rename application ID)
may also be installed on test devices. Make sure you are inspecting
`io.github.abdulmoizhussain.tasknotifier`.

## Source layout

All under `app/src/main/java/io/github/abdulmoizhussain/tasknotifier/`:

| Area | Files |
| --- | --- |
| UI | `MainActivity.kt`, `ActivityAddTask.kt`, `ActivityViewTask.kt`, `listadapters/ListAdapter.kt` |
| Scheduling | `utils/MyAlarmManager.kt`, `services/TaskService.kt` |
| Delivery | `utils/MyNotificationManager.kt`, `android_services/TaskNotifierAndroidService.kt` |
| Receivers | `broadcast_receivers/` — `SendNotificationBroadcastReceiver`, `OnBootReceiver`, `NotificationDismissedBroadcastReceiver` |
| Data | `data/AppDatabase.kt`, `data/task/`, `repositories/TaskRepository.kt`, `viewmodels/TaskViewModel.kt` |
| Diagnostics | `diagnostics/DiagnosticLog.kt`, `diagnostics/DiagnosticsExporter.kt` |

Room schemas are exported to `app/schemas/` — a schema change requires a
migration and a new schema JSON.

## Diagnostics

The app can export a diagnostics bundle for field debugging. An export from the
affected device is available as a sibling working directory
(`task-notifier-diagnostics-<timestamp>/`) containing:

- `app-state.json` — device/SDK/build, channel importances, active notification IDs
- `tasks.json` — full task rows including reminder text
- `events/*.jsonl` — per-process event logs (`main`, `:ProcessTaskNotifierAndroidService`); these deliberately omit reminder text
- `database/` — SQLite snapshot plus raw DB and WAL/SHM sidecars

Event names to know: `NOTIFICATION_POST_REQUESTED`, `NOTIFICATION_POST_RESULT`,
`NOTIFICATION_POST_FAILED`, `NOTIFICATION_CANCEL_REQUESTED`,
`NOTIFICATION_CANCEL_RESULT`. The `*_RESULT` events carry `active` and
`activeNotificationIds`, which is the ground truth for whether a notification
actually landed in the shade.

Note the app runs in **two processes** — the service runs in
`:ProcessTaskNotifierAndroidService`. Cross-process state assumptions are a
known source of bugs here; always correlate both event logs by timestamp.

## Documentation conventions

- The repository root holds **only** `README.md` and `AGENTS.md`.
- Every other Markdown document lives in `docs/`.
- When a document in `docs/` is superseded by new findings, update it in place
  rather than adding a parallel document, and keep the link list at the bottom
  of `README.md` current.

## Known constraints

- `targetSdk` is 30, so the app does not yet opt into Android 13 notification
  permissions, Android 14 exact-alarm permissions, or Android 14+ foreground
  service types. Several notification-reliability behaviours therefore run in
  legacy-compatibility mode.
- On Android 14+, ongoing notifications are user-dismissible; the app re-posts a
  swiped notification as best-effort persistence.
- Export/import menu entries exist but are not implemented.
