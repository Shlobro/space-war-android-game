# Packaged Levels Guide

## Purpose

`app/src/main/assets/levels` contains the packaged campaign levels consumed by the Android runtime and edited by the external level editor.

## File Format

Each file is a JSON level document with:

- top-level metadata such as `schemaVersion`, `levelId`, `name`, `description`, `sortOrder`, `unlockAfterLevelId`, `twoStarTimeSeconds`, `threeStarTimeSeconds`, and `introMessage`
- `worldWidth` and `worldHeight` for the playable map bounds
- `bases[]` entries for structures placed on the map
- `obstacles[]` entries for circular blockers used by route generation

Within each base entry, `capLevel` sets the starting capacity tier (cap = capLevel × 10) and visual size (radius = 36 + (capLevel − 1) × 6). `maxLevel` caps in-game upgrades; `capLevel` must not exceed it. Do not author `cap` or `radius` — both are derived at runtime.
AI-owned bases are configured through level-level `aiControllers[]` entries, which assign an AI owner such as `AI_1` to an AI type such as `STANDARD`.
`twoStarTimeSeconds` and `threeStarTimeSeconds` define the completion-time targets for earning 2 or 3 stars; finishing slower still awards 1 star, and the 3-star target must be lower than the 2-star target.

The authoritative field definitions and validation live in `app/src/main/java/com/example/cw/game/levels/levels_developer_guide.md`.

## Files

- `level_1.json`: Opening command-base map with only regular structures.
- `level_2.json`: Follow-up map that introduces fast-launch bases.

## Working In This Folder

- Keep one level per file.
- Use stable integer IDs because campaign progress and selection logic key off level and base IDs.
- The editor dev server works against this folder directly, so opening or saving a level in the browser updates these packaged JSON files.
- When adding new AI owners, AI types, node types, or other authored fields here, update both the Android runtime and the editor before committing new level data.
