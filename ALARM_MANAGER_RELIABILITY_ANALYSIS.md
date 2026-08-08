# AlarmManager Reliability Analysis

Analysis date: August 9, 2026

This document evaluates Task Notifier's current alarm and notification delivery pipeline against Android Doze, app standby and hibernation, user-controlled battery restrictions, force-stop behavior, and manufacturer-specific controls such as Xiaomi/Redmi MIUI and HyperOS.

No implementation changes were made as part of this analysis.

## Executive conclusion

The application's core AlarmManager API choice is appropriate for user-visible reminders, but the complete delivery pipeline is only moderately reliable on current Android and is weak on aggressively managed Xiaomi/Redmi devices.

| Situation | Expected reliability |
| --- | --- |
| Samsung, Realme, or stock Android; app not restricted | Good |
| Ordinary Doze; individual reminders more than approximately nine minutes apart | Good at the alarm-trigger level; moderate-to-good end to end |
| One-minute or five-minute repeats during Doze | Poor; Android deliberately throttles while-idle alarms |
| App placed in Android's **Restricted** battery mode | Very poor; alarms may not be delivered |
| Redmi/Xiaomi with default optimization and background autostart disabled | Low and potentially inconsistent |
| Redmi/Xiaomi with **No restrictions**, background autostart enabled, and notifications allowed | Significantly better, but not an absolute guarantee |
| After a device reboot | Moderate-to-low because recovery relies on a fragile background-service handoff |
| After force-stop | None until the user directly or indirectly interacts with the app again |
| After app hibernation | None until the user opens the app; existing alarms are not automatically restored by Android |

The reported Redmi failures are consistent with both the current source and Xiaomi's documented background controls. Android 16 itself is not the sole explanation: the source's receiver-to-service handoff and Xiaomi's additional process/autostart restrictions combine to create the largest risk.

## Current scheduling and delivery pipeline

Task Notifier currently uses this sequence:

```text
User saves reminder
    -> TaskNotifierAndroidService queries future enabled rows
    -> AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP)
    -> explicit PendingIntent targets SendNotificationBroadcastReceiver

At the selected time
    -> AlarmManager wakes the application and invokes the receiver
    -> receiver loads and updates the Room task
    -> receiver starts TaskNotifierAndroidService to post the notification
    -> receiver starts TaskNotifierAndroidService again to schedule the next occurrence
```

The relevant source areas are:

- `app/src/main/java/com/example/tasknotifier/utils/MyAlarmManager.kt`
- `app/src/main/java/com/example/tasknotifier/services/TaskService.kt`
- `app/src/main/java/com/example/tasknotifier/broadcast_receivers/SendNotificationBroadcastReceiver.kt`
- `app/src/main/java/com/example/tasknotifier/broadcast_receivers/OnBootReceiver.kt`
- `app/src/main/java/com/example/tasknotifier/android_services/TaskNotifierAndroidService.kt`
- `app/src/main/AndroidManifest.xml`

## What the current implementation does correctly

### Correct Doze-capable alarm API

On Android 6.0 and later, Task Notifier schedules:

```kotlin
alarmManager.setExactAndAllowWhileIdle(
    AlarmManager.RTC_WAKEUP,
    triggerAtMillis,
    pendingIntent,
)
```

This is the appropriate AlarmManager API when a user-visible reminder needs to occur at a nearly precise wall-clock time, including while the device is in Doze.

`RTC_WAKEUP` tells the system that the alarm is based on wall-clock time and may wake the CPU when the screen is off. This matches reminders selected as a date and time. A duration-only timer would be a better match for an elapsed-real-time clock, but that is not Task Notifier's current model.

