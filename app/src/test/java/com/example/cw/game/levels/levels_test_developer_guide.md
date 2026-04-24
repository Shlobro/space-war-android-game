# Level Test Package Guide

## Purpose

`app/src/test/java/com/example/cw/game/levels` contains JVM tests for the authored level pipeline.

## Files

- `LevelRepositoryTest.kt`: Verifies JSON parsing, validation failures, star-threshold ordering and maximum rules, repository failure reporting for malformed files, catalog sorting, unlock rules, and mapping from level definitions into runtime match state including the clean no-banner start for new missions.

## Working In This Folder

- Add tests here whenever the shared level format, validation rules, or repository behavior changes.
- Prefer file-free fixtures built from inline JSON strings so failures stay easy to read.
- Keep inline base JSON aligned with authored capacity tiers, where `capLevel` 1 means a `cap` of 10 and each additional level adds 10 more.
