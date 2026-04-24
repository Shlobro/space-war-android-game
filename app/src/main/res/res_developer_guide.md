# Resource Guide

## Purpose

`app/src/main/res` contains the Android resource tree for the runtime app, including strings, launcher assets, drawable XML, themes, and backup configuration.

## Subfolders

- `values/`: Centralized text and scalar resources referenced by Kotlin and the manifest. `strings.xml` holds the app name and short HUD labels such as the in-match funds label.
- `drawable/`: XML drawable assets used by the Android app shell.
- `mipmap-anydpi/`, `mipmap-hdpi/`, `mipmap-mdpi/`, `mipmap-xhdpi/`, `mipmap-xxhdpi/`, `mipmap-xxxhdpi/`: Launcher icon variants generated for Android density buckets.
- `xml/`: Android XML configuration files such as backup or data-extraction rules.

## Working In This Folder

- Keep user-facing text in `values/` instead of hard-coding strings in Compose where practical.
- Prefer documenting detailed resource usage in the deepest relevant child folder and link from here instead of duplicating it.
- Treat launcher and backup resources as app-shell concerns; gameplay-specific behavior belongs in Kotlin packages under `java/`.
