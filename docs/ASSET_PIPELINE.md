# Puzzle asset pipeline

The `asset-pipeline` module is a Kotlin/JVM application. It has no Android runtime dependency and uses Java ImageIO plus kotlinx.serialization.

## Input

Each puzzle has a JSON definition under `puzzles/<puzzle-id>/puzzle.json`:

```json
{
  "id": "cosmic-journey",
  "title": "Cosmic Journey",
  "category": "Space",
  "sourceImage": "../../app/src/main/res/drawable/puzzle.png",
  "rows": 5,
  "columns": 6,
  "activeTextureMaxSize": 768,
  "thumbnailSize": 320,
  "edgeSeed": 4821
}
```

Puzzle IDs are lowercase kebab-case. Rows/columns must be positive, total pieces cannot exceed 512, active textures are limited to 256–4096 px, and thumbnails to 64–1024 px. Missing/invalid images fail the task with an actionable message.

## Output

The representative task writes `app/src/main/assets/puzzles/cosmic-journey/`:

- `texture.png`: active GPU texture, scaled to fit the configured maximum;
- `thumbnail.png`: low-resolution gallery image;
- `manifest.json`: format version, source SHA-256, dimensions, display metadata, grid, seed, and per-piece geometry.

`syncPuzzleThumbnails` also copies the generated thumbnail to `app/src/main/res/drawable-nodpi/cosmic_journey_thumbnail.png` for Compose's resource loader. The app build depends on both tasks.

Per-piece metadata contains normalized vertices, UVs, triangle indices, final position, width/height, bounds, and reveal order. Runtime validates `formatVersion` and consumes this data without cropping, slicing, thumbnail generation, or triangulation.

Format 2 applies `edgeSeed` through a stable integer mixer. Each internal grid boundary is generated once conceptually and reused in reverse by its neighbor, so tabs and sockets are complementary with no runtime fitting. Curved boundaries are sampled deterministically, concave piece polygons are ear-clipped on the JVM, and the emitted UVs use the same normalized board coordinates as the vertices. Reveal ranks are also a deterministic permutation of the piece indices.

## Determinism and incremental behavior

The Gradle task declares the definition and source image as inputs and the generated package as output. Identical source bytes and definitions produce identical PNG and JSON bytes in tests. The manifest hashes the original source bytes with SHA-256, so changing source art invalidates its identity even when dimensions do not change.

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :asset-pipeline:test :asset-pipeline:generatePuzzleAssets :asset-pipeline:syncPuzzleThumbnails
```

Generated files are committed so ordinary runtime startup never requires generator tooling. Review generated diffs with the definition/source change.

## Format compatibility

Format 2 is the current runtime format. Changing the topology algorithm, sample layout, or serialized meaning requires another format-version increment and regenerated committed packages. The generator tests verify byte-for-byte reproducibility, valid triangle indices, polygon/triangle area equality, complete reveal ranks, shared adjacent-edge points, and seed-dependent topology.
