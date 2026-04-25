# Game Test Package Guide

## Purpose

`app/src/test/java/com/example/cw/game` contains JVM tests for deterministic gameplay helpers in `com.example.cw.game`.

## Files

- `CampaignPersistenceTest.kt`: Verifies round-trip serialization for completed levels and per-level star records, defensive parsing of malformed persisted data, current spent-star persistence, and schema-version gating so unsupported save formats fall back to a safe empty campaign state while older save formats still load without inventing extra spendable stars.
- `LevelCardStyleTest.kt`: Verifies the menu-only level-card style helper returns the intended high-contrast palette for completed and unlocked missions, checks that the locked-card text still clears explicit contrast thresholds against the locked surface, and keeps the shared status copy stable for completed, available, and locked cards.
- `GameTapLaunchMessageTest.kt`: Verifies tap-triggered attacks and friendly reinforcements keep ship-count launch messaging without the redundant batch-status banner, covers both single-source and multi-source selections so combined launches report a truthful total ship count, and checks mixed-success plus all-sources-fail launch batches so the shared helper keeps message responsibility in one place.
- `GameLogicTest.kt`: Verifies fleet launch math, player base upgrades, the round-number upgrade cost schedule and insufficiency messaging, AI spending against that same upgrade schedule, obstacle-aware route generation, reduced passive-funds accrual with explicit arithmetic notes, capture-level downgrade behavior after successful assaults including over-cap survivor cases, friendly-arrival over-cap reinforcement handling plus later decay toward cap on follow-up ticks, tap-selection behavior that keeps HUD messages free of selection text while also clearing selection on empty-space taps or stale non-player selections, short-lived onboarding hints for invalid enemy-target taps, preservation of those hints until their configured expiry even when the player immediately taps a valid base, selected-base upgrade visibility at `maxLevel`, compact HUD summary formatting for the floating player-funds readout, pause toggling for the HUD action, pause-safe match stepping, star-award thresholds, campaign best-star accumulation, spendable-star accounting, the post-victory best-star delta flow plus transition-safety for already-finished wins, floating upgrade-pill offset clamping near viewport edges, safe-drawing inset boundaries, and bottom-edge cases, selection cleanup after ownership changes during simulation, the five-second enemy AI think cadence, capped-out max-level AI pressure attacks only when the odds are reasonable, and canvas label layout for the overlapping bottom-edge level badge across normal and small rendered node sizes using the relative badge offset model, including visibility clamps near the bottom canvas boundary while selection remains a non-text visual state. The upgrade-pill geometry assertions use the same fallback size constants as `GameInGameUi.kt` so test fixtures stay aligned with the live overlay.
- `UpgradeAnimationTest.kt`: Verifies the UI helper that detects when a selected player base has just leveled up, ensures selection swaps do not trigger the animation, keeps the distinction between a selected player base and an actually upgradable one explicit so max-level upgrades can still animate on their final tier, and covers the extracted animation runner so upgrade-ring overlay state is cleared both after a normal completion and after coroutine cancellation.

## Subfolders

- `levels/`: Tests for JSON parsing, authored level validation, summary sorting, unlock rules, and authored-to-runtime mapping. See `levels/levels_test_developer_guide.md`.

## Working In This Folder

- Add tests here when the production code can run without Android framework services.
- Prefer small state fixtures and direct calls into extracted helpers so failures point to a single gameplay rule.
- Keep base fixtures internally consistent with authored rules, including `cap = capLevel * 10`.
