# Incremental migration plan

The port uses a strangler approach and keeps a runnable application after each substantial phase.

| Phase | Status | Exit evidence / remaining work |
|---|---|---|
| 0. Toolchain | Complete | Android CLI discovery, dynamic JBR wrappers, doctor, Gradle 9.6 validation |
| 1. Audit | Complete | pre-change assemble/test/lint baseline and line-referenced hotspot audit |
| 2. Pure engine | Complete | `core-model`, `core-game`, deterministic reducer and ViewModel adapter tests |
| 3. Persistence | Complete for first schema | Room/DataStore repositories, migrations, transactional completion/reset, tests; exact piece resume remains |
| 4. Assets | Complete for format 2 | deterministic complementary tabs, JVM triangulation, shared schema, representative package, thumbnail, geometry tests |
| 5. Renderer | Complete for representative puzzle | libGDX/KTX surface, shared mesh/texture, explicit commands, renderer acknowledgement |
| 6. Effects/lifecycle | Complete for first profile set | fixed-step timing, clamped resume, pooled sparkles, reveal/shake/completion effects, quality/reduced motion, disposal |
| 7. Compose shell | Complete for current screens | lifecycle collection, cached background, adaptive keyed gallery, thumbnail, settings persistence/confirmation and emulator screenshot QA |
| 8. Performance | Emulator diagnostics complete | minified profileable target, passing diagnostic journeys, generated profiles/traces, Perfetto audit; physical measurements remain |
| 9. Cleanup | In progress | legacy Compose renderer removed after device parity; final aggregate verification/docs remain |

## Next implementation actions

1. Run the same Macrobenchmark journeys on an API 34+ physical device and compare the raw traces with the explicitly non-authoritative emulator diagnostics.
2. Capture gameplay-specific Perfetto traces for rapid answers and completion on physical hardware.
3. Add puzzle-specific artwork/packages for the remaining gallery entries.
4. Persist exact revealed piece identities for resumable mid-session play.
5. Integrate generated baseline rules only when a stable plugin supports this AGP 9 model and can rewrite R8 mappings.

The repository already used AGP 9.2.1, Gradle 9.6.0, and Kotlin 2.2.10. No unrelated build-stack migration is part of this plan.
