# MiniNote

Offline-first Android notes app (Kotlin, Jetpack Compose, Material 3). Package: `app.mininote.mininote`.

## Features

- Email/password auth (register, login, forgot-password) against a configurable backend
- Notes with categories, starring, and search (online via API, offline via local search fallback)
- Fully offline-capable: all writes go local-first (Room) and sync through a background outbox queue once connectivity returns
- Runtime-switchable server address (gear icon on the login screen)
- Responsive layout: bottom navigation + push detail on phone, navigation rail + two-pane list-detail on tablet (window width ≥ 840dp)
- Profile (theme toggle, password change), "About" screen with feedback form

## Requirements

- JDK 25 (managed via `gradle/gradle-daemon-jvm.properties`, no local JDK setup needed if using the Gradle wrapper)
- Android SDK with `compileSdk`/`targetSdk` 37 installed
- A running instance of the MiniNote backend API (see `mini-note-design/` for the UX/API reference used during development)

## Building

```bash
./gradlew assembleDebug        # build debug APK
./gradlew testDebugUnitTest    # JVM unit tests
./gradlew lintDebug            # Android lint
./gradlew connectedAndroidTest # instrumented tests; needs an emulator/device
```

By default the app points at `10.0.2.2:9077` (the Android emulator's alias for the host machine), base path `/api/v1`. Change or add servers from the login screen's gear icon.

## Project structure

- `app/` — the single Gradle module; all application code lives under `app/src/main/java/app/mininote/mininote/`
- `mini-note-design/` — a standalone, buildless HTML/React prototype used as the UX/visual design reference; not part of the Gradle build

Dependency versions are centralized in `gradle/libs.versions.toml`.
