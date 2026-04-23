# CW Test Package Guide

## Purpose

`app/src/test/java/com/example/cw` holds JVM unit tests for logic that does not require an Android runtime.

## Files

- `ExampleUnitTest.kt`: Default template smoke test from the Android project scaffold.

## Subfolders

- `game/`: Gameplay-focused JVM tests for simulation, world helpers, and authored level mapping. See `game/game_test_developer_guide.md`.

## Working In This Package

- Prefer adding tests to the most specific child package that matches the production code under test.
- Keep Android-dependent behavior in instrumented tests instead of growing JVM tests with platform assumptions.
