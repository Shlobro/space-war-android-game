# Game Package Guide

## Purpose

`com.example.cw.game` contains the full prototype gameplay loop: screen flow, campaign state, match simulation, canvas rendering, and world helpers.

## Files

- `CampaignPersistence.kt`: SharedPreferences-backed campaign save and load helpers plus compact serialization for completed levels and per-level star records. Saves now write an explicit schema version so unsupported future formats can be rejected instead of being parsed as partial progress.
- `GameApp.kt`: Top-level Compose entry for the game feature. Owns screen navigation, campaign state, active match state, packaged level loading, the frame-timed simulation loop, and persistence wiring for campaign progress. Campaign progress records each level's best star result, persists across app restarts, and adds only the improvement delta to the player's total star currency while still granting upgrade points on first completion.
- `GameScreens.kt`: Menu-only UI for the home screen, asset-backed level selection, and the upgrade screen. These screens use one consistent `Stars` label for the total star currency, each level card shows the best earned star row, and the upgrade screen remains the only place that surfaces meta-progression spending.
- `GameInGameUi.kt`: In-match HUD, pause overlay, level-end overlay, the selected-base upgrade affordance, and small in-game UI formatting helpers. The top HUD uses a gold funds chip plus an icon-based pause action, and the chip label is sourced from Android string resources so HUD text stays centralized.
- `GameCanvas.kt`: Custom canvas rendering for the starfield, obstacles, bases, fleet trails, and fleets. Base labels show current unit count in the center and the base level as a small number near the bottom edge using radius-scaled layout so the label stays inside small rendered nodes.
- `GameUiKit.kt`: Shared visual tokens and reusable Compose primitives such as the menu background, divider, action buttons, and stat rows used by both menu screens and overlays.
- `GameLogic.kt`: Input handling, fleet launching, base upgrades, frame stepping, AI decisions, ship production, combat resolution, arrival handling, capture-level downgrade handling, and selection cleanup when ownership changes. `stepMatch` treats pause as a hard no-op so elapsed time and economy cannot advance if a caller accidentally invokes it while paused, clears short-lived HUD hints after they expire, and empty-space taps now clear the current player selection without overwriting the active HUD message.
- `GameWorld.kt`: Match creation from authored level definitions, route building around obstacles, coordinate conversion, and shared world geometry helpers. New matches start with an empty HUD banner so authored helper text does not cover the playfield.
- `GameModel.kt`: Immutable state models and enums for campaign progress, match state, world bounds, bases, fleets, obstacles, ownership, and base types. Campaign state stores per-level star records, total star currency is derived from those records, invalid star awards are rejected at the write path, and match state tracks elapsed time plus the stars earned on the current result.

## Subfolders

- `levels/`: Shared authored level schema, JSON parsing, repository loading, and campaign unlock helpers. See `levels/levels_developer_guide.md`.

## Runtime Flow

1. `MainActivity` renders `GameApp`.
2. `GameApp` loads packaged level summaries from `assets/levels` through `levels/AssetLevelRepository`.
3. When the user starts a level, `createMatch` maps the authored level definition into `MatchState`.
4. While a level is running, the `LaunchedEffect` loop calls `stepMatch` once per rendered frame with a capped delta time.
5. `GameCanvas` renders the latest state, and tap handlers call `onScreenTap` or `upgradeBase`.

## Key Gameplay Rules

- Player sends launch `floor(current units * 0.5)` ships from a base.
- Fast bases launch fleets at higher movement speed.
- Bases produce units over time until they hit cap, and over-cap bases decay back toward cap.
- Base capacity is tiered by `capLevel`, with `cap = capLevel * 10`; each upgrade raises both by one tier until `maxLevel`, after which the selected-base upgrade pill is hidden.
- Funds accrue for each non-neutral owner at a slower passive rate of `0.6 + 0.25 * owned bases` per second before campaign cash-rate multipliers apply, which keeps early cap upgrades from snowballing too quickly.
- Level completion awards between 1 and 3 stars based on authored completion-time thresholds stored in the level JSON; replaying a level improves the stored record only when the new run earns more stars.
- Capturing a base reduces its `capLevel` by two tiers, but never below 1; surviving attackers keep their full post-combat count even if that temporarily exceeds the new captured cap.
- Friendly reinforcements also keep their full arrival count even if that temporarily pushes the destination base above its current cap; normal production then decays any over-cap garrison back toward cap over time.
- Tapping player-owned bases only changes selection state, empty-space taps clear the current selection, and tapping enemy or neutral targets with nothing selected shows only a brief onboarding hint instead of a persistent help banner; the message banner is reserved for actionable feedback such as launch, upgrade, pause, win, loss, and error states.
- The in-game HUD does not expose per-AI money totals, level title text, or a remaining-rivals chip; it keeps only the player-funds readout and pause action on screen so the playfield stays less cluttered.
- Each configured AI controller evaluates nearby non-owned bases every five seconds and attacks when it has a large enough unit advantage; otherwise it buys cap upgrades until a base is both full and at `maxLevel`, at which point that base pressures the weakest nearby non-owned target only when it has at least a modest numerical edge instead of idling or suiciding into a stronger defense.
- Route generation inserts detour waypoints when a direct line intersects an obstacle buffer.

## Extension Points

- Add new authored entity fields in `levels/` before wiring them into runtime behavior.
- Add new base or fleet modifiers in `GameModel.kt` and apply them in `GameLogic.kt` plus `GameCanvas.kt`.
- Keep UI-only changes in `GameScreens.kt` or `GameCanvas.kt`; keep deterministic gameplay rules in `GameLogic.kt` so they stay unit-testable.
- When adding a new AI owner, AI type, node type, or authored level field, update the external editor in the same change so runtime and authoring stay on one schema.
