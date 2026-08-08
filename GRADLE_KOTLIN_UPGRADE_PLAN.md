# Gradle and Kotlin Upgrade Plan

Plan date: August 8, 2026

This document proposes a build-tool modernization path. It is an analysis and implementation plan only; the versions and configuration described here have not yet been applied.

For the broader application and deprecated-code review, see [`ANALYSIS.md`](ANALYSIS.md).

## Current state

| Component | Current version or setting | Assessment |
| --- | --- | --- |
| Android Gradle Plugin | 8.2.0 | Old but currently paired with a compatible Gradle version |
| Gradle wrapper | 8.2 | Correct minimum for AGP 8.2 |
| Gradle runtime JDK | JDK 17 required | Already the required baseline for AGP 8.2+ |
| Kotlin Gradle plugin | 1.6.21 | Very old; predates the K2 compiler |
| Kotlin annotation processing | kapt | Maintenance mode and incompatible with AGP 9 built-in Kotlin |
| Java source/target | Java 8 | Valid but unnecessarily old for a fully modernized project |
| Kotlin JVM target | JVM 8 | Configured through the legacy `android.kotlinOptions` DSL |
| Compile SDK | 31 | Outdated |
| Target SDK | 30 | Below current Google Play requirements |
| Build Tools | Explicitly pinned to 31.0.0 | Normally should follow AGP's default |

AGP 8.2 and Gradle 8.2 are internally compatible, and Kotlin 1.6 is supported by AGP 8.2. Therefore, the current problem is age and migration distance rather than an invalid version combination.

