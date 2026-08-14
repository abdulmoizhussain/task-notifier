# Task Notifier v1.0.0-beta.7

This is the first beta in the new 1.0 release line and a substantial update since [v0.0.3](https://github.com/abdulmoizhussain/task-notifier/releases/tag/v0.0.3), with a redesigned interface, more reliable scheduling and notifications, diagnostic tools, and a new visual identity.

> [!IMPORTANT]
> **Upgrade notice:** The application ID has changed from `com.example.tasknotifier` to `io.github.abdulmoizhussain.tasknotifier`. Android treats this release as a separate app, not an in-place update to v0.0.3. Existing reminders and settings will not transfer automatically. Keep the old installation until you have recreated any reminders you still need. **Export diagnostics is a troubleshooting feature, not a supported backup/import workflow.**

## Highlights

### Refreshed reminder experience

- Redesigned the reminder list with clearer cards, scheduling information, status indicators, and an empty state.
- Streamlined the create, edit, and reminder-detail screens with clearer action labels.
- Added a vertically resizable description field and scrolling for long reminder details.
- Added **Latest created** and **Recently modified** ordering, with the selected order retained across app launches.
- Added creation and modification timestamps to reminders, including a Room database migration for the new fields.
- Preserved the reminder-list scroll position when returning from the create/edit screen.

### Notification and scheduling reliability

- Fixed notifications so each one opens its own associated reminder, even when several notifications are active.
- Active reminder notifications that are swiped away are restored silently until the reminder is acknowledged.
- Fixed **Turn off scheduling** so it cancels the future alarm without incorrectly removing an already-active notification.
- Prevented stale alarm broadcasts from reactivating reminders whose scheduling has been turned off.
- Made the scheduling service stop after completing its work while keeping its alarms and notifications active.
- Preserved creation and modification timestamps during alarm-driven reminder updates.
- Improved service-restart, boot, time-change, and timezone-change recovery behavior.

### Diagnostics

- Added **Export diagnostics** to help investigate notification and scheduling issues that develop over time.
- Diagnostic ZIP files include app state, reminder data as JSON, event logs, a queryable SQLite snapshot, and the raw database with any available WAL/SHM sidecar files.
- Added structured logging around alarms, services, notification delivery and dismissal, database updates, and restart operations.

### New visual identity

- Added a new bell-with-checkmark launcher icon.
- Added adaptive launcher support and an Android 13+ monochrome layer for system-themed light and dark icons.
- Added correctly sized legacy launcher assets for older supported Android versions.
- Added a dedicated monochrome notification status-bar icon.

## Developer and compatibility changes

- Changed the application ID and source package to `io.github.abdulmoizhussain.tasknotifier`.
- Updated the project toolchain and synchronized it with a newer Android Studio/Gradle environment.
- Raised the compile SDK to API 33 for Android 13 themed-icon support while retaining Android 4.1 / API 16 as the minimum supported version.
- Expanded unit and instrumentation coverage for database migration, diagnostics export, notification routing and restoration, scheduling state, service lifecycle, ordering, and scroll restoration.
- Release APK filenames now include the app name, version name, and version code.

## Known limitations

- Task backup/import remains unimplemented; the diagnostics export should not be treated as a restore file.
- This is a beta release. Additional modernization is still required for current Google Play target API requirements and newer Android background-execution behavior.

**Full changelog:** [v0.0.3...v1.0.0-beta.7](https://github.com/abdulmoizhussain/task-notifier/compare/v0.0.3...v1.0.0-beta.7)
