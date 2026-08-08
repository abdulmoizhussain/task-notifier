# Task Notifier

Task Notifier is a native Android reminder app for creating one-time or repeating tasks. Tasks are stored locally and scheduled as exact alarms so that the app can show a notification at the selected date and time.

## Download

[Download Task Notifier v0.0.3 (APK)](https://github.com/abdulmoizhussain/task-notifier/releases/download/v0.0.3/task-notifier-v0.0.3.apk)

## Features

- Create, edit, turn future scheduling on or off, and delete reminder tasks.
- Send a task notification immediately with **Notify Now**.
- Repeat reminders hourly, daily, weekly, monthly, yearly, or at several custom minute/hour/day/week/month intervals.
- Optionally stop a repeating reminder after a selected number of notifications.
- Restore scheduled tasks and active notifications after boot, time changes, or timezone changes.
- Restore an ongoing task notification after it is swiped away while the task still awaits acknowledgement.
- Open the exact task associated with each notification, including when another task detail screen is already open.
- Use a card-based reminder list with clear status indicators, up to four lines of task details, concise scheduling information, an empty state, and streamlined create/edit/detail forms.
- Order reminders by **Latest created** or **Recently modified** and retain the selected order across app launches.
- Persist task data and its creation/modification timestamps locally with Room.
- Use separate default and silent notification channels on Android 8.0 and later.

## Current project configuration

| Setting | Value |
| --- | --- |
| Application ID | `com.example.tasknotifier` |
| Version | `0.0.3` (`versionCode` 3) |
| Minimum Android version | Android 4.1 / API 16 |
| Compile SDK | API 31 |
| Target SDK | API 30 |
| Android Gradle Plugin | 7.1.3 |
| Gradle | 7.2 |
| Kotlin | 1.6.10 |
| Gradle JDK | JDK 11 |
| Java compatibility | Java 8 bytecode |

This is the verified legacy baseline. Android Studio Panda 4 can sync and run it when the project uses the Gradle wrapper and JDK 11. JDK 11 runs Gradle; the application itself continues to emit Java 8-compatible bytecode.

## Build from source

1. Install JDK 11 and an Android SDK containing API 31.
2. Clone the repository and open it in Android Studio.
3. In Android Studio's Gradle settings, select **Wrapper** and JDK 11.
4. Let Android Studio complete the Gradle sync without applying further AGP, Gradle, or Kotlin upgrades.
5. Run the `app` configuration on an emulator or connected Android device.

You can also build a debug APK from the repository root:

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux, use:

```bash
./gradlew assembleDebug
```

The generated APK is placed under `app/build/outputs/apk/debug/`.

## Architecture

The project is a single Android application module built with Kotlin and XML layouts. It uses:

- AndroidX AppCompat, RecyclerView, Lifecycle, and Preference libraries.
- Room for local task persistence.
- `AlarmManager` and broadcast receivers for reminder scheduling.
- Android notification channels and a one-shot service for scheduling alarms and delivering or restoring notifications.
- A repository and `AndroidViewModel` layer between the activities and Room database.

## Known limitations

- Export and import options are visible in the menu but are not implemented.
- Import/export timestamp compatibility is intentionally deferred; a future implementation must preserve creation/modification dates and accept legacy data without them.
- The current target SDK is API 30, so the project needs modernization before publishing an update under current Google Play target API requirements.
- Newer Android versions impose notification, exact-alarm, and background-service requirements that the current implementation does not yet fully handle.
- On Android 14 and later, ongoing notifications are system-dismissible. The app re-posts a swiped active task notification as a best-effort persistence mechanism, but users can still disable notifications or force-stop the app.

## Modernization documents

- [Project and target SDK analysis](ANALYSIS.md)
- [Gradle and Kotlin upgrade plan](GRADLE_KOTLIN_UPGRADE_PLAN.md)

## TODO / future work

- Test thoroughly for lifecycle-related leaks and retained activity/service references.
- Add a 3-dots toggle option for: `Use 24-hour time`.
- Test alarm reliability under Android Doze, battery optimization, and manufacturer-specific background restrictions.
