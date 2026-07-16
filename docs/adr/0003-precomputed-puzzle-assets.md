# ADR 0003: Precomputed puzzle assets

Status: accepted, 2026-07-15.

## Context

The prototype clipped one full bitmap repeatedly and generated jigsaw outlines during drawing. Runtime slicing or triangulation would move similar work to the main/render thread and make results device-dependent.

## Decision

A deterministic JVM generator creates the active texture, gallery thumbnail, versioned JSON manifest, source hash, and piece geometry before Android packaging. The schema lives in `core-model` so generator and renderer share serializers. Runtime rejects unsupported versions and performs no image slicing, thumbnail creation, or triangulation.

Format 2 uses a stable seed mixer to generate normalized complementary tab boundaries. The generator samples the curves and ear-clips every concave polygon before packaging; adjacent pieces reuse identical boundary points in reverse. UVs remain in global normalized texture coordinates.

## Consequences

- generated artifacts are reproducible, testable, and reviewable;
- gallery and gameplay use purpose-sized images;
- source changes are visible through the manifest hash;
- topology and triangulation changes are versioned generator changes rather than hot-loop code;
- emulator screenshots and area/complement tests verify the tabbed silhouettes before removal of the legacy Compose renderer.
