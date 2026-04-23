# JVM Test Root Guide

## Purpose

`app/src/test/java` is the root of the JVM unit test tree for logic that can run without an Android device.

## Subfolders

- `com/example/cw/`: Unit tests for runtime-agnostic game systems. See `com/example/cw/cw_test_developer_guide.md`.

## Working In This Folder

- Prefer placing tests in the deepest package that matches the production code under test.
- Keep parsing, validation, and deterministic gameplay behavior covered here so regressions are caught without emulator runs.