Reference: [Android: Schedule alarms](https://developer.android.com/develop/background-work/services/alarms)

### System-owned alarms survive application process death

Once a PendingIntent alarm has been accepted by AlarmManager, it is owned by the Android system. It does not depend on `TaskNotifierAndroidService` remaining alive. Returning `START_NOT_STICKY` and stopping the one-shot service does not cancel scheduled alarms or already-posted notifications.

The alarm does not, however, survive device reboot, force-stop, app hibernation, exact-alarm permission revocation, package removal, or user/manufacturer restrictions that explicitly suppress it.

### Stable task identities

The database task ID is used as the PendingIntent request code. This gives each task a stable alarm identity and prevents different tasks from normally replacing one another. Rescheduling the same task replaces its previous alarm, which is desirable when the user edits its date or time.

The cancellation path reconstructs an explicit intent targeting the same receiver with the same request code. Intent extras do not participate in PendingIntent identity, so omitting the task-ID extra when reconstructing the cancellation PendingIntent does not by itself prevent cancellation.

### One-shot repeat scheduling

Repeating reminders are implemented by scheduling one exact alarm and creating the following one after delivery. This gives the application control over custom minute, hour, day, week, month, and year intervals and avoids Android's inexact repeating-alarm behavior.

### Recovery broadcasts exist

The manifest receiver listens for:

- `BOOT_COMPLETED`
- `TIME_SET`
- `TIMEZONE_CHANGED`

This is the correct set of core events to consider for alarms based on wall-clock time. The implementation attempts to query Room and re-register future enabled tasks after these events.

## Doze limitations that still apply

`setExactAndAllowWhileIdle()` avoids the normal rule that defers standard alarms until a Doze maintenance window, but it is not unlimited.

Android documents an app-level rate limit for `setAndAllowWhileIdle()` and `setExactAndAllowWhileIdle()` alarms. Current developer guidance states that they cannot fire more than approximately once per nine minutes per app while idle. The precise platform implementation and device configuration may vary.

Consequences for Task Notifier:

- **Every minute** cannot be guaranteed every minute during Doze.
- **Every five minutes** cannot be guaranteed every five minutes during Doze.
- Several different tasks due within a short period share the same app-level quota.
- Later alarms may be deferred even though each alarm was requested as exact.
- Battery saver, standby buckets, and OEM policies can apply further limits.

Reference: [Android: Optimize for Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)

### Repeat drift

When an alarm is delivered, `Constants.getNextOccurrence()` starts from the current clock time rather than the task's previous scheduled time.

If a 2:00 PM hourly reminder is delivered at 2:15 PM, its next occurrence becomes approximately 3:15 PM rather than 3:00 PM. This avoids emitting a burst of missed occurrences but means that repeated delays permanently move the schedule.

The application should eventually define and document one of these policies:

- fixed cadence based on the original schedule;
- elapsed interval based on actual delivery;
- skip missed occurrences and select the next future point in the original cadence.

The current behavior implements the second policy implicitly.

## Largest application-level reliability weakness

The alarm receiver does not complete the user-visible reminder itself. It performs a database update and then starts an ordinary service, in a separate process, to post the notification and schedule the next alarm.

AlarmManager holds a CPU wake lock while `BroadcastReceiver.onReceive()` is executing. It releases that wake lock when `onReceive()` returns. Android explicitly warns that if the receiver calls `Context.startService()`, the device can sleep before the requested service has actually launched.

Reference: [Android `AlarmManager` API reference](https://developer.android.com/reference/android/app/AlarmManager)

This produces the following failure window:

1. AlarmManager successfully delivers the alarm broadcast.
2. The receiver reads the task and advances its Room state.
3. The receiver requests one or two service starts.
4. The receiver returns and AlarmManager releases its wake lock.
5. Android or the OEM delays, rejects, or kills the service process before it posts the notification or registers the next alarm.
6. Room now indicates a future repeat occurrence even though no matching AlarmManager entry exists.

This can look like a random AlarmManager failure even though AlarmManager successfully triggered the receiver.

The service is also declared with:

```xml
android:process=":ProcessTaskNotifierAndroidService"
```

That adds a secondary process cold start. It is not inherently incorrect, but it adds cost and another process boundary at the exact moment Xiaomi/Redmi is deciding whether background execution should be allowed.

## Why repeating reminders have a single point of failure

The next alarm is registered only after the current alarm is delivered. This is normally a sound design, but only if the delivery path reliably reaches the rescheduling statement or has recovery.

If the service responsible for scheduling does not run:

- the database can contain the calculated next date;
- there may be no Android alarm for that date;
- normal `MainActivity` launch does not reconcile Room rows with AlarmManager;
- the task can remain silently unscheduled until an edit, a successful boot/time-change recovery, or the manual **Restart Service** action.

The source already contains a TODO describing the risk that tasks may not be scheduled after an update while still appearing scheduled in the main list.

## Reboot and time-change recovery weaknesses

Android clears AlarmManager registrations when the device reboots. Task Notifier correctly has a boot receiver, but that receiver immediately calls an ordinary background `startService()`.

The app targets API 30, so Android 8.0+ background-service restrictions apply. `BOOT_COMPLETED` is an allowed manifest broadcast, but receiving it does not make every ordinary background-service design reliable on every platform or OEM. A future target-SDK upgrade makes this path even more important to redesign.

The restoration query selects only tasks where:

```sql
status = On AND dateTime >= current time
```

If boot recovery is delayed until after a reminder's selected time, that reminder is not restored. There is no explicit missed-reminder policy and no calculation of the next valid repeat occurrence for overdue rows.

## Android user-controlled battery modes

### Unrestricted

This is the strongest user setting for Task Notifier. It reduces ordinary Android background limitations. It does not override force-stop, hibernation, disabled notifications, exact-alarm permission revocation, or every manufacturer policy.

### Optimized

A single `setExactAndAllowWhileIdle()` reminder should normally work on standard Android when all of the following are true:

- the app has not been force-stopped;
- it has not been hibernated;
- notifications and the relevant channel are enabled;
- exact alarms are permitted for the app's target SDK;
- the app is not in a severe standby bucket;
- the manufacturer does not add stronger restrictions.

This is consistent with successful Samsung and Realme testing.

### Restricted

Task Notifier should not be considered reliable in Restricted mode. Android's guidance states that restricted apps can have alarms and jobs suppressed. Current resource-limit guidance lists the Restricted standby state as allowing as little as one alarm per day, exact or inexact.

References:

- [Android: Background optimization](https://developer.android.com/topic/performance/background-optimization)
- [Android: Power-management resource limits](https://developer.android.com/topic/performance/power/power-details)

## Xiaomi, Redmi, MIUI, and HyperOS

Xiaomi exposes controls beyond standard Android Doze and the ordinary per-app battery choice. Its official support material documents that app battery saver can restrict background activity and affect real-time notifications. Xiaomi also exposes a separate background-autostart permission.

For a reminder app on Redmi/HyperOS, the user should check all of the following. Exact paths and labels vary by device, region, MIUI version, and HyperOS version.

1. Open the app's battery settings and choose **No restrictions**.
2. Open Apps -> Permissions -> Background autostart and enable Task Notifier.
3. In Security -> Boost speed -> Lock apps, lock Task Notifier if that device provides this option. This prevents one-tap background cleanup from clearing it.
4. Enable app notifications and both Task Notifier notification channels.
5. Enable lock-screen notification presentation if desired.
6. Check Do Not Disturb rules and channel sound/vibration settings.
7. Avoid global Ultra battery saver while depending on reminders.
8. Disable **Pause app activity if unused** for this app if reminders must remain valid for months without opening it.
9. Do not force-stop the application.

Official Xiaomi references:

- [Xiaomi: Background autostart](https://www.mi.com/my/support/faq/details/KA-497677/)
- [Xiaomi: No restrictions](https://www.mi.com/uk/support/faq/details/KA-538010/)
- [Xiaomi: Background App lock](https://www.mi.com/global/support/faq/details/KA-535435/)

Background App lock is supplementary. It is not the same as **No restrictions**, Android's battery-optimization exemption, or Xiaomi's background-autostart permission.

No normal application API can silently grant these Xiaomi settings. The app can inspect some standard Android state and guide the user, but manufacturer screens and policies vary.

## Force-stop behavior

Force-stop is an intentional hard boundary. An app cannot design around the user's decision to force-stop it.

Starting with Android 15, the system cancels all PendingIntents when an app enters the stopped state. AlarmManager alarms based on those PendingIntents therefore disappear. The package remains stopped until a direct or indirect user action removes it from that state.

When the user later launches the app, Task Notifier currently does not automatically reconcile and restore all future enabled reminders. That makes its post-force-stop recovery incomplete even after the user returns.

Reference: [Android 15 behavior changes: stopped package state](https://developer.android.com/about/versions/15/behavior-changes-all)

## App hibernation

Because Task Notifier targets API 30, it is eligible for modern unused-app restrictions and hibernation. After months without direct user interaction, Android can hibernate the app.

Alarm deliveries, scheduled jobs, and implicit broadcasts do not count as the kind of user interaction that prevents hibernation. While hibernated, the application cannot run background jobs or alerts. When the user opens it again, Android does not automatically restore the alarms that existed before hibernation.

The current main-screen launch path does not perform complete alarm reconciliation, so opening the app after hibernation may still leave database rows without matching system alarms.

Reference: [Android: App hibernation](https://developer.android.com/topic/performance/app-hibernation)

## Exact-alarm permission and the current target SDK

The current app targets API 30. Exact-alarm special-access enforcement applies when an app targets API 31 or later, so the legacy build can currently call `setExactAndAllowWhileIdle()` on newer Android releases without declaring `SCHEDULE_EXACT_ALARM`.

That is a legacy compatibility behavior, not a future-safe design.

Before raising the target SDK, the app needs:

- a justified choice between `SCHEDULE_EXACT_ALARM` and the policy-limited `USE_EXACT_ALARM`;
- a `canScheduleExactAlarms()` check;
- an explanation and route to the **Alarms & reminders** special-access screen when necessary;
- a receiver for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`;
- rescheduling when access is granted;
- graceful behavior when access is denied or revoked;
- protection against `SecurityException` at every exact-alarm scheduling call.

On fresh installs targeting Android 13 or later, `SCHEDULE_EXACT_ALARM` is generally denied by default. Revoking it stops the app and cancels future exact alarms.

Reference: [Android: Exact alarms denied by default](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)

## Notification delivery is a separate failure domain

An alarm can fire correctly while the user sees no notification.

The current manifest does not declare `POST_NOTIFICATIONS`, and the app does not implement an explicit Android 13+ notification-permission flow. Since the app targets API 30, the platform controls the compatibility permission-dialog timing. If the user denies notifications, or disables the default channel, AlarmManager and Room can operate while nothing is visible.

Redmi also exposes additional lock-screen notification, notification-effect, channel, DND, and battery controls. These can make an AlarmManager delivery appear to have failed.

Field diagnostics should distinguish these checkpoints:

```text
Was an alarm successfully registered?
Did AlarmManager invoke the receiver?
Did the receiver load and update the intended Room row?
Did the notification-posting code execute?
Did NotificationManager accept the notification?
Were notification permission and channel importance enabled?
Did the OEM suppress sound, vibration, or lock-screen presentation?
Was the following repeat alarm actually registered?
```

Reference: [Android: Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)

## Can the receiver do the work directly?

Yes. For Task Notifier's current local work, a service is not inherently required after AlarmManager triggers the receiver.

A more direct conceptual pipeline is:

```text
AlarmManager
    -> SendNotificationBroadcastReceiver
        -> goAsync()
        -> load and validate the Room task
        -> calculate and persist the next state
        -> register the next alarm when applicable
        -> post the notification directly with NotificationManager
        -> finish the PendingResult
```

The important constraints are:

- `BroadcastReceiver.onReceive()` is intended for short work.
- AlarmManager's wake lock lasts through synchronous `onReceive()`.
- For short suspend/Room work, `goAsync()` can keep the broadcast active while a properly owned coroutine finishes.
- The `PendingResult` must be completed promptly and exactly once.
- Long-running, network-heavy, or retryable work should not be kept inside the receiver.
- A recovery/reconciliation path is still needed because Room and AlarmManager cannot be updated as one atomic transaction.

Posting a local notification, updating one Room row, and registering one following alarm are normally short operations. They do not need a continuously running service. Removing the two receiver-to-service handoffs would eliminate an important timing and process-start failure window.

WorkManager is suitable for durable, deferrable secondary work, but not as a replacement for the exact user-visible alarm itself. Doze and quotas can defer WorkManager. A foreground service is appropriate only when genuinely longer, immediately user-visible work must continue beyond the receiver's short window.

## Why an Activity should not replace the service

An Activity is a user interface, not a general background execution container.

Launching an Activity automatically when a reminder fires would have several problems:

- Android 10 and later restrict background Activity launches.
- It can interrupt whatever the user is currently doing.
- It can unexpectedly appear over another application or the lock screen.
- OEMs can block or alter the launch.
- It gives the reminder alarm-screen behavior even when the product only promised a notification.
- It creates poor accessibility and task-stack behavior.
- Google Play and Android reserve full-screen notification behavior for narrow urgent categories such as alarms and calls.

The correct ordinary-reminder model is:

```text
alarm fires -> receiver posts notification -> user taps notification -> Activity opens
```

Task Notifier already uses this model for its notification content intent. The detail Activity is opened only after user interaction.

If the product later adds an explicit **critical alarm** mode, it could evaluate `setAlarmClock()` plus a properly declared and policy-compliant full-screen notification intent. That would be a distinct, highly visible feature with user consent; it should not be the default implementation for every task.

Reference: [Android: Display time-sensitive notifications](https://developer.android.com/develop/ui/views/notifications/time-sensitive)

## Recommended future direction

The following is an analysis recommendation, not an implemented change.

### Highest priority

1. Complete short alarm handling directly in the receiver with `goAsync()` and structured coroutine ownership.
2. Post the notification and register the next occurrence without two ordinary background-service starts.
3. Add an idempotent reconciliation function that compares enabled future Room rows with required alarms.
4. Run reconciliation on ordinary app launch, boot, time/timezone changes, package replacement, and exact-alarm permission grant.
5. Define a missed-reminder and repeat-cadence policy.
6. Add exact-alarm special-access handling before raising target SDK.
7. Add notification runtime-permission handling.

### Xiaomi/Redmi reliability UX

1. Add a reliability/status screen showing standard Android states the app can inspect.
2. Explain **No restrictions** and background-autostart requirements without pretending the app can grant them.
3. Link to appropriate settings screens using supported standard intents where possible.
4. Avoid hard-coding a single Xiaomi component as the only route because component names vary across MIUI/HyperOS releases.
5. Explain force-stop and hibernation limitations clearly.

### Scheduling policy

1. Retain exact alarms for reminders where the user explicitly expects precise delivery.
2. Avoid one-minute/five-minute exact wakeups as a guaranteed background feature; disclose their Doze limitation or redesign that feature.
3. Consider an explicit critical-alarm mode only if the product meaning and store policies justify alarm-clock behavior.
4. Keep local alarm delivery independent of network access.

### Verification matrix

Test independently on Pixel/AOSP, Samsung/One UI, Realme, and multiple Redmi/HyperOS versions:

- screen on and app foreground;
- screen off before Doze;
- forced light and deep Doze through ADB;
- Optimized, Unrestricted, and Restricted battery modes;
- Xiaomi background autostart off and on;
- Xiaomi App battery saver versus No restrictions;
- one-time reminders and every repeat interval;
- several tasks scheduled close together;
- device reboot and delayed boot completion;
- manual time and timezone changes;
- notification permission allowed and denied;
- notification channel enabled and disabled;
- exact-alarm access granted, denied, and revoked;
- process kill without force-stop;
- explicit force-stop followed by user relaunch;
- simulated app hibernation followed by user relaunch;
- missed-alarm recovery and repeat-cadence behavior.

Instrumentation should log each checkpoint separately so a hidden notification is not misclassified as a missing alarm.

## Final assessment

Task Notifier's use of `RTC_WAKEUP` and `setExactAndAllowWhileIdle()` is fundamentally sound for ordinary user-facing reminders. This explains why it performs acceptably on Samsung and Realme under non-restricted settings.

The main reliability problem is not that the service stops after scheduling. AlarmManager registrations remain system-owned. The main problem is that, after an alarm fires, critical work is delegated through two normal service starts and a separate process without a durable retry or ordinary-launch reconciliation path.

Therefore, the current version is:

- reasonably dependable for normal one-time reminders on permissive devices;
- intentionally unable to honor sub-nine-minute repeats during prolonged Doze;
- unreliable in Android Restricted battery mode;
- unreliable on Redmi unless users enable the appropriate Xiaomi settings;
- unable to operate after force-stop or hibernation until the user returns;
- not yet ready for an unchanged target-SDK upgrade;
- a good candidate for direct, short receiver handling instead of the current service handoff.

