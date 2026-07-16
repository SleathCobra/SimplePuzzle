# Adding a puzzle

1. Add source art to a stable repository path. Preserve the original artwork; do not overwrite another puzzle.
2. Create `puzzles/<puzzle-id>/puzzle.json` using the schema in [ASSET_PIPELINE.md](ASSET_PIPELINE.md).
3. Choose rows/columns whose product matches the intended progression count. Use a stable `edgeSeed`; changing it is a topology change.
4. Add a dedicated Gradle generation/sync task or extend the generator task to enumerate definitions. Do not add launch-time generation.
5. Run:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :asset-pipeline:test :asset-pipeline:generatePuzzleAssets :asset-pipeline:syncPuzzleThumbnails
   ```

6. Verify the generated manifest ID, format version, source hash, dimensions, piece count, bounds, and reveal order.
7. Add an immutable gallery model that points to the dedicated generated thumbnail and runtime asset root. Never use the active texture as a gallery image.
8. Add generator/manifest validation coverage for any new topology or metadata rule.
9. Build `:app:assembleDebug` and `:app:assembleBenchmark`.
10. On a device, open the gallery, enter/leave the puzzle repeatedly, complete the final piece, rotate or recreate the Activity, and inspect texture/resource disposal.

Expected current dimensions for Cosmic Journey are 768 x 768 for the active texture and 320 x 320 for the thumbnail. New puzzles may use another aspect ratio; coordinates remain normalized and the renderer letterboxes with a `FitViewport`.
