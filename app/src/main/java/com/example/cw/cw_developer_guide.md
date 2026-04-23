# CW App Package Guide

## Purpose

`com.example.cw` contains the Android entry point for the app package. Gameplay systems live in the child `game` package so the root package stays small and easy to scan.

## Files

- `MainActivity.kt`: Single activity that enables edge-to-edge drawing, applies `CWTheme`, and renders `GameApp`.

## Subfolders

- `game/`: Gameplay UI, simulation, rendering, asset-backed level loading, and world helpers. See `game/game_developer_guide.md`.
- `ui/theme/`: Compose theme tokens used by the activity and app shell.

## Working In This Package

- Keep this folder limited to application bootstrapping and cross-feature wiring.
- Add new game systems under `game/` instead of growing `MainActivity.kt`.
