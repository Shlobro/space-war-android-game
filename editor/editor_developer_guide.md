# Editor Guide

## Purpose

`editor` contains the external level editor used to author the JSON files stored in `app/src/main/assets/levels`.

## Top-Level Files

- `package.json`: Frontend package manifest and editor scripts.
- `tsconfig.json`: TypeScript compiler configuration for the browser app.
- `vite.config.ts`: Vite configuration plus the local dev-server API that reads and writes `app/src/main/assets/levels`.
- `index.html`: Browser entry document for the editor UI.
- `../run_level_editor.bat`: Windows launcher that opens this folder, installs dependencies when needed, and starts the Vite dev server.

## Subfolders

- `src/`: React source for the editor interface, validation, and shared browser-side level types. See `src/src_developer_guide.md`.

## Working In This Folder

- Keep this tool focused on developer authoring workflows; the Android app remains the runtime consumer.
- The editor reads and writes the packaged levels in `app/src/main/assets/levels` directly through the local Vite dev server.
- Use `run_level_editor.bat` from the repository root for the standard Windows startup flow instead of retyping the npm command.
- The editor canvas is presented inside a phone-shell frame so the visible placement boundary matches the intended mobile viewport.
- The center canvas scales to the available viewport so the phone frame, placement controls, and status bar remain visible without center-column scrolling on typical desktop screens.
- Keep the editor schema synchronized with the Android runtime. New AI owners, AI controller types, node types, or other authored level fields must be added to both sides together.
