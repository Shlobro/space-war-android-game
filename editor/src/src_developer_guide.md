# Editor Source Guide

## Purpose

`editor/src` contains the browser-based level editor UI and its local validation helpers.

## Files

- `main.tsx`: React bootstrap for the editor app.
- `App.tsx`: Main editor shell, file workflow, responsive phone-framed playfield, and a right sidebar that swaps between selected-node properties, placement defaults, and level metadata.
- `editor.css`: Layout and styling for the editor UI, including the device frame around the playable screen.
- `level-api.ts`: Browser-side API client for listing, opening, saving, and deleting packaged level JSON files through the local dev server.
- `level-types.ts`: Browser-side types mirroring the packaged level JSON format.
- `level-validation.ts`: Shared validation and serialization helpers used before save.

## Working In This Folder

- Keep browser types aligned with the Kotlin level schema so saved files stay loadable by the Android app.
- Put save-blocking validation in `level-validation.ts` rather than scattering checks across UI handlers.
- Keep placement-time defaults in `App.tsx` aligned with the right-sidebar controls so users can choose what gets created before they click the map without adding extra top-row UI.
- Keep selected-node actions in the right sidebar, including destructive actions like deleting the currently selected base or obstacle.
- Route file I/O through `level-api.ts` so the editor stays tied to the packaged levels folder and can reopen existing levels without browser file-picker state.
- Keep capacity fields synchronized so `capLevel` defines `cap` directly, using 10 units per level.
- Treat the numeric unit labels as visual overlays only; pointer handling should stay on the draggable node shapes underneath.
- Keep owner choices, AI controller types, and node/base types aligned with the Android runtime schema. When adding a new AI owner, AI type, node type, or authored field, update both the editor and the Android app in the same change.
