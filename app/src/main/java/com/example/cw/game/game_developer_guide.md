# Game Package Guide

## Purpose

`com.example.cw.game` contains the full prototype gameplay loop: screen flow, campaign state, match simulation, canvas rendering, and world helpers.

## Files

- `CampaignPersistence.kt`: SharedPreferences-backed campaign save and load helpers plus compact serialization for completed levels, per-level star records, bonus star credits, and spent-star totals. Saves write an explicit schema version so unsupported future formats can be rejected instead of being parsed as partial progress.
- `GameApp.kt`: Top-level Compose entry for the game feature. Owns screen navigation, campaign state, active match state, packaged level loading, the frame-timed simulation loop, and persistence wiring for campaign progress. Campaign progress records each level's best star result, persists across app restarts, only adds the best-star improvement delta to the player's spendable star reserves, and only mutates campaign progression on the `RUNNING -> PLAYER_WON` transition.
- `GameScreens.kt`: Menu-only UI for the home screen, asset-backed level selection, and the upgrade screen. These screens surface ready-to-spend stars separately from lifetime earned stars, each level card shows the best earned star row, and the upgrade screen remains the only place that spends meta-progression currency.
- `GameInGameUi.kt`: In-match HUD, pause overlay, level-end overlay, the selected-base upgrade affordance, and small in-game UI formatting helpers. The top HUD now keeps the player economy readout as a compact floating gold `$` amount at the left edge plus an icon-based pause action, while reward text comes from Android plural resources. Node upgrades trigger a short gold ring-fill animation around the upgraded base; the trigger logic is derived from the selected player base's `capLevel` transition instead of being stored as a separate gameplay event, and the animation runner always clears its overlay state in `finally` so selection changes cannot leave a frozen ring behind.
- `GameCanvas.kt`: Custom canvas rendering for the starfield, obstacles, bases, fleet trails, and fleets. Base labels show current unit count in the center and render the base level in a small circular badge that overlaps the node's bottom edge using a relative badge offset so the geometry stays readable without competing with the garrison count; when a base is near the bottom of the viewport, the badge clamps upward enough to remain visible. Selection feedback stays visual through the aura and stroke treatment, without a separate text tag above the node.
- `GameUiKit.kt`: Shared visual tokens and reusable Compose primitives such as the menu background, divider, action buttons, and stat rows used by both menu screens and overlays.
- `GameLogic.kt`: Input handling, fleet launching, base upgrades, frame stepping, AI decisions, ship production, combat resolution, arrival handling, capture-level downgrade handling, and selection cleanup when ownership changes. `stepMatch` treats pause as a hard no-op so elapsed time and economy cannot advance if a caller accidentally invokes it while paused, clears short-lived HUD hints after they expire, and empty-space taps now clear the current player selection without overwriting the active HUD message. Tap-driven launches and reinforcements keep ship-count messaging instead of the old redundant batch-status banner, multi-base selections collapse into one truthful combined launch-total message, and the batch helper now owns that final player-facing message so per-base sends do not overwrite it mid-loop.
- `GameWorld.kt`: Match creation from authored level definitions, route building around obstacles, coordinate conversion, and shared world geometry helpers. New matches start with an empty HUD banner so authored helper text does not cover the playfield.
- `GameModel.kt`: Immutable state models and enums for campaign progress, match state, world bounds, bases, fleets, obstacles, ownership, and base types. Campaign state stores per-level star records, derives lifetime earned stars from those records, tracks spent stars separately for the upgrade economy, rejects invalid star awards at the write path, and match state tracks elapsed time plus the stars and reserve gain earned on the current result.

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
- Base capacity is tiered by `capLevel`, with `cap = capLevel * 10`; each upgrade raises both by one tier until `maxLevel`, after which the selected-base upgrade pill is hidden. Upgrade prices use round tens based on the current level: `20` for level 1 to 2, `30` for level 2 to 3, `40` for level 3 to 4, and so on.
- The in-match upgrade affordance can still animate the most recent level-up even when that upgrade reaches `maxLevel`, because the animation follows the selected player base before the button visibility check removes the pill.
- Funds accrue for each non-neutral owner at a slower passive rate of `0.6 + 0.25 * owned bases` per second before campaign cash-rate multipliers apply, which keeps early cap upgrades from snowballing too quickly.
- Level completion awards between 1 and 3 stars based on authored completion-time thresholds stored in the level JSON; replaying a level improves the stored record only when the new run earns more stars, and only the improvement delta is added to spendable star reserves.
- Capturing a base reduces its `capLevel` by two tiers, but never below 1; surviving attackers keep their full post-combat count even if that temporarily exceeds the new captured cap.
- Friendly reinforcements also keep their full arrival count even if that temporarily pushes the destination base above its current cap; normal production then decays any over-cap garrison back toward cap over time.
- Tapping player-owned bases only changes selection state, empty-space taps clear the current selection, and tapping enemy or neutral targets with nothing selected shows only a brief onboarding hint instead of a persistent help banner; the message banner is reserved for actionable feedback such as launch, upgrade, pause, win, loss, and error states. Short-lived hints now remain visible until their expiry instead of being cleared early by a corrective base-selection tap.
- The in-game HUD does not expose per-AI money totals, level title text, or a remaining-rivals chip; it keeps only a compact floating player-funds readout and the pause action on screen so the playfield stays less cluttered.
- Each configured AI controller evaluates nearby non-owned bases every five seconds and attacks when it has a large enough unit advantage; otherwise it buys cap upgrades until a base is both full and at `maxLevel`, at which point that base pressures the weakest nearby non-owned target only when it has at least a modest numerical edge instead of idling or suiciding into a stronger defense.
- Route generation inserts detour waypoints when a direct line intersects an obstacle buffer.

## Extension Points

- Add new authored entity fields in `levels/` before wiring them into runtime behavior.
- Add new base or fleet modifiers in `GameModel.kt` and apply them in `GameLogic.kt` plus `GameCanvas.kt`.
- Keep UI-only changes in `GameScreens.kt` or `GameCanvas.kt`; keep deterministic gameplay rules in `GameLogic.kt` so they stay unit-testable.
- When adding a new AI owner, AI type, node type, or authored level field, update the external editor in the same change so runtime and authoring stay on one schema.
