# Gradle and Kotlin Upgrade Plan

Plan refreshed: August 8, 2026

This document starts from the verified working legacy baseline. It is a plan only; the proposed modernization versions have not been applied.

For application and target-SDK prerequisites, see [`ANALYSIS.md`](ANALYSIS.md).

## Verified working baseline

| Component | Current value | Status |
| --- | --- | --- |
| Android Studio | Panda 4 / 2025.3.4 | Successfully syncs the project |
| Android Gradle Plugin | 7.1.3 | Minimum compatibility bridge accepted by Panda 4 |
| Gradle wrapper | 7.2 | Correct pairing for AGP 7.1 |
| Gradle runtime JDK | JDK 11 | Required for this AGP generation |
| Kotlin Gradle plugin | 1.6.10 | Working but obsolete |
| Kotlin annotation processing | kapt | Working for Room 2.4.0 but in maintenance mode |
| Java source/target | Java 8 | Working application bytecode target |
| Kotlin JVM target | JVM 8 | Configured through `android.kotlinOptions` |
| Compile SDK | 31 | Working but outdated |
| Target SDK | 30 | Working but below current Play requirements |
| Minimum SDK | 16 | Working but restricts dependency choices |

The project originally used AGP 7.0.4 and Gradle 7.0.2. That exact historical combination was successfully compiled, tested, installed, and launched using JDK 11. Panda 4 then required the small AGP 7.1.3/Gradle 7.2 bridge for supported IDE import. Kotlin, dependencies, source, manifest, compile SDK, and target SDK were unchanged.

Baseline verification completed:

- Gradle wrapper startup on JDK 11.
- `assembleDebug`.
- `testDebugUnitTest`.
- `lintDebug`.
- APK installation and cold launch on Android 13/API 33.
- Existing `connectedDebugAndroidTest` test.
- Successful Panda 4 Gradle sync and user-confirmed emulator launch.

On August 8, 2026, notification regression coverage was expanded to three connected tests. The suite verifies task-specific notification routing and restoration of an active notification after its dismissal callback. `assembleDebug`, unit tests, lint, and all connected tests pass on the legacy toolchain.

This baseline should be committed or tagged before further upgrades.

