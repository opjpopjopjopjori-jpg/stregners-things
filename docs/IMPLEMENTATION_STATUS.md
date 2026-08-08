# Implementation Status

> “Implemented” means implemented in source. It does **not** mean Forge runtime-tested; the approved build workspace compiles cleanly, while real client/world evidence remains required.

| System | Source status | Notes |
|---|---|---|
| Forge build workspace and clean compile | Verified build | Java 17, Forge 47.4.22, GeckoLib Forge 4.7.1.1, clean compilation, resource processing, jar packaging, and reobfuscation succeeded; no game runtime claim |
| Four companion entities and lifecycle | Implemented | UUID roster, duplicate prevention, lifecycle transaction records |
| Formations and navigation recovery | Implemented source | Follow/Cave/Combat/Retreat, expiring slot reservations, bounded safe candidates, path-node checks, no-progress recovery, stale-task cancellation after load |
| Team event queue, plans, and encounter context | Implemented source | Retreat, Defend, Structure Entry, durable plan action ID, decision domains/leads, observed biome/structure/threat context, role-aware dialogue mappings, approval/cancel/failed-safe paths |
| Transactions and reservations | Implemented source | Bounded action ledger for lifecycle, inventory, plans, rescue, powers, duo switch, intentions |
| Save migration and recovery | Implemented source | Version markers, safe defaults, pre-recap recovery, target validation, migration backup advisory, readable Safe Mode fallback |
| Safe Mode | Implemented source | Keeps Follow/Recall/Journal; isolates plans, powers, combat initiative, Hive focus, and repeated-fault circuit breaker |
| Downed and rescue | Implemented source | Status bands, priority ordering, reservation, role-readable posture cues, safe timeout fallback |
| Base life and anchors | Implemented | Non-destructive role anchor behavior; no chest/bed/farm/redstone interaction |
| Duo Dynamics | Implemented source | Six server-validated two-role profiles, safe HOME/REST switching, bounded personal RESTING snapshots, no damage combos |
| Optional arcs, mystery board, promises, and Journal view | Implemented source foundation | Four bounded arcs, evidence-based mystery clues, one optional safe-base promise, and bounded owner-only Story Board projection; no timer, loot, coordinate reveal, or material reward |
| Role behavior and capabilities | Implemented source foundation | Data-driven personality/tactical profiles, Will evidence confidence/stress, Max scout prechecks/radius limits, Gifted readiness/non-auto suggestions, Push bystander validation, and Guardian after-action review |
| Consequence and player identity | Implemented source foundation | Non-punitive stable consequence records, opt-in safe nickname/play style, first-return/base-plan milestones |
| Perception, interrupt, and spatial etiquette | Implemented source foundation | Bounded local sight signals, P0/P1 action interruption, interaction-radius yielding, dynamic silence; no hidden-world knowledge |
| Micro-scenes | Implemented source foundation | Campfire/threshold/return/landmark/silence hooks without camera or input control |
| Daily ambient | Implemented source | One rotating dawn opportunity at safe base, subject to chat budget/feature flag |
| English-only runtime | Implemented | One packaged locale: `en_us`; source/resource/docs audit excludes Arabic text |
| Dialogue and social interaction system | Implemented source foundation | 534 original English lines, priority/cooldown, stable IDs, expanded player conversation topics, paired social lead/reply director, visible local world cues, and no external AI/runtime generation |
| Powers | Implemented source foundation | Sense/Disrupt/Will hostile control and Will Surge with a control-heart budget, Guardian Brace, Push/Shield/Rescue, Scout Route/Signal/Grounding plus disabled-by-default advanced Mind Anchor actions with server safety checks; runtime balance and animation validation pending |
| Personal inventory and ownership | Implemented source foundation | Six slots, transaction path, ownership classes, conservative WORLD_FREE pickup code disabled by default, original-dimension safe-base RESTING snapshot; no private chest access |
| Team Supply | Implemented source foundation, disabled by default | Explicit safe-base player-bound container, role whitelist/cap, reservation, transaction, bounded privacy-safe status projection, and journal record; no automatic container discovery or container-content UI |
| Personal policy editor | Implemented source | Per-owner Seer/Gifted/Scout policy overrides in Journal/commands; config remains fallback and Safety Gates remain final |
| Data-pack extension | Implemented source | Dialogue, compatibility metadata, intentions, safe item classifications, advisory threat/structure profiles with tells/counterplay, tags |
| Playtest telemetry and behavior catalog | Implemented source foundation | Local-only Test A–F ratings, Go-rule summary, diagnostics export, fixed behavior briefing catalog, and Guardian 9/10 controlled acceptance target; no runtime ratings/pass claims |
| Mode and performance policy | Implemented source foundation | Survival/Creative/Peaceful/Spectator contract plus LIGHT/STANDARD/CINEMATIC optional-work cadence; runtime profiling still pending |
| Vanilla rule policy and optional guidance | Implemented source foundation | Server-derived Vanilla rule snapshot, no-world-edit invariant, combat-plan pause in Creative/Spectator/Peaceful, bounded tutorial state, Guide tab, and critical-only dialogue profile; runtime gamerule/UI validation pending |
| Operational emergency controls | Implemented source | Stop All Actions persisted Safe Mode, all-role recall, per-role task reset, plan clear, repair, and conservative pre-uninstall dismissal; runtime edge testing pending |
| Developer observability | Implemented source foundation | Opt-in in-memory AI/navigation/director/dialogue timing, owner-only task/path/chat snapshot, F8 overlay, local export, and dev command; not a profiler claim |
| Visual/HUD/animation/audio contract | Implemented source foundation | Shared humanoid v2 rig, original late-arc private texture refresh, expanded role social/world clip assets, server-derived mapper/controller, original registered companion/UI/ambience cue library, safe marker/VFX metadata, compact HUD, Social tab, and English Journal projection; visual/FPS/audio runtime validation pending |
| Natural-world hostile interaction | Implemented source foundation | No custom hostile entities or worldgen. Threat assessment, Will control, Guardian Brace, Scout Signal, Gifted powers, formations, and safety gates react to visible loaded vanilla or compatible mod hostile mobs only. |
| Compatibility packs, boss adapters | Partial foundation | Registry/contract exists; no broad external-mod pack is shipped or runtime tested |
| Cross-dimension travel/transport | Deferred | Requires separate safety tests |
| Full Story Graph / mystery board / long arcs | Deferred | Bounded optional arc/intention foundation only |
| Full off-screen simulation / storage | Deferred | RESTING snapshots are bounded and safe-base only; companions do not adventure, farm, chunk-load, or use player storage while inactive |

## Runtime completion gate

The approved Java 17 + Forge 47.4.22 + GeckoLib 4.7.1.1 workspace now compiles and packages successfully. The project still cannot be called stable until that workspace proves Save/Load, formations, plans, dialogue, manual inventory, natural hostile interactions, Safe Mode, migration, duo switching, chaos scenarios, diagnostics, animation behavior, and performance in a real world without crash, duplicate entity, corrupt state, loss of player control, or sustained TPS regression.

See `docs/BUILD_VALIDATION_REPORT.md`, `docs/RELIABILITY_AND_RECOVERY.md`, and `docs/CHAOS_AND_ACCEPTANCE_TESTS.md` for the verified build boundary and pending runtime evidence.