Reference: [AGP and Gradle compatibility](https://developer.android.com/build/releases/about-agp)

## Recommended destination

The recommended final build stack is:

| Component | Target |
| --- | --- |
| Android Gradle Plugin | 9.3.1 |
| Gradle wrapper | 9.5.0 |
| Gradle runtime JDK | 17 |
| Kotlin integration | AGP built-in Kotlin |
| Kotlin language/API level | 2.4 where supported by the built-in compiler |
| Standalone Kotlin version, if temporarily required on AGP 8 | 2.4.10 |
| Annotation processing | KSP |
| Java source/target | 17 |
| Kotlin JVM target | Inherit Java target compatibility or explicitly use 17 |
| Compile SDK | 36 |
| Target SDK | 36 |

AGP 9.3 supports API 37 and requires Gradle 9.5 and JDK 17. AGP 9.3.1 is the latest patch listed in the current release notes.

Reference: [AGP 9.3 release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes)

Kotlin 2.4.10 is the latest Kotlin 2.4 bug-fix release. Android's compatibility table requires AGP 8.5.2 or later for Kotlin 2.4 class files, so Kotlin 2.4 must not be introduced while the project remains on AGP 8.2.

References: [Kotlin releases](https://kotlinlang.org/docs/releases.html), [Kotlin and AGP compatibility](https://developer.android.com/build/kotlin-support)

## Why a staged upgrade is recommended

Moving directly from AGP 8.2/Kotlin 1.6 to AGP 9.3 combines several independent migrations:

- Gradle 8 to Gradle 9.
- AGP 8's legacy DSL to AGP 9's new DSL interfaces.
- External Kotlin Android plugin to built-in Kotlin.
- Kotlin 1.6 compiler to Kotlin 2.4/K2 behavior.
- kapt to KSP.
- Java/JVM 8 to 17.
- Compile and target SDK 31/30 to 36.
- Multiple years of AndroidX, Room, and coroutines updates.
- Android runtime behavior changes for notifications, exact alarms, services, and edge-to-edge UI.

A staged approach creates smaller failure sets and makes regressions easier to diagnose.

## Phase 0: Establish a reproducible baseline

Before editing build files:

1. Use JDK 17 for both Android Studio Gradle and command-line Gradle.
2. Record the active Android Studio version and installed SDK components.
3. Run and save results for:
   - `assembleDebug`
   - `assembleRelease`
   - `testDebugUnitTest`
   - `connectedDebugAndroidTest`, when a device is available
   - `lintDebug`
4. Record existing warnings rather than treating all warnings after the upgrade as new.
5. Manually test alarm creation, notification delivery, repeat scheduling, boot restoration, editing, disabling, and deletion.
6. Preserve a known-good APK for behavior comparison.

Do not start the version upgrade until the current branch either builds successfully or its existing failures are documented.

## Phase 1: Prepare application code and dependencies

Perform target-SDK-sensitive application changes before the final AGP jump:

1. Implement Android 13+ notification permission handling.
2. Implement modern exact-alarm permission/access handling.
3. Replace background `startService()` paths with WorkManager or compliant foreground-service flows.
4. Add explicit immutable pending-intent flags.
5. Replace `onBackPressed()` overrides with `OnBackPressedDispatcher` callbacks.
6. Add window-inset handling and test edge-to-edge layouts.
7. Replace blocking `runBlocking` flows with structured, lifecycle-aware coroutines.
8. Enable Room schema export and add database migration tests.

Then update runtime dependencies in small groups rather than all at once:

1. Core, AppCompat, Activity, and Material.
2. Lifecycle and ViewModel; remove `lifecycle-extensions`.
3. Room runtime and compiler.
4. Coroutines.
5. RecyclerView, ConstraintLayout, Preference, test libraries, and Espresso.

Each group should be built and tested before continuing. Current AndroidX releases may require raising the minimum SDK. Decide whether to retain API 16 or move to a newer baseline such as API 23 before selecting exact library versions.

## Phase 2: Upgrade to the final AGP 8 line

Use AGP 8.13 and Gradle 8.13 as an intermediate checkpoint.

This phase should include:

1. Upgrade AGP from 8.2.0 to 8.13.x.
2. Upgrade the Gradle wrapper from 8.2 to 8.13.
3. Keep JDK 17.
4. Upgrade Kotlin from 1.6.21 to 2.4.10.
5. Migrate Room from kapt to KSP.
6. Set compile SDK and target SDK to 36.
7. Remove the explicit Build Tools version unless a verified requirement remains.
8. Move Java and Kotlin bytecode targets to 17.
9. Resolve all Kotlin K2 compiler errors and warnings.
10. Run all baseline build and behavioral checks.

This intermediate checkpoint separates Kotlin, KSP, SDK, and dependency problems from AGP 9's built-in-Kotlin and new-DSL changes.

## Phase 3: Modernize the build layout

Before or during the AGP 8 checkpoint:

1. Replace the root `buildscript` classpath with the plugins DSL.
2. Move plugin repositories to `pluginManagement` in `settings.gradle`.
3. Move dependency repositories to `dependencyResolutionManagement`.
4. Optionally introduce `gradle/libs.versions.toml` for version management.
5. Replace method-style Android properties:
   - `compileSdkVersion 31` → `compileSdk 36`
   - `minSdkVersion 16` → `minSdk 16` or the newly selected minimum
   - `targetSdkVersion 30` → `targetSdk 36`
6. Replace the eager clean task and deprecated `rootProject.buildDir` access with lazy APIs using `layout.buildDirectory`.
7. Review and remove unnecessary compatibility flags from `gradle.properties`.
8. Keep the namespace and application ID explicit and separate.

Migrating Groovy scripts to Kotlin DSL is optional. It can improve IDE assistance, but it should not be combined with the core toolchain upgrade unless there is a clear benefit, because it adds another source of errors.

## Phase 4: Migrate to AGP 9.3.1

AGP 9 enables built-in Kotlin by default. The project must be adjusted accordingly:

1. Upgrade AGP to 9.3.1.
2. Upgrade the Gradle wrapper to 9.5.0.
3. Continue using JDK 17.
4. Remove the `kotlin-android` plugin from the app module.
5. Remove the Kotlin Gradle plugin classpath/version from the root build.
6. Remove `kotlin-kapt`; Room should already be using KSP.
7. Replace `android.kotlinOptions` with `kotlin.compilerOptions`, if explicit compiler settings remain necessary.
8. Confirm that Kotlin's JVM target matches Java target compatibility.
9. Resolve new DSL and removed API errors without opting out when a supported migration exists.
10. Run the complete build, lint, test, and manual behavior matrix again.

Android documents temporary opt-outs through `android.builtInKotlin=false` and `android.newDsl=false`, but these should be emergency transition aids rather than the planned final state. The opt-outs are not a durable solution for later AGP releases.

Reference: [Migrate to built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)

## Required build-script migrations

### Kotlin Android plugin

Current:

```groovy
plugins {
    id 'com.android.application'
    id 'kotlin-android'
}
```

AGP 9 destination:

```groovy
plugins {
    id 'com.android.application'
    id 'com.google.devtools.ksp'
}
```

Built-in Kotlin replaces only the Kotlin Android plugin. KSP remains a separate plugin.

### kapt to KSP

Current:

```groovy
id 'kotlin-kapt'
kapt 'androidx.room:room-compiler:<version>'
```

Destination:

```groovy
id 'com.google.devtools.ksp'
ksp 'androidx.room:room-compiler:<version>'
```

Reference: [Migrate from kapt to KSP](https://developer.android.com/build/migrate-to-ksp)

### Kotlin compiler options

Current:

```groovy
android {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}
```

Destination direction:

```groovy
android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}
```

With built-in Kotlin, the Kotlin JVM target defaults to the Android Java target compatibility. Add `kotlin.compilerOptions` only when another compiler setting must be explicit.

### Gradle clean task

Current:

```groovy
task clean(type: Delete) {
    delete rootProject.buildDir
}
```

Destination direction:

```groovy
tasks.register('clean', Delete) {
    delete layout.buildDirectory
}
```

Reference: [Gradle `buildDir` deprecation](https://docs.gradle.org/current/userguide/upgrading_version_8.html)

## Version-selection cautions

- Do not upgrade Kotlin to 2.4 while remaining on AGP 8.2. Kotlin 2.4 class files require AGP 8.5.2 or later.
- Do not install AGP 9 while leaving `kotlin-android` enabled unless intentionally using the documented temporary opt-out flags.
- Do not leave Room on `kapt` for the intended AGP 9 destination. Prefer KSP; `com.android.legacy-kapt` is only a fallback.
- Do not raise target SDK to 36 as a purely build-file change. Runtime notification, exact-alarm, background-service, and layout behavior must be updated and tested.
- Do not blindly update every AndroidX artifact to its latest version while retaining min SDK 16. Check each artifact's minimum SDK first.
- Do not update AGP without updating the Gradle wrapper to a compatible version.
- JDK 17 is the Gradle runtime requirement. Java/Kotlin bytecode compatibility is a separate setting, although moving the project to JVM 17 is recommended.

## Validation gates

Every phase should pass these gates before continuing:

### Build gate

- Gradle sync completes without errors.
- Debug and release APKs build.
- R8 completes for the minified release build.
- No unsupported Gradle or AGP API warning remains unexplained.

### Test gate

- Unit tests pass.
- Instrumentation tests pass on the selected minimum and current Android versions.
- Lint has no newly introduced errors.
- Room schema and migration tests pass.

### Runtime gate

- Notification permission flows work.
- Exact-alarm permission flows work.
- Alarm delivery remains reliable in Doze.
- Reboot and time/timezone changes reschedule correctly.
- Notification taps produce the intended back stack.
- Background work does not throw service-start exceptions.
- Layouts render correctly edge-to-edge.

### Release gate

- A signed release build installs and launches.
- The release build preserves Room data across an app update.
- Play Console pre-launch and target-API checks pass.
- Required permissions and foreground-service declarations comply with Play policy.

## Recommended outcome

The best balance of currency and maintainability is AGP 9.3.1 with Gradle 9.5, JDK 17, AGP built-in Kotlin, Kotlin 2.4 language level, KSP, and compile/target SDK 36.

The recommended path is not a single version-number edit. First make the application compatible with the current Android platform, then establish an AGP 8.13/Kotlin 2.4/KSP checkpoint, and finally migrate to AGP 9.3.1 built-in Kotlin.