References: [AGP 7.0 compatibility](https://developer.android.com/build/releases/agp-7-0-0-release-notes), [AGP and Gradle compatibility](https://developer.android.com/build/releases/about-agp)

## Recommended destination

The long-term destination, based on stable releases available on the analysis date, is:

| Component | Target |
| --- | --- |
| Android Gradle Plugin | 9.3.1 |
| Gradle wrapper | 9.5.0 |
| Gradle runtime JDK | 17 |
| Kotlin integration | AGP built-in Kotlin |
| Kotlin language level | 2.4 |
| Standalone Kotlin version during an AGP 8 checkpoint | 2.4.10 |
| Annotation processing | KSP |
| Java/Kotlin JVM target | 17 |
| Compile SDK | 36 initially; consider 37 separately |
| Target SDK | 36 |

AGP 9.3 supports API 37 and requires Gradle 9.5 and JDK 17. Kotlin 2.4 class files require AGP 8.5.2 or newer. Kotlin 2.4.10 is the current bug-fix release in the Kotlin 2.4 line.

References: [AGP 9.3 release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes), [Kotlin releases](https://kotlinlang.org/docs/releases.html), [Kotlin and AGP compatibility](https://developer.android.com/build/kotlin-support)

## Upgrade principles

- Preserve a runnable checkpoint at every phase.
- Separate build-tool upgrades from target-SDK behavior changes.
- Change one major compatibility boundary at a time: Gradle runtime JDK, AGP, Kotlin, annotation processing, dependencies, and target SDK.
- Do not accept a large collection of Android Studio Upgrade Assistant changes without reviewing the exact diff.
- Run the same build and emulator checks after every checkpoint.
- Keep database compatibility and existing user reminders intact.
- Do not combine an optional Groovy-to-Kotlin-DSL rewrite with required build upgrades.

## Phase 0: Preserve the baseline

Status: ready.

1. Review the AGP 7.1.3 and Gradle 7.2 compatibility bridge.
2. Commit or tag the working baseline.
3. Preserve the known-good debug APK for comparison.
4. Record current lint findings and runtime behavior.
5. Keep Android Studio configured to use the wrapper and JDK 11 while on AGP 7.x.

Exit criteria:

- Clean, recoverable Git baseline.
- Panda 4 sync succeeds.
- Debug build, tests, lint, installation, and launch succeed.

## Phase 1: Prepare code without raising target SDK

Keep target SDK 30 initially. Implement upgrade-sensitive changes while existing behavior can still be compared with the verified baseline:

1. Replace deprecated `onBackPressed()` overrides with `OnBackPressedDispatcher` callbacks.
2. Add explicit immutable flags to notification activity pending intents. **Completed August 8, 2026**, together with unique task identities and warm-activity routing tests.
3. Remove `System.gc()` and make the unbound service's `onBind()` return `null`.
4. Replace `runBlocking` on UI, receiver, and service paths with structured coroutines.
5. Separate notification delivery, alarm rescheduling, and durable background work responsibilities.
6. Prepare notification runtime-permission handling without raising the target SDK yet.
7. Prepare exact-alarm access checks and denial behavior.
8. Add window-inset/IME handling for modern edge-to-edge behavior.
9. Enable Room schema export and add migration tests before changing Room.

Detailed reasoning is in [`ANALYSIS.md`](ANALYSIS.md#areas-requiring-changes-before-a-target-sdk-upgrade).

Exit criteria:

- Existing features still work at target SDK 30.
- New code paths are covered by tests where practical.
- No database schema change occurs without a migration test.

## Phase 2: Move to the final AGP 7 line

Recommended checkpoint:

- AGP 7.4.2.
- Gradle 7.5.
- JDK 11.
- Keep Kotlin 1.6.10 for the first sync/build.

Procedure:

1. Upgrade only AGP and the Gradle wrapper.
2. Sync and run the full validation suite.
3. Add the required `namespace` declaration if it has not already been introduced.
4. Remove the explicit Build Tools version if AGP's selected version works.
5. Modernize simple Android DSL spellings where supported.
6. After AGP 7.4.2 is stable, upgrade Kotlin separately to 1.9.25.
7. Build and test again before continuing.

Why this checkpoint exists:

- It closes the AGP 7 generation before crossing into AGP 8.
- Kotlin 1.9 requires AGP 7.4.2 or newer according to Android's Kotlin compatibility table.
- It keeps JDK 11 while isolating Kotlin compiler changes from the JDK 17 transition.

## Phase 3: Move to AGP 8 and JDK 17

Recommended checkpoint:

- AGP 8.13.x.
- Gradle 8.13.
- Gradle runtime JDK 17.
- Kotlin 2.4.10 using the Kotlin Android plugin temporarily.
- KSP for Room.

Do this in smaller commits even if Android Studio presents it as one upgrade:

1. Change the Gradle runtime from JDK 11 to JDK 17.
2. Upgrade AGP and its compatible Gradle wrapper.
3. Resolve AGP 8 DSL, namespace, manifest, and build-feature changes while keeping the existing Kotlin version.
4. Run the full validation suite.
5. Upgrade Kotlin from 1.9.25 to 2.4.10 and resolve K2 compiler findings.
6. Replace kapt with KSP for Room.
7. Update Room and its compiler together.
8. Run the full validation suite again.

At this phase, modernize build organization:

- Replace the root `buildscript` classpath with the plugins DSL.
- Move plugin repositories to `pluginManagement` in `settings.gradle`.
- Move dependency repositories to `dependencyResolutionManagement`.
- Optionally introduce `gradle/libs.versions.toml`.
- Replace eager task creation and deprecated `rootProject.buildDir` access with lazy APIs and `layout.buildDirectory`.
- Adopt current BuildConfig and R-class defaults where possible; add legacy compatibility flags only for a verified source requirement.

## Phase 4: Upgrade dependencies and compile SDK

Update dependencies in groups so failures remain attributable:

1. Core, AppCompat, Activity, Material, and ConstraintLayout.
2. Lifecycle and ViewModel; remove `lifecycle-extensions`.
3. Room runtime, Room KTX, KSP compiler, and Room testing.
4. Coroutines.
5. RecyclerView and Preference.
6. JUnit, AndroidX Test, and Espresso.

Then:

1. Raise compile SDK to 36 while keeping target SDK 30 temporarily.
2. Resolve compile-time API changes and lint findings.
3. Decide whether min SDK 16 remains a requirement.
4. If adopting current libraries requires a higher minimum, evaluate API 23 as a practical baseline and document the user impact.

Do not choose dependency versions solely because they are the newest. Verify each artifact's minimum SDK and compatibility with Kotlin, KSP, and Room.

## Phase 5: Raise target SDK to 36

Only start this phase after the application prerequisites in `ANALYSIS.md` are implemented.

1. Raise target SDK one behavior boundary at a time where useful, testing after each step.
2. Complete Android 13+ notification permission handling.
3. Complete exact-alarm permission/access handling and Play policy review.
4. Replace invalid background service starts with appropriate WorkManager or foreground-service flows.
5. Add any required foreground-service permissions and service types.
6. Validate pending-intent mutability on API 31+.
7. Validate edge-to-edge and predictive-back behavior.
8. Add modern backup/data-extraction rules.
9. Test Doze, battery optimization, reboot, clock changes, timezone changes, and permission revocation.
10. Confirm Google Play target-API and policy compliance.

Starting August 31, 2026, new applications and updates submitted to Google Play must target API 36.

Reference: [Google Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en-AU)

## Phase 6: Migrate to AGP 9 built-in Kotlin

Final checkpoint:

- AGP 9.3.1.
- Gradle 9.5.0.
- JDK 17.
- Built-in Kotlin.
- KSP.

Required changes:

1. Remove `kotlin-android` from the app module.
2. Remove the standalone Kotlin Gradle plugin classpath/version from the root build.
3. Ensure no kapt usage remains.
4. Replace `android.kotlinOptions` with `kotlin.compilerOptions` only where explicit options are still needed.
5. Let Kotlin's JVM target inherit Java target compatibility unless a documented override is required.
6. Resolve AGP 9 new-DSL issues without relying on the temporary legacy-DSL opt-out as the final state.
7. Run build, lint, unit, instrumentation, Room migration, and manual alarm/notification tests.

AGP documents temporary `android.builtInKotlin=false` and `android.newDsl=false` opt-outs, but they should be emergency migration aids rather than the intended result.

Reference: [Migrate to built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)

## Important version cautions

- Do not run the current Gradle 7.2 build on JDK 21; use JDK 11.
- Do not move to AGP 8 without moving the Gradle runtime to JDK 17.
- Do not introduce Kotlin 2.4 before reaching AGP 8.5.2 or newer.
- Do not move to AGP 9 while leaving `kotlin-android` enabled unless deliberately using the temporary documented opt-out.
- Prefer KSP over `com.android.legacy-kapt`; legacy kapt is a fallback, not the destination.
- Do not raise target SDK to 36 as a build-number-only change.
- Do not assume current AndroidX releases still support min SDK 16.
- Keep the Gradle runtime JDK separate conceptually from the application's Java/Kotlin bytecode target.

## Validation gate for every phase

Run at least:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

With an emulator or device connected:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Also verify:

- Debug APK installation and cold launch.
- Release build with R8 enabled.
- Creation, editing, disabling, deletion, and immediate notification.
- One-time and repeating alarm delivery.
- Stop-after behavior.
- Notification tap/back-stack behavior.
- Reboot and time/timezone rescheduling.
- Room data preservation across the update.
- Denied/revoked notification and exact-alarm access.
- Doze and manufacturer battery restrictions.

Do not advance to the next phase until failures are understood and the current checkpoint is recoverable in Git.
