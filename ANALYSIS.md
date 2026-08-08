# Task Notifier Project Analysis

Analysis date: August 8, 2026

This document records the current project analysis after restoring and verifying the legacy application. The Gradle and Kotlin modernization proposal is maintained separately in [`GRADLE_KOTLIN_UPGRADE_PLAN.md`](GRADLE_KOTLIN_UPGRADE_PLAN.md).

## Verified baseline

On August 8, 2026, the legacy project was successfully synced in Android Studio Panda 4 and launched in an emulator after applying only the IDE's minimum compatibility bridge:

- Android Gradle Plugin 7.0.4 → 7.1.3.
- Gradle wrapper 7.0.2 → 7.2.
- Kotlin remained at 1.6.10.
- Gradle continued to run on JDK 11.
- Compile SDK remained 31 and target SDK remained 30.

The historical AGP 7.0.4/Gradle 7.0.2 build was also verified before the bridge: debug compilation, unit tests, lint, APK installation, cold launch on Android 13/API 33, and the existing instrumentation test all succeeded. This confirms that later modernization failures should be treated as upgrade regressions rather than unresolved baseline failures.

## Project summary

Task Notifier is a single-module native Android application written in Kotlin with XML layouts. It stores reminder tasks locally, schedules them with `AlarmManager`, and posts notifications through an Android service.

The application currently supports:

- Creating, editing, enabling, disabling, and deleting reminder tasks.
- Sending an immediate notification with **Notify Now**.
- One-time and repeating reminders.
- Minute, hour, day, week, month, and year repeat intervals.
- Stopping a repeating reminder after a configured number of notifications.
- Local persistence through Room.
- Restoring scheduled tasks and active notifications after boot, time changes, and timezone changes.
- Default and silent notification channels on Android 8.0 and later.

The menu includes export and import actions, but both are currently placeholders and are not implemented.

## Current architecture

The project uses a straightforward layered structure:

- Activities provide the UI for listing, creating, editing, and viewing tasks.
- `TaskViewModel` exposes Room data to the activities.
- `TaskRepository` separates the ViewModel and service layers from `TaskDao`.
- Room stores `Task` entities in the local `task_notifier_database` database.
- `AlarmManager` schedules exact reminder events.
- Broadcast receivers handle alarm delivery, boot completion, time changes, and timezone changes.
- `TaskNotifierAndroidService` restores tasks and sends notifications.

The implementation uses classic Android Views rather than Jetpack Compose. It has no dependency-injection framework and no navigation component.

## Current platform configuration

| Setting | Current value |
| --- | --- |
| Application ID | `com.example.tasknotifier` |
| Version | `0.0.3` (`versionCode` 3) |
| Android Gradle Plugin | 7.1.3 |
| Gradle wrapper | 7.2 |
| Kotlin | 1.6.10 |
| Gradle runtime JDK | JDK 11 |
| Minimum SDK | API 16 / Android 4.1 |
| Compile SDK | API 31 |
| Target SDK | API 30 |
| Java source/target compatibility | Java 8 |
| Database | Room 2.4.0 |
| Lifecycle | Lifecycle 2.4.0 plus `lifecycle-extensions` 2.2.0 |
| Coroutines | 1.5.2 |

The target SDK is the most important platform gap. Starting August 31, 2026, Google Play requires new applications and application updates to target Android 16 / API 36. The current application targets API 30.

Reference: [Google Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en-AU)

## Notification correctness fixes implemented

Two notification defects were addressed on August 8, 2026 without changing the Gradle, Kotlin, compile SDK, or target SDK baseline.

### Persistent-until-acknowledged task notifications

Android 14 and later allow users to dismiss ordinary ongoing notifications by swiping them individually. `setOngoing(true)` therefore cannot provide the old non-dismissible behavior on current Android versions.

Task Notifier now attaches a task-specific notification `deleteIntent`. If a user swipes an ongoing task notification away, `NotificationDismissedBroadcastReceiver` re-queries Room and silently posts it again only when that exact task still exists and has `inProgress == true`. Pressing **Remove this Notification** first turns off the database flag and then cancels the notification; deleting a task also cancels its notification. This preserves the product's persistent-until-acknowledged behavior while respecting the platform dismissal event.

This is best-effort persistence, not a way to override system control. A user can still disable notification permission or channels, force-stop the app, or otherwise prevent reposting.

