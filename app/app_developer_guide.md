# App Module Guide

## Purpose

The `app` module contains the Android application packaging, the Compose UI entry point, packaged level assets, and local tests for the prototype strategy game.

## Top-Level Files

- `build.gradle.kts`: Android application module configuration, Compose enablement, and test dependencies.
- `proguard-rules.pro`: Release shrinker configuration.

## Main Source Layout

- `src/main/AndroidManifest.xml`: Declares the application and launcher activity.
- `src/main/java/com/example/cw/MainActivity.kt`: Hosts the Compose app and applies the shared theme.
- `src/main/java/com/example/cw/game/`: Detailed gameplay implementation, including runtime level loading from packaged JSON assets. See `src/main/java/com/example/cw/game/game_developer_guide.md`.
- `src/main/java/com/example/cw/ui/theme/`: Material theme tokens and typography for the app shell.
- `src/main/assets/`: Packaged runtime data, including level JSON files shared with the editor. See `src/main/assets/assets_developer_guide.md`.
- `src/main/res/`: String, color, theme, launcher icon, and backup resource files.

## Test Layout

- `src/test/java/com/example/cw/`: Unit tests for JVM-safe logic. See `src/test/java/com/example/cw/cw_test_developer_guide.md`.
- `src/androidTest/java/com/example/cw/`: Instrumented Android test scaffolding for device or emulator runs.

## Working In This Module

- Keep gameplay logic in `com.example.cw.game` so it can be tested without moving UI-only files.
- Put authored runtime content in `src/main/assets` rather than encoding it directly in Kotlin.
- Put detailed feature documentation in the nearest package guide and link to it from parent guides instead of duplicating explanations.
- When the authored level vocabulary changes, update both the Android runtime and the external editor in the same change so they continue to read and write the same schema.
