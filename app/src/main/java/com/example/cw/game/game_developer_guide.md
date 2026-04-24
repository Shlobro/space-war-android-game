# Game Package Guide

## Purpose

`com.example.cw.game` contains the full prototype gameplay loop: screen flow, campaign state, match simulation, canvas rendering, and world helpers.

## Files

- `GameApp.kt`: Top-level Compose entry for the game feature. Owns screen navigation, campaign state, active match state, packaged level loading, and the frame-timed simulation loop.
- `GameScreens.kt`: Menu, asset-backed level selection, upgrade screen, in-game HUD, pause overlay, level-end overlay, the contextual message card for launch and upgrade feedback, and the selected-base upgrade affordance that only appears for player-owned bases still below `maxLevel`. The floating upgrade-cost pill measures itself and clamps its screen offset against both the viewport bounds and safe-drawing insets so it stays unobscured near screen edges on edge-to-edge devices.
- `GameCanvas.kt`: Custom canvas rendering for the starfield, obstacles, bases, fleet trails, and fleets. Base labels show current unit count in the center and the base level as a small number near the bottom edge using radius-scaled layout so the label stays inside small rendered nodes.
- `GameLogic.kt`: Input handling, fleet launching, base upgrades, frame stepping, AI decisions, ship production, combat resolution, arrival handling, and selection cleanup when ownership changes.
- `GameWorld.kt`: Match creation from authored level definitions, route building around obstacles, coordinate conversion, and shared world geometry helpers.
- `GameModel.kt`: Immutable state models and enums for campaign progress, match state, world bounds, bases, fleets, obstacles, ownership, and base types.

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
- Tapping player-owned bases only changes selection state, and stale-selection cleanup paths clear invalid selection without replacing the current HUD message; the message banner is reserved for actionable feedback such as launch, upgrade, pause, win, loss, and error states.
- Each configured AI controller evaluates nearby non-owned bases every five seconds and attacks when it has a large enough unit advantage; otherwise it buys cap upgrades.
- Route generation inserts detour waypoints when a direct line intersects an obstacle buffer.

## Extension Points

- Add new authored entity fields in `levels/` before wiring them into runtime behavior.
- Add new base or fleet modifiers in `GameModel.kt` and apply them in `GameLogic.kt` plus `GameCanvas.kt`.
- Keep UI-only changes in `GameScreens.kt` or `GameCanvas.kt`; keep deterministic gameplay rules in `GameLogic.kt` so they stay unit-testable.
- When adding a new AI owner, AI type, node type, or authored level field, update the external editor in the same change so runtime and authoring stay on one schema.
