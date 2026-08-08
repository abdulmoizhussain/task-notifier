# Task Notifier

Task Notifier is a native Android reminder app for creating one-time or repeating tasks. Tasks are stored locally and scheduled as exact alarms so that the app can show a notification at the selected date and time.

## Download

[Download Task Notifier v0.0.3 (APK)](https://github.com/abdulmoizhussain/task-notifier/releases/download/v0.0.3/task-notifier-v0.0.3.apk)

## Features

- Create, edit, enable, disable, and delete reminder tasks.
- Send a task notification immediately with **Notify Now**.
- Repeat reminders hourly, daily, weekly, monthly, yearly, or at several custom minute/hour/day/week/month intervals.
- Optionally stop a repeating reminder after a selected number of notifications.
- Restore scheduled tasks and active notifications after boot, time changes, or timezone changes.
- Persist task data locally with Room.
- Use separate default and silent notification channels on Android 8.0 and later.

## Current project configuration

| Setting | Value |
| --- | --- |
| Application ID | `com.example.tasknotifier` |
| Version | `0.0.3` (`versionCode` 3) |
| Minimum Android version | Android 4.1 / API 16 |
| Compile SDK | API 31 |
| Target SDK | API 30 |
| Android Gradle Plugin | 8.2.0 |
| Gradle | 8.2 |
| Kotlin | 1.6.21 |
| Java compatibility | Java 8 bytecode |

AGP 8.2 requires JDK 17 to run Gradle, even though the application currently emits Java 8-compatible bytecode.

## Build from source

1. Install JDK 17 and an Android SDK containing API 31.
2. Clone the repository and open it in Android Studio.
3. Let Android Studio complete the Gradle sync.
4. Run the `app` configuration on an emulator or connected Android device.

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
- Android notification channels and a service for delivering and restoring notifications.
- A repository and `AndroidViewModel` layer between the activities and Room database.

## Known limitations

- Export and import options are visible in the menu but are not implemented.
- The current target SDK is API 30, so the project needs modernization before publishing an update under current Google Play target API requirements.
- Newer Android versions impose notification, exact-alarm, and background-service requirements that the current implementation does not yet fully handle.

TODO / Future Work
- Scan thoroughly for any memory leaks. 
- Android Dove mode - is this still a problem in the latest android mobiles, like it used to? bcoz you know various mobile brands have some OS customizations that used to limit the background tasks and AlarmManager working..
- 