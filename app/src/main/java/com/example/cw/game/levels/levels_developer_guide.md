# Level Systems Guide

## Purpose

`com.example.cw.game.levels` contains the shared level-definition pipeline used by both packaged Android content and the external level editor.

## Files

- `LevelModels.kt`: Runtime data classes for authored levels, summaries, world bounds, and authored base or obstacle entries.
- `LevelJson.kt`: Small JSON parser plus mapping from JSON documents into validated level models.
- `LevelRepository.kt`: Level repository interfaces, asset-backed loading, summary sorting, and campaign unlock helpers.

## Data Model

The shared level format is versioned JSON. Each level defines:

- metadata used by the level list: `levelId`, `name`, `description`, `sortOrder`, and optional `unlockAfterLevelId`
- world bounds: `worldWidth`, `worldHeight`
- runtime intro text: `introMessage`
- `bases[]`: id, position, owner, type, starting units, `capLevel`, and `maxLevel`
- `obstacles[]`: position and radius

Owner names, AI controller types, and base type names map directly to the existing runtime enums so the Android app and editor use one vocabulary.
`capLevel` sets the base's starting capacity tier (cap = capLevel × 10) and its initial visual size (radius = 36 + (capLevel − 1) × 6).
`maxLevel` caps how high the base can be upgraded in-game; `capLevel` must not exceed `maxLevel`.
Both `cap` and `radius` are derived at runtime and must not be authored in the JSON.
Schema v1 files (which contain `cap` and `radius`) are accepted and auto-upgraded to v2 semantics on load.

## Working In This Package

- Keep authored content validation here so the asset loader, tests, and editor-facing rules stay aligned.
- Add new authored entities here before wiring them into gameplay or editor UI.
- When this shared schema changes, update both the Android runtime and the level editor together.
