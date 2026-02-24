# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## Common Commands

- **Build the debug APK**
  ```bash
  ./gradlew assembleDebug
  ```
- **Build the release APK**
  ```bash
  ./gradlew assembleRelease
  ```
- **Run lint**
  ```bash
  ./gradlew lint
  ```
- **Install debug APK on a connected device**
  ```bash
  adb install app/build/outputs/apk/debug/app-debug.apk
  ```
- **Run instrumentation tests** (if any)
  ```bash
  ./gradlew connectedAndroidTest
  ```
- **Run a single instrumentation test** (replace `<TestClass>#<testMethod>`)
  ```bash
  ./gradlew connectedAndroidTest -Ptest.single=<TestClass>#<testMethod>
  ```

## High‑Level Architecture

The application is built in Kotlin and uses Jetpack Compose for UI. Its core functionality is divided into the following layers:

1. **Accessibility Service** – `com.friction.app.accessibility.FrictionAccessibilityService` is the engine that watches for `TYPE_WINDOW_STATE_CHANGED` events. When a protected app launches, it launches the overlay.
2. **Overlay (Friction Wall)** – `com.friction.app.ui.screens.FrictionWallActivity` presents a challenge (breathing, typing, math, etc.). On success it calls `finish()` so the original app continues.
3. **Data Layer** – Room database (`FrictionDatabase`) stores the list of protected apps and user settings. The repository (`AppRepository`) is the single source of truth.
4. **Billing** – `FrictionBillingManager` handles Google Play subscriptions for premium features.
5. **UI & Theme** – Screens under `ui/screens` and shared theme in `ui/theme` provide navigation, home dashboard, paywall, and the add‑app picker.
6. **Utilities** – Helper classes in `utils` provide roast messages, schedule checks, and other small utilities.

The flow is illustrated in the README: a user taps an app → `AccessibilityService` detects the launch → `FrictionWallActivity` is shown on top → user completes the challenge → `finish()` lets the original app load.

## Development Tips

- The app must run on a real device; emulators cannot properly test the `AccessibilityService`.
- To enable the service, go to **Settings → Accessibility → Installed Apps → Friction → Enable**.
- The repository follows Kotlin conventions; all logic lives under `app/src/main/java/com/friction/app`.

## Notes

- No unit tests are included in the repo; instrumentation tests can be added under `app/src/androidTest`.
- For debugging, run `adb logcat` and filter for `Friction` tags.

---

© 2026 Friction Inc. All rights reserved.
