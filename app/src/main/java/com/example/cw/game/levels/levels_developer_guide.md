# Level Systems Guide

## Purpose

`com.example.cw.game.levels` contains the shared level-definition pipeline used by both packaged Android content and the external level editor.

## Files

- `LevelModels.kt`: Runtime data classes for authored levels, summaries, star thresholds, world bounds, and authored base or obstacle entries.
- `LevelJson.kt`: Small JSON parser plus mapping from JSON documents into validated level models.
- `LevelRepository.kt`: Level repository interfaces, asset-backed loading, summary sorting, and campaign unlock helpers.

## Data Model

The shared level format is versioned JSON. Each level defines:

- metadata used by the level list: `levelId`, `name`, `description`, `sortOrder`, optional `unlockAfterLevelId`, and the `twoStarTimeSeconds` plus `threeStarTimeSeconds` thresholds used for campaign scoring
- world bounds: `worldWidth`, `worldHeight`
- authored intro text metadata: `introMessage`. The runtime keeps missions free of an opening HUD banner, but the field remains part of the shared level document.
- `bases[]`: id, position, owner, type, starting units, `capLevel`, and `maxLevel`
- `obstacles[]`: position and radius

Owner names, AI controller types, and base type names map directly to the existing runtime enums so the Android app and editor use one vocabulary.
`capLevel` sets the base's starting capacity tier (cap = capLevel × 10) and its initial visual size (radius = 36 + (capLevel − 1) × 6).
`maxLevel` caps how high the base can be upgraded in-game; `capLevel` must not exceed `maxLevel`.
Both `cap` and `radius` are derived at runtime and must not be authored in the JSON.
Schema v1 files are still accepted as a migration bridge. The runtime ignores authored `cap` and `radius`, derives both values from `capLevel`, and defaults missing `maxLevel` to `DEFAULT_MAX_LEVEL`.
Missing star-threshold fields default to `DEFAULT_TWO_STAR_TIME_SECONDS` and `DEFAULT_THREE_STAR_TIME_SECONDS` as a migration bridge, but authored schema v2 files should provide both values explicitly.
`threeStarTimeSeconds` must be positive and lower than `twoStarTimeSeconds`.
Both star thresholds must stay at or below `MAX_STAR_TIME_SECONDS` so authored targets remain sane in both the editor and runtime validation.

## Working In This Package

- Keep authored content validation here so the asset loader, tests, and editor-facing rules stay aligned.
- Add new authored entities here before wiring them into gameplay or editor UI.
- When this shared schema changes, update both the Android runtime and the level editor together.
- The runtime and editor both derive `cap` and `radius` from `capLevel`; keep the formulas and constants synchronized across Kotlin and TypeScript.
