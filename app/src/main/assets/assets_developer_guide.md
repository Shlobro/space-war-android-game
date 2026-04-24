# Assets Guide

## Purpose

`app/src/main/assets` contains packaged data files that the Android app reads at runtime.

## Subfolders

- `levels/`: Versioned level definitions shared with the external level editor. See `levels/levels_developer_guide.md`.

## Working In This Folder

- Keep authored game content in stable, text-based formats so it can be validated in tests and edited outside Android Studio.
- Treat asset filenames as part of the content pipeline; avoid ad hoc naming because the level editor lists these files directly.
- When adding a new AI owner, AI controller type, node type, or other authored field, update both the asset-consuming Android code and the level editor so this folder stays valid in both tools.
- Campaign-facing authored values such as per-level star thresholds belong in the level JSON files so the runtime menu, victory screen, and editor all share one source of truth.
