# Game Test Package Guide

## Purpose

`app/src/test/java/com/example/cw/game` contains JVM tests for deterministic gameplay helpers in `com.example.cw.game`.

## Files

- `CampaignPersistenceTest.kt`: Verifies round-trip serialization for completed levels and per-level star records, defensive parsing of malformed persisted data, and schema-version gating so unsupported save formats fall back to a safe empty campaign state while legacy unversioned saves still load.
- `GameLogicTest.kt`: Verifies fleet launch math, player base upgrades, obstacle-aware route generation, reduced passive-funds accrual with explicit arithmetic notes, capture-level downgrade behavior after successful assaults, friendly-arrival cap clamping, tap-selection behavior that keeps HUD messages free of selection text while also clearing selection on empty-space taps or stale non-player selections, selected-base upgrade visibility at `maxLevel`, compact HUD summary formatting for player funds, pause toggling for the HUD action, pause-safe match stepping, star-award thresholds, campaign best-star accumulation, the post-victory campaign update flow, floating upgrade-pill offset clamping near viewport edges, safe-drawing inset boundaries, and bottom-edge cases, selection cleanup after ownership changes during simulation, the five-second enemy AI think cadence, and canvas label layout for base-level number placement across normal and small rendered node sizes. The upgrade-pill geometry assertions use the same fallback size constants as `GameInGameUi.kt` so test fixtures stay aligned with the live overlay.

## Subfolders

- `levels/`: Tests for JSON parsing, authored level validation, summary sorting, unlock rules, and authored-to-runtime mapping. See `levels/levels_test_developer_guide.md`.

## Working In This Folder

- Add tests here when the production code can run without Android framework services.
- Prefer small state fixtures and direct calls into extracted helpers so failures point to a single gameplay rule.
- Keep base fixtures internally consistent with authored rules, including `cap = capLevel * 10`.
