# Game Test Package Guide

## Purpose

`app/src/test/java/com/example/cw/game` contains JVM tests for deterministic gameplay helpers in `com.example.cw.game`.

## Files

- `GameLogicTest.kt`: Verifies fleet launch math, player base upgrades, obstacle-aware route generation, selected-base upgrade visibility at `maxLevel`, selection cleanup after ownership changes during simulation, the five-second enemy AI think cadence, and canvas label layout for base-level number placement across normal and small rendered node sizes. It uses a shared `matchState(...)` fixture helper so state-shape changes stay localized.

## Subfolders

- `levels/`: Tests for JSON parsing, authored level validation, summary sorting, unlock rules, and authored-to-runtime mapping. See `levels/levels_test_developer_guide.md`.

## Working In This Folder

- Add tests here when the production code can run without Android framework services.
- Prefer small state fixtures and direct calls into extracted helpers so failures point to a single gameplay rule.
- Keep base fixtures internally consistent with authored rules, including `cap = capLevel * 10`.
