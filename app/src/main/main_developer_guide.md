# Main Source Set Guide

## Purpose

`app/src/main` contains the Android runtime source set for the game. It combines Kotlin source, packaged assets, Android resources, and the manifest used to build the application.

## Files

- `AndroidManifest.xml`: Declares the application package, launcher activity, and Android runtime metadata.

## Subfolders

- `java/`: Kotlin source for the application entry point, gameplay systems, and level-loading code. See `java/java_developer_guide.md`.
- `assets/`: Packaged runtime data that ships with the app, including authored level JSON files. See `assets/assets_developer_guide.md`.
- `res/`: Android resources such as strings, colors, themes, and launcher assets. See `res/res_developer_guide.md`.

## Working In This Folder

- Put gameplay content that should ship with the app in `assets/` rather than hardcoding it in Kotlin.
- Keep detailed package behavior documented in the closest child guide and link to it from here instead of repeating it.
- Keep authored campaign metadata such as star thresholds in the shared level JSON so the runtime and editor stay aligned, and keep player progression persistence inside the game package rather than scattering it through UI files.
