# Design Bible Gap Audit

This audit distinguishes **source implemented**, **build verified**, and **runtime verified**. The owner later approved a Forge build workspace; a clean compile/package now succeeds, while Minecraft runtime verification remains pending.

## Implemented source foundations

- Offline/single-player logical-server authority with client-only rendering, UI, animation, and HUD projection.
- Four role entities, fixed role IDs, roster lifecycle, active-duo limit of two, UUID duplicate prevention, dismissal/resting handling, and safe lifecycle transactions.
- Follow, hold, guard, regroup, retreat, return home, base activity, safe recall, bounded navigation, formation reservations, and Stuck Recovery without block edits, door use, chunk loading, or automatic transport.
- Versioned Team SavedData, companion NBT, bounded action ledger/reservations, migration hooks, migration backup advisory, pre-recap recovery, Safe Mode, and fault circuit breaker.
- Player emergency controls: all-role recall, task reset, Stop All Actions through persisted Safe Mode, plan clear, repair save, diagnostics export, and conservative pre-uninstall dismissal support.
- Event queue, single active plan invariant, Retreat/Defend/Structure Entry plan contracts, player approval, visible plan targets, decision leads, reason codes, and Guardian after-action review state.
- Explainable threat dimensions: immediate damage, crowd, terrain, escape, resource, unknown, score, and readable reason codes.
- Story Downed, rescue priority/reservation, role-readable rescue posture, recovery fallback, and no off-screen solo activity.
- Bounded memory, owner-only Story Board Journal projection, anchors, temporal hooks, optional intentions, first-safe-return/base-plan milestones, mystery clues, optional promises, player identity, non-punitive consequences, local perception, and quiet micro-scenes.
- Data-driven dialogue, personality/tactical profiles, threat/structure profiles, encounter dialogue, compatibility registry, item classifications, and support-pack reload boundaries.
- Conservative resource ownership, six-slot companion inventory, default-off local WORLD_FREE pickup, disabled-by-default explicit Team Supply binding/whitelist/cap/transaction foundation, and privacy-safe status projection without container contents or coordinates.
- Will evidence confidence/stress rules, loaded-terrain Max scout rules, Gifted readiness/policy-aware non-auto suggestions, Push protected-bystander validation, owner-only Shield range boundary, and Guardian review state.
- Broad natural-hostile Will-control counterplay with a heart budget, boss-resistance contracts, collision-safe bounded Seer lift/shatter source behavior, rare explicit Hive Surge source modes, Mind Anchor support boundaries, original texture/model/animation resources, shared humanoid v2 companion rig, expanded clip assets, server-derived animation mapper, marker/VFX/sound safety contracts, and registered original Hive/companion/UI/ambience cue library.
- Survival/Creative/Peaceful/Spectator policy, server-derived Mob Griefing/Keep Inventory/Daylight Cycle/Hardcore projection, no-world-edit invariant, LIGHT/STANDARD/CINEMATIC performance policy, opt-in developer timing/path/chat overlay, local diagnostic export, and fixed runtime test contracts.
- Optional persisted onboarding, Guide tab, critical-alert-only dialogue profile, and fixed behavior acceptance catalog without a forced tutorial, input lock, or fake test pass.
- English-only source, runtime data, UI, localization, commands, dialogue, documentation, and generated resources.

## Partial by deliberate scope

- Team Supply, optional world-free pickup, Hive Surge, Mind Anchor advanced actions, compatibility registry, boss adapters, mystery/promise, personal arcs, role readiness, and developer overlay are **source foundations**, not proven runtime-balanced systems.
- RESTING snapshots preserve bounded personal state only at a validated safe base in the original dimension. They are not remote storage, a portable chest, cross-dimension inventory transport, or off-screen simulation.
- Guardian review is a compact factual card, not a full strategic replay, cinematic, or relationship simulator.
- Gifted suggestions are local non-spam source behavior, not a claim of final pacing, voice performance, or combat balance.
- Asset mappings and cues are static/source validated, not FPS, camera-obstruction, animation-blend, or audio-mix benchmarked.

## Deferred by design

- Automatic boats, horses, minecarts, portals, Nether/End transfer, and any automatic cross-dimension transport.
- Natural Hive worldgen, broad boss content/phase ecosystem, village raid integration, advancement integration, and arbitrary external-mod compatibility packs.
- Multiplayer, dedicated-server support work, multiple owners, external AI control, internet/API-key requirements, or free-form ChatGPT-like offline conversation.
- Autonomous block breaking/building, TNT, lava, fire, Redstone, farms, remote/private chest access, automatic loot routes, permanent controlled pets, and camera/input lock.
- Full long-form Story Graph, full relationship simulation, controller radial navigation, full voice acting, full audio mix, and public-release licensing/package workflow beyond the current separation contracts.

## Runtime gate

A successful clean Forge build does not replace a Java 17 + Forge 47.4.22 + GeckoLib 4.7.1.1 runtime campaign. Before a stable claim, test real Save/Load/migration worlds, pause/exit edges, navigation, plans, role powers, protected bystanders, duo switching, chaos scenarios, visual/audio behavior, and TPS/MSPT profiling.

See:

- `docs/RELIABILITY_AND_RECOVERY.md`
- `docs/OPERATIONAL_SAFETY_AND_OBSERVABILITY.md`
- `docs/ROLE_READINESS_AND_AFTER_ACTION_CONTRACT.md`
- `docs/ANIMATION_DIRECTOR_SYSTEM.md`
- `docs/CHAOS_AND_ACCEPTANCE_TESTS.md`
- `docs/PLAYTEST_CAMPAIGN.md`