Reference: [Android 14 changes to non-dismissible notifications](https://developer.android.com/about/versions/14/behavior-changes-all#non-dismissable-notifications)

### Exact task routing from notification taps

Each task notification now uses:

- Its database ID as the pending-intent request code.
- A unique data URI in the form `task-notifier://task/{id}`.
- `FLAG_UPDATE_CURRENT` and `FLAG_IMMUTABLE` where supported.
- `TaskStackBuilder.addNextIntentWithParentStack()` with `MainActivity` declared as the detail activity's parent.

`ActivityViewTask` also processes replacement intents in `onNewIntent()` and redraws every field from the newly selected DB row. This covers both cold launches and taps received while a detail activity is already open.

Regression tests now create temporary Room tasks and verify that two notifications open their respective task rows and that the dismissal callback restores only an active task notification.

### Separate notification presentation

Each task notification now receives its own stable app-defined group key derived from its database ID. This prevents the normal Android auto-grouping path used when an app posts several notifications without group identities. Regression tests verify that different task notifications have different group keys, and emulator notification-manager inspection confirmed the posted keys remain separate without an app-generated summary.

This is a best-effort presentation request rather than an absolute guarantee. Android 16 can auto-group notifications on the app's behalf, including notifications with no summary or only a small number of children, and manufacturer System UI implementations such as One UI can make additional presentation decisions. Android exposes no public “never group” flag that can override those system-level choices.

Reference: [Android notification behavior and grouping](https://developer.android.com/develop/ui/compose/notifications#notification-behavior)

## UI and UX refresh implemented

The existing XML/View interface was refreshed on August 8, 2026 without changing task storage, scheduling, or Gradle/Kotlin versions. The work intentionally prioritizes clarity over decoration:

- The main screen now has a clear heading, an **Order by: Latest** control, card-based tappable rows, concise dates without seconds, status chips, a strong add-reminder action, and an empty state. The current ordering uses the scheduled reminder time in descending order, with the database ID used only as a deterministic tie-breaker when scheduled times match.
- New and edited reminders share a consistent form with clearer field grouping, larger touch targets, visible selection affordances, explicit action wording, and edit-only turn-off/delete controls.
- The notification detail screen presents task information in one readable card and explains the difference between acknowledging the active notification and editing the reminder.
- The theme now uses a restrained indigo/neutral palette, consistent surfaces, outlines, spacing, button hierarchy, and text contrast.
- New XML resources retain compatibility with the existing API 16 minimum.

Emulator QA covered the populated main list and both new/edit form states. Build, unit tests, and lint pass. The three Android instrumentation tests also pass when invoked directly through ADB; the AGP 7.1 Gradle UTP wrapper can abort on the newer emulator runtime because of a protobuf tooling conflict, which is separate from application behavior.

## Areas requiring changes before a target SDK upgrade

### Background service starts

`Context.startService()` is called from activities and from both broadcast receivers. In particular, `OnBootReceiver` and `SendNotificationBroadcastReceiver` can start `TaskNotifierAndroidService` while the application is in the background.

Android 8.0 and later restrict background service starts. Raising the target SDK without redesigning this flow can cause `IllegalStateException`, missed reminders, or service-start failures. The work should be divided according to purpose:

- Short deferrable database and rescheduling work should use WorkManager or another scheduled-work API.
- Work that must begin immediately and remain visible to the user may require a properly declared foreground service.
- A foreground service must call `startForeground()` promptly and display the required notification.
- Newer Android releases place additional restrictions on starting foreground services from the background, so each receiver-to-service path must be tested.

Reference: [Android background execution limits](https://developer.android.com/about/versions/oreo/background)

### Exact alarm access

The app calls `setExact()` and `setExactAndAllowWhileIdle()` but does not declare or manage modern exact-alarm access.

Before targeting current Android versions, the application needs to:

- Decide whether `SCHEDULE_EXACT_ALARM` or the more restricted `USE_EXACT_ALARM` policy is appropriate.
- Check `AlarmManager.canScheduleExactAlarms()` where required.
- Provide a user flow for granting special exact-alarm access.
- Handle denial or later revocation without crashing.
- Reschedule alarms when exact-alarm access becomes available again.
- Confirm that the chosen permission is permitted by Google Play policy for the app's core purpose.

Reference: [Schedule alarms](https://developer.android.com/develop/background-work/services/alarms)

### Notification runtime permission

The manifest does not declare `POST_NOTIFICATIONS`, and the application has no Android 13+ runtime permission flow. On a fresh Android 13 or later installation, ordinary notifications remain disabled until the user grants this permission.

Because notifications are the primary app feature, permission handling should include an explanation, denial behavior, and a route to system settings when permission has been permanently denied.

Reference: [Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)

### PendingIntent mutability

The alarm and notification pending intents now add `FLAG_IMMUTABLE` on Android 6.0 and later. This prerequisite is complete for the currently identified pending-intent call sites. Re-check all call sites when targeting API 31 or later and when introducing any new pending intents.

### Back navigation

`MainActivity`, `ActivityAddTask`, and `ActivityViewTask` override `Activity.onBackPressed()`. This API is deprecated and does not integrate correctly with modern predictive-back behavior.

The activities should register callbacks through `OnBackPressedDispatcher`. Their current custom navigation behavior should also be reviewed because starting a replacement activity or explicitly opening the home screen can create surprising task-stack behavior.

Reference: [Activity back handling](https://developer.android.com/reference/android/app/Activity.html)

### Edge-to-edge layout behavior

Modern target SDK levels enforce or encourage edge-to-edge presentation. The current layouts do not appear to apply window insets explicitly. Every activity should be tested for content hidden behind status bars, navigation bars, display cutouts, and the software keyboard.

The global-layout keyboard-height calculation in `ActivityAddTask` is fragile under modern inset and window-resizing behavior and should be replaced with window-inset/IME visibility APIs.

## Deprecated or obsolete dependencies and APIs

### `lifecycle-extensions`

`androidx.lifecycle:lifecycle-extensions:2.2.0` is obsolete. The project should depend only on the individual Lifecycle artifacts it actually uses, such as LiveData, ViewModel, and their Kotlin extensions.

### kapt

Room compilation currently uses Kotlin kapt. Kapt is in maintenance mode and should be replaced with Kotlin Symbol Processing (KSP). This is also a prerequisite for a clean migration to AGP 9 built-in Kotlin.

Reference: [Migrate from kapt to KSP](https://developer.android.com/build/migrate-to-ksp)

### `android.kotlinOptions`

The module configures `android.kotlinOptions`. AGP built-in Kotlin requires the newer `kotlin.compilerOptions` DSL. With built-in Kotlin, the Kotlin JVM target can inherit `android.compileOptions.targetCompatibility`.

### Legacy Android DSL spellings

The build uses method-style properties including `compileSdkVersion`, `minSdkVersion`, and `targetSdkVersion`. They should be modernized to `compileSdk`, `minSdk`, and `targetSdk` while upgrading the build scripts.

The explicit `buildToolsVersion '31.0.0'` is normally unnecessary because AGP selects an appropriate default Build Tools version.

### Deprecated Gradle `buildDir`

The root clean task reads `rootProject.buildDir`. Gradle deprecated `Project.buildDir` in favor of `Project.layout.buildDirectory`. The task should also be registered lazily rather than created eagerly.

Reference: [Gradle 8 upgrade notes](https://docs.gradle.org/current/userguide/upgrading_version_8.html)

### Legacy build organization

The root build uses a `buildscript` classpath and `allprojects.repositories`. A modern project would normally use:

- The plugins DSL for plugin declarations.
- `pluginManagement` in `settings.gradle` for plugin repositories.
- `dependencyResolutionManagement` in `settings.gradle` for dependency repositories.
- Optionally, a version catalog for centralized dependency and plugin versions.

This is not necessarily an immediate runtime problem, but modernizing it reduces upgrade friction and makes plugin resolution more predictable.

### Gradle behavior changes

The current `gradle.properties` does not override BuildConfig or R-class behavior. During AGP upgrades, adopt the newer defaults where the source permits it. Do not add compatibility flags such as `android.defaults.buildfeatures.buildconfig`, `android.nonTransitiveRClass`, or `android.nonFinalResIds` merely to silence an upgrade warning; introduce one only when a verified source requirement exists and document why.

## Other high-risk implementation areas

These are not all deprecated APIs, but they should be addressed during modernization because they can affect correctness and reliability.

### Blocking coroutines

The application uses `runBlocking` in activities, a service, a broadcast receiver, and `TaskService`. This blocks the caller thread and can cause UI freezes or broadcast/service ANRs.

Recommended direction:

- Use `lifecycleScope` or ViewModel functions for activity work.
- Keep database operations on `Dispatchers.IO` where appropriate.
- Use `BroadcastReceiver.goAsync()` for short asynchronous receiver work, or delegate durable work to WorkManager.
- Give the service its own structured coroutine scope and cancel it in `onDestroy()`.
- Avoid nested `runBlocking { launch { ... } }`, which provides no useful concurrency and still blocks.

### Sticky service lifecycle

`TaskNotifierAndroidService` returns `START_STICKY`, performs synchronous database work, and calls `System.gc()`. Explicit garbage collection should be removed. More importantly, the service responsibilities should be separated into notification delivery, task rescheduling, and persistent work before choosing the appropriate modern Android component for each responsibility.

### Broadcast receiver execution time

`SendNotificationBroadcastReceiver.onReceive()` reads and writes Room data while blocking the receiver thread. Broadcast receivers have a limited execution window. Long database work should not run synchronously inside `onReceive()`.

### Service binding implementation

`TaskNotifierAndroidService.onBind()` throws through `TODO()`. Because this is an unbound service, it should return `null` with the correct nullable return type rather than retaining a crash path.

### Notification implementation

The notification code should be reviewed for:

- Notification permission state before posting.
- Channel-specific behavior, because importance, vibration, and sound are controlled by channels on Android 8.0+.
- A valid monochrome small notification icon rather than the launcher background asset.

Immutable notification pending intents and task-specific tap/back-stack behavior have now been implemented and covered by emulator regression tests.

There is also an apparent channel configuration defect in `AppStartup`: properties intended for `silentChannel` are applied to `channel` after `silentChannel` is constructed.

### Date formatting thread safety

`MyDateFormat` stores shared `SimpleDateFormat` instances in a singleton. `SimpleDateFormat` is not thread-safe. This becomes more risky if blocking code is replaced with properly concurrent coroutines. Prefer `java.time` APIs with core-library desugaring, or create formatter instances in a thread-safe manner.

### Room schema management and backup

The Room database uses version 1 with `exportSchema = false`. Before changing entities or updating Room:

- Enable schema export.
- Commit schema files.
- Add migration tests.
- Decide whether destructive fallback is acceptable; it normally is not for user reminder data.

The manifest enables application backup. Modern backup/data-extraction rules should explicitly define whether reminder data is backed up, restored, or excluded.

### Main-thread and lifecycle behavior

Several activity flows load Room data through `runBlocking` during `onCreate()`. Besides blocking rendering, this may perform UI updates after lifecycle state changes. Data should be exposed as observable state or loaded in lifecycle-aware coroutines.

### Minimum SDK decision

The current minimum is API 16. Retaining it may force older AndroidX versions and increases compatibility code. Raising the minimum SDK is a product decision rather than an automatic build change, but API 23 is a practical baseline to evaluate for a modernized release.

If API 16 support is retained, dependency upgrades must be selected individually according to each library's minimum-SDK requirements.

## Suggested verification matrix

When modernization work begins, verify at least:

- Fresh install with notifications allowed, denied, and later enabled.
- Exact-alarm access allowed, denied, and revoked.
- One-time and every repeat interval scheduling.
- Stop-after behavior for every configured count.
- Device reboot with future and in-progress tasks.
- Manual clock and timezone changes.
- Doze and battery optimization behavior.
- Force-stop and subsequent manual relaunch behavior.
- Notification taps from a cold and warm process.
- Editing, disabling, and deleting scheduled tasks.
- Android versions around API 23, 26, 31, 33, 35, and 36, depending on the final minimum SDK.
- Room migration and backup/restore behavior.
- Release builds with R8 enabled.

## Analysis scope

This report combines static source inspection with baseline build and emulator verification. The initial baseline verification changed only the minimal AGP 7.1.3 and Gradle 7.2 compatibility bridge required by Panda 4. Subsequent notification correctness work changed the notification manager, task-detail handling, manifest receiver/parent declarations, task deletion cleanup, and focused instrumentation tests; it did not change Gradle, Kotlin, SDK, or dependency versions.
