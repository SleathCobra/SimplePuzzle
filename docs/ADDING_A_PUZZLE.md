# Adding a puzzle

1. Add source art to a stable repository path. Preserve the original artwork; do not overwrite another puzzle.
2. Create `puzzles/<puzzle-id>/puzzle.json` using the schema in [ASSET_PIPELINE.md](ASSET_PIPELINE.md).
3. Choose rows/columns whose product matches the intended progression count. Use a stable `edgeSeed`; changing it is a topology change.
4. Add the package to authoritative `puzzles/catalog.json` with its definition path, generated asset root, Android thumbnail resource name, availability, initial lock state, and any real legacy ID.
5. Add a dedicated Gradle generation/sync input or extend the tasks to enumerate definitions. Do not add launch-time generation. A catalog row alone does not generate assets.
6. Run:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :asset-pipeline:test :asset-pipeline:generatePuzzleAssets :asset-pipeline:syncPuzzleThumbnails :asset-pipeline:generatePuzzleCatalog
   ```

7. Verify the package manifest and generated catalog agree on ID, title/category, asset root, piece count, format, source hash, dimensions, bounds, and reveal order. Run generation a second time and require a byte-identical/no-diff result.
8. Map the catalog thumbnail resource name in `AndroidPuzzleCatalog.thumbnailResource`. Never use the active texture as a gallery/Compose image and never fabricate a playable entry.
9. Add generator/catalog validation coverage for any new topology or metadata rule.
10. Build `:app:assembleDebug` and `:app:assembleBenchmark`.
11. On a device, verify gallery → mode selection → gameplay routes the new asset root, title preview can select/switch it, invalid-package fallback remains safe, and repeated title/game renderer handoff disposes both surfaces.

Expected current dimensions for Cosmic Journey are 768 x 768 for the active texture and 320 x 320 for the thumbnail. New puzzles may use another aspect ratio; coordinates remain normalized and the renderer letterboxes with a `FitViewport`.
