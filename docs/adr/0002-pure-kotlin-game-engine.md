# ADR 0002: Pure Kotlin game engine

Status: accepted, 2026-07-15.

## Context

Rules, randomness, score, progression, UI resources, and timing were coupled inside one Android ViewModel. Correctness was difficult to reproduce and visual completion could not be acknowledged independently.

## Decision

`core-model` and `core-game` are Kotlin/JVM modules. The engine is an immutable reducer with injected `RandomSource`, `QuestionGenerator`, and `ScoringPolicy`. It has no Android, Compose, libGDX, Room, DataStore, or resource dependency.

A correct answer moves state to `REVEALING_PIECE` with a pending `PieceId`. Only `RevealAnimationFinished` commits the piece and produces the next question or completion. `GameViewModel` maps between the current screen model and reducer state during the strangler migration.

## Consequences

- fixed seeds make tests reproducible;
- duplicate answers and duplicate renderer callbacks are ignored by phase/piece validation;
- renderer timing cannot silently complete logical progression;
- UI model cleanup and exact resumable reducer persistence remain follow-up work.
