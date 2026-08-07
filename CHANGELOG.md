# Changelog

## Multi-stage contextual scene director 0.9.0-dev

- Expanded the Contextual Interaction Director from one-shot definitions into 198 authored Discovery/Revisit scenes. The same animal, field-guide mob, biome, structure, work block, weather/time, or fatigue subject now changes its authored response only after the prior scene cooldown rather than repeating one line.
- Added player-used non-container work-block context, weather, dawn/dusk/night context, staged structure notes after player-selected visible assessment, and stage-aware group memory. No context sees hidden terrain, reads a container, grants a resource, controls an animal, or creates a forced mission.
- Added authored lead/reply scene choreography: a lead line triggers a contextual body action, then an eligible nearby second companion can provide one delayed paired reply with its own contextual action. Danger, plan, Safe Mode, recovery, low health, or invalid pair cancels the reply.
- Expanded default contextual dialogue to 1,584 lines and full default dialogue to 2,134 lines. Every line is English-only authored field-guide, route, safety, rest, or personality content rather than runtime-generated text.
- Retained finite context animation vocabulary rather than adding duplicate filler clips: nine body gestures per role combine with independent face/hair layers, staged data scenes, role-specific lead/reply text, and cooldown/memory selection to create many safe contextual combinations.
- Runtime trigger timing, pair choreography, animation blending, chat budget behavior, FPS/VRAM cost, and player acceptance remain pending real integrated-client evidence.

## Contextual interaction director 0.8.0-dev

- Added a reloadable, authored Contextual Interaction Director for visible local animals, safely observed distant mobs, current biome, player-selected structure context, and companion fatigue.
- Added 80 data definitions and 640 new original English context lines, bringing the default dialogue library to 1,190 entries. Definitions are role-aware through dialogue triggers, bounded priority/cooldown, and nine visual-only contextual action categories.
- Added original contextual animation clips for animal greeting/observation, field note, threat brief, loot note, biome brief, structure brief, rest request, and route note for every role, with face and secondary motion layers active beneath them.
- Added curated Minecraft safety/field-guide information about animals, hostile counterplay, possible drops and limits, biomes, structures, routes, and fatigue. Information never starts automation, grants loot, controls animals/pets/villagers, accesses storage, forces a quest, reveals hidden terrain, or uses external AI.
- Context is suppressed in danger, Safe Mode, active/pending plan, low player health, hazard state, combat, recovery, or ineligible companion state. Structure notes require an existing player-selected visible assessment; hostile briefs require a safely distant visible perception cue.
- Bumped configuration version to `16` and protocol to `33` for contextual visual-action vocabulary. Runtime trigger timing, dialogue quality, animation playback, performance, and player acceptance remain pending integrated-client evidence.

## Visible hostile response and anti-freeze overhaul 0.7.0-dev

- Added `ThreatResponseDirector`, a bounded local visible/direct-attack response for normal loaded hostile mobs. Combat-capable companions clear stale formation slots, receive nearby confirmed targets, enter `FIGHTING`, and no longer allow Follow/formation navigation to overwrite melee pursuit.
- Added explicit `FIGHTING` state handling: valid local targets keep melee navigation active; invalid/dead/out-of-pursuit targets cleanly clear back to Follow. Melee combat may intentionally orient the body toward a confirmed target, while ordinary path attention remains head-only.
- Added original English `hostile_contact` and `hostile_crowd` dialogue for all roles, with one bounded lead and one paired reply only after a real lead. New lines communicate line, route, spacing, pressure, and retreat options without hidden knowledge or forced player action.
- Three-plus visible threats may draft a Defend proposal for player approval. Critical player health, extreme crowd pressure, or a critically injured active companion can use the existing Retreat safety path; no automatic power cast, target scan, custom monster, or forced combat plan was added.
- Added zombie/visible-hostile behavior and chaos cases, runtime matrix, anti-freeze formation guards, and static source invariants. Actual hostile targeting, combat movement, dialogue timing, plan UI, performance, and player acceptance require real integrated-client evidence.

## Event-driven natural attention refinement 0.6.5-dev

- Removed repeated timed re-glances for a target that remains in the same lateral/rear sector. A look gesture now occurs only when an already-selected target is new or changes sector.
- Normal idle and alert face loops now keep head yaw neutral. Alert remains visible through eyes/brows only; it cannot become a periodic head scan.
- Kept the finite eye-lead/head-turn/hold/head-return/eye-recenter gesture path, but it now returns to forward and stays there unless another meaningful target event occurs.
- Added a regression invariant that alert face clips contain no left/right head yaw and every glance/rear clip remains finite/non-looping.

## Natural look gesture refinement 0.6.4-dev

- Replaced static/looping lateral head turns with finite authored attention gestures: eye lead, smooth head turn, short hold, head return, and eye recenter.
- Replaced static rear head turns with a finite over-shoulder inspection that returns to forward before cooldown. A companion cannot remain visually pinned to a side or rear target while walking.
- Gait state now keeps a gesture active only for its authored clip duration: 20 ticks for a lateral glance and 30 ticks for a rear check, followed by 70/120-tick cooldowns.
- Marked every glance/rear animation as non-looping and added a static regression guard against a future static-loop look pose.

## Natural gait and head-look overhaul 0.6.3-dev

- Removed the companion `LookAtPlayerGoal` and `RandomLookAroundGoal` full-body jitter path. A server-derived gait director now stabilizes body yaw toward actual travel velocity with a capped turn rate while the companion follows a valid path.
- Added synchronized presentation-only `CompanionLookIntent`: forward, finite head-only left/right glance, and bounded left/right rear check. A glance lasts 20 ticks with a 70-tick cooldown; a rear check lasts 30 ticks with a 120-tick cooldown; both return to forward instead of repeatedly spinning the body.
- Moved head orientation exclusively into dedicated face-layer clips. Walk/run body clips no longer animate the head, so lateral attention cannot fight locomotion or rotate the whole entity away from its path.
- Added per-role glance and rear-check face clips alongside the body/face/secondary controller split. The director reads only an existing current target; it does not scan terrain, acquire targets, alter navigation, control the camera, or change gameplay authority.
- Added a natural-gait runtime contract and static guard for canonical look intent, no old random/full-body look goal instantiation, and required glance/rear-check clips. Real integrated-client path heading, controller blending, face orientation, clipping, and acceptance remain pending evidence.

## Layered animation presentation rebuild 0.6.2-dev

- Replaced the single visual playback path with independent GeckoLib `companion_body`, `companion_face`, and `companion_secondary` controllers. Their bone ownership does not overlap: body actions no longer freeze blink, local gaze, brow expression, jaw-talk, hair, or clothing motion.
- Added six face loops and six secondary-motion loops per role: idle, alert, talk, combat, power, recovery, idle secondary, walk secondary, run secondary, combat secondary, power secondary, and recovery secondary.
- Rebuilt role locomotion into articulated upper/lower limbs, feet, forearms, torso counter-motion, and role-specific timing. Seer stays restrained, Guardian weighted, Gifted controlled, and Scout athletic.
- Rebuilt Seer Hive focus/release variants and Gifted focus/push/shield/rescue body presentation for the new face/hair layers. These animations remain visual-only; server validation resolves every power, damage, rescue, and cooldown outcome separately.
- Added static controller, face, hair, UV, animation-bone, and source-package invariants. Actual GeckoLib three-controller blending, face/hair visual quality, clipping, FPS/VRAM cost, and user acceptance still require real integrated-client evidence.

## Reliability and precision presentation hotfix 0.6.1-dev

- Added owner-scoped, non-persistent Safe Recall / first-spawn destination reservations plus destination living-entity AABB clearance. Companion recall bursts no longer intentionally select one centered square; a tight local area now safely rejects or holds a later request rather than stacking entities.
- Safe Recall clears a recalled companion's stale formation slot before Team Director assigns a fresh spacing-safe slot. Claims expire after 60 ticks, clear at logout, and release on failed spawn/dismiss/rest lifecycle paths; they expose no coordinates or client authority.
- Added a repeatable `safe_recall_distinct_destinations` behavior case, `safe_recall_destination_contention` chaos briefing, and open-terrain/tight-corridor runtime matrix.
- Strengthened faceted presentation clips: Seer Hive focus/release and Gifted focus/push/shield/rescue now carry authored face/hair precision layers; all roles retain differentiated walk/run/combat variants plus blink, local eye drift, brow, jaw-talk, and secondary hair motion.
- This is build/static-validated only. No claim is made yet for real GeckoLib playback, visual beauty, show-level resemblance, animation blending, recall placement, collision behavior, FPS/VRAM cost, or user acceptance until a Minecraft integrated-client test is captured.

## Visual presentation milestone 0.6.0-dev — Faceted Character and Expression Rebuild

- Rejected and replaced the entire prior companion visual direction: all eight generated character texture files and all four Geo models were regenerated from a clean faceted-character pipeline rather than recolored or patched.
- Added original tapered/octagonal head construction, separate nose/chin/ear planes, independent eye white/iris/pupil/catchlight components, upper/lower lids, brows, and jaw/mouth presentation bones. The model no longer presents a single square Steve-style head block.
- Rebuilt all four role silhouettes with 68–72 bones and 57–61 bounded cuboid layers: grounded hair masses, articulated body segments, ordinary late-1980s clothing, restrained personal items, and no fantasy hood, talisman, rune, glowing-eye, weapon, hiking-pack, bedroll, map, compass, or tactical-prop visual direction.
- Replaced every personal/public 512×512 texture atlas for the new face/material geometry. The technical atlas remains rectangular, but the visible model is built from faceted geometry rather than a flat square skin.
- Added authored local blink, eye drift, brow expression, jaw-talk, secondary hair sway, and role-specific idle/walk/run/combat motion layers. Seer remains restrained, Guardian weighted, Gifted controlled, and Scout athletic; these visual clips never alter gameplay authority.
- Added complete rebuild tooling plus geometry/UV/animation-bone regression checks. Runtime culling, close-up face presentation, blink alignment, hair clipping, animation blending, FPS/VRAM cost, and player acceptance remain unverified until real Minecraft testing.
- Reference research is restricted to broad grounded visual direction. The source does not copy actor likeness, exact screen costumes, source pixels, logos, show assets, or Yes Steve Model assets.

## Visual presentation milestone 0.5.0-dev — Modular 512px Companion Rebuild

- Replaced the previous flat 256px presentation with eight original 512×512 RGBA atlases: four private-profile role atlases and four matching public-profile fallbacks. All paths now share one 512px UV scale.
- Rebuilt all four Geo models into original modular layered cubic characters while preserving the existing `shared_humanoid_v2` animation backbone. The Seer, Guardian, Gifted, and Scout now carry 43–47 bones and 34–38 bounded cuboid layers for hair volume, outer layers, cuffs, boots, collars, straps, packs, field gear, and role-readable silhouette detail.
- Added the `modular_layered_companion_512_v1` visual contract, UV-bound validation, public-profile resolution validation, role-specific modular-bone floors, source model/atlas review boards, and a canonical generator that preserves authored animation files.
- Kept model additions visual-only: they do not change hitboxes, authority, abilities, damage, gameplay state, online requirements, or the two-active-companion limit.
- Retained the 0.4.1-dev integrated-world JSON reload hotfix. This visual rebuild is compile/static-validated only; final in-game texture rendering, culling, clipping, animation blending, GPU cost, and player acceptance require real Minecraft evidence.
- The presentation uses broad original late-1980s survival-game direction only. It does not copy actor likeness, exact screen costumes, show assets, or third-party model/texture files.

## Hotfix 0.4.1-dev — Integrated-World JSON Reload Initialization

- Fixed a direct integrated single-player world-creation crash: nine `SimpleJsonResourceReloadListener` singletons constructed before their static `Gson` field initialized, so Java passed `null` to the listener superclass. The first data reload then failed inside `GsonHelper` with a null-Gson exception.
- Reordered every affected listener so its `Gson` exists before singleton construction: dialogue, compatibility, intention, item classification, threat profile, structure override, mental effect, encounter dialogue, and behavior profile reloaders.
- Added a source validation invariant that fails if any `SimpleJsonResourceReloadListener` can again construct its singleton before its `Gson`.
- This is a targeted crash hotfix. A successful build and bytecode check do not yet constitute a completed in-game regression test; clean-profile world creation still requires user verification.

## Source milestone 3.6 — Texture Revision, Social Director, and Contextual Conversation

- Rebuilt all four private-profile 256×256 companion texture atlases with original late-arc field styling: layered practical clothing, weathering, readable faces, role-specific gear, and UV-safe accessory detail. No actor likeness, exact costume, logo, or copied show asset was used.
- Added thirteen social/world interaction clips to every companion animation file plus role-specific Seer, Guardian, Gifted, and Scout social clips. Social visuals are server-directed, interruptible, and suppressed by combat, retreat, downed, recovery, or other urgent states.
- Added a bounded two-companion Social Director that responds only to player-visible local campfire, weather, horizon, safe-base, player-selected work block, cave, village, travel, and calm cues. It never reads storage, scans hidden terrain, changes player input, alters gameplay, or uses external AI.
- Expanded authored English dialogue to 534 lines with paired lead/reply families for social world cues, role-specific speaking direction, normal chat budget sharing, and session-safe cancellation.
- Added player-led conversation topics for world reading, team check, last encounter, and check-in; replies derive only from visible local/team/bounded-memory facts.
- Added a Social Journal projection, social behavior/chaos acceptance cases, social feature flag, texture art-direction contract, and social dialogue/interaction contract.
- Bumped configuration version to `15`, custom protocol to `32`, and development artifact version to `0.4.0-dev`. Runtime texture, animation, dialogue pacing, UI, and performance validation remain pending.

## Source milestone 3.5 — Natural World Only and Custom-Hostile Retirement

- Retired every mod-added hostile entity, custom enemy texture/model/animation resource, hostile loot table, enemy renderer, manual custom-monster summon path, natural-spawn possibility, and Rift Operation wave system.
- The entity registry now contains companion entities only. The mod does not add or replace Minecraft monsters; normal gameplay uses visible loaded vanilla hostiles and compatible hostile mod mobs.
- Removed the operation packet fields, operation Journal tab, operation commands, operation behavior/chaos cases, operation feature/config settings, operation persistence, and operation-specific reward/drop/XP hooks.
- Added a forward migration retirement step that discards the old saved `RiftOperation` record rather than ever resuming a retired encounter.
- Kept and refined the natural-world companion systems: Will control-heart targeting, Guardian Brace, Scout Signal, Gifted safety gates, threat observation, formation, navigation, story evidence, and no-farm boundaries. Retired operation-only dialogue; the default original English library now contains 390 lines.
- Bumped team/blackboard/config values to `23` / `19` / `14`, custom protocol to `29`, and development artifact version to `0.3.0-dev`.

## Source milestone 3.4 — Rift Operations, Guardian Brace, and Scout Signal

- Added three optional player-started contained Rift Operations: `Recon Sweep`, `Breach Containment`, and `Anchor Disruption`. They use fixed bounded waves of original custom Hive entities only and never natural-spawn or edit the world.
- Expanded the original Hive roster from four to six threats with Lurker (fast ordinary melee flank pressure) and Sentry (visible short line-of-sight Slowness pulse), including original 128px textures, Geo models, eight-clip animation sets, renderer registration, tags, bounded attributes, loot tables, and operation-wave integration.
- Added an explicit operation state machine with safe loaded-space spawn sampling, briefing/intermission/wave/terminal phases, player boundary, full timeout, cooldown, Safe Mode, game-mode, team-availability, downed, dimension, target-boundary, player-death, cancellation, and Save/Load recovery routes.
- Marked operation entities with a persistent owner/session/expiry marker. Loaded marked entities are discarded on safe cancellation; unloaded marked entities expire when active again. Operation drops clear and operation XP is zero, preventing a loot/farm loop.
- Added coordinate-free owner HUD and Team Journal **Ops** projection, fixed C2S operation commands, direct English command surface, diagnostic export, onboarding, behavior/chaos cases, and a complete runtime contract without exposing spawn locations, entity UUIDs, inventory, or hidden-world information.
- Added Guardian Brace: an explicit 80-tick, close-range, energy/cooldown-bounded 20% player-damage mitigation stance. It is not invulnerability, taunt ownership, projectile deletion, loot control, or an automatic action.
- Added Scout Signal: a focus/cooldown-bounded, non-damaging temporary mark for up to three visible active hostile targets. It has no projectile, wall vision, protected-target, damage, loot, or world-edit path.
- Added original English dialogue coverage for operation briefing/wave/success/abort, Guardian Brace, and Scout Signal; total default dialogue is now 406 lines.
- Bumped team/blackboard/config values to `22` / `18` / `13` and custom protocol to `28` for operation status plus appended fixed ability/visual vocabulary. Runtime Minecraft behavior remains unverified.

## Source milestone 3.3 — Will Control-Heart Budget and Broad Hostile Eligibility

- Replaced the tag-only Seer control gate with a server-owned hostile `Mob` / `Enemy` eligibility boundary, allowing standard vanilla hostiles and supported hostile mod mobs while continuing to reject players, companions, villagers, animals, tameable entities, and explicit protected types.
- Added a deterministic nearest-first Will control-heart budget based primarily on target maximum health, with bounded attack/armor pressure, a hard Swarm target cap, load-scaled Energy/Strain costs, owner HUD load/count feedback, and a temporary Surge capacity bonus.
- Added an honest short `Stagger` fallback for an overloaded or partial-resistance target. Wither and Ender Dragon are explicitly partial-resistance targets; Stagger never disables their AI/gravity, lifts, redirects, kills, changes loot, or bypasses boss phases.
- Extended direct Seer Disrupt through the same hostile eligibility and heart-pressure model while retaining active-combat and sensitive-area gates.
- Bumped configuration version to `12` and custom protocol to `26` for display-only Will target-count and control-heart HUD packet fields.
- Added source contracts and static invariants for broad hostile eligibility, protected-target exclusions, heart-load selection, boss resistance, and protocol discipline. Runtime Minecraft behavior remains unverified.

## Source milestone 3.2 — Seer Lift, Structural Shatter, and Gifted Precision Presentation

- Added bounded collision-safe lift motion for the then-tagged Seer target boundary under Suspend/Swarm Freeze, with no teleport, no chunk loading, and restored gravity/AI on release.
- Added mode-specific Seer redirect/shatter visual actions, custom Hive suspend/shatter animations, and original non-graphic structural shatter cue/effect treatment.
- At this historical milestone, the control boundary was tag-only; later milestone 3.3 supersedes that boundary with broad hostile eligibility and a control-heart budget.
- Added detailed Seer/Gifted precision power contract and runtime acceptance matrix.

## Build milestone 3.1 — Original Companion Audio Library and Packaging Expansion

- Added 23 deterministic original synthesized companion, UI, and ambience OGG Vorbis cues with no actor voice, show music, copied dialogue, or third-party samples.
- Registered confirmed action/UI cues through `ModSounds` and `CompanionPresentationSoundService`, with a user-facing `companion_audio_enabled` safety setting and per-entity cooldowns.
- Added audio provenance, accessibility fallback, privacy, mix, and runtime QA contract documentation.
- Ambient loops are packaged for future safe-base mix testing but are not auto-looped by current source logic.

## Build milestone 3.0 — Approved Forge Workspace and Clean Compile

- Added the owner-approved ForgeGradle 6 workspace for Minecraft 1.20.1 / Forge 47.4.22, Java 17, Gradle 8.8, and GeckoLib Forge 4.7.1.1.
- Resolved real compile compatibility issues in GeckoLib `PlayState`, GUI background calls, entity NBT method visibility, particle option typing, biome registry access, attribute event naming, target goal compatibility, and missing imports.
- Completed a clean `./gradlew clean build` successfully, including Java compilation, resource processing, jar packaging, and Forge reobfuscation.
- Added a build validation report and updated source validators/docs from the historical no-builder phase to the approved build-workspace policy.
- Runtime launch, world behavior, Save/Load, GeckoLib visual verification, performance profiling, and acceptance tests remain unexecuted and are not claimed.

## Source milestone 2.5 — Scope Governance and Publication Readiness Policy

- Added explicit source/runtime/release completion levels, feature kill criteria, version discipline, non-negotiable scope boundaries, and PERSONAL/PUBLIC publication review policy.
- Documented that legal ownership/license selection remains owner-controlled and no public-release claim is made from source alone.

## Source milestone 2.4 — Repeatable Behavior Acceptance Catalog

- Added fixed behavior test cases for Guardian safety, Seer evidence discipline, Scout refusal, Gifted bystander safety, Story Journal privacy, and Team Supply privacy.
- Added `/riftcompanions_dev behavior <case_id>` local briefing command; it reports setup/expected/failure boundary but never fakes a pass.
- Documented the controlled Guardian acceptance target of 9 safe/explainable outcomes in 10 repeated written scenarios.
- Added regression discipline requiring a behavior case, chaos scenario, validation invariant, or intentional scope boundary for every runtime bug.

## Source milestone 2.3 — Team Supply Status Privacy Projection

- Added a bounded owner-only Team Supply status view with feature/bound/safe-base state and per-role daily cap metadata.
- Explicitly excluded bound coordinates, dimension, container type, slots, stacks, counts, NBT, and all container contents from the client projection.
- Added Team Journal Guide status text and English-only Team Supply UI/privacy/runtime contract.
- Bumped custom network protocol to `25` for the Team Supply status snapshot.

## Source milestone 2.2 — Bounded Story Journal and Evidence Presentation

- Added owner-only bounded chapter, promise, and recent visible-evidence clue projection to the Team Journal without coordinates, raw clue IDs, hidden structure data, or client authority.
- Revised the Journal tab into an optional Story Board view with chapter state, promise state, Mystery Board summaries, deferred clue clarity, and recent team memory linkage.
- Kept all story completion, promise changes, clue deferral, unlocks, and evidence validation server-owned through existing commands/services.
- Bumped custom network protocol to `24` for the bounded story Journal snapshot and added an English-only packet/privacy/runtime acceptance contract.

## Source milestone 2.1 — Shared Rig, Animation State Mapping, and Marker Safety

- Upgraded all four Geo JSON models to the shared `shared_humanoid_v2` canonical Blockbench rig while preserving legacy bridge bones for existing clips.
- Expanded each role animation resource with locomotion, combat, interaction, stuck, downed/recovery, emotion, calm, and role-specific power/ranged clip families.
- Added a central animation mapper/controller that derives GeckoLib playback from authoritative visual state and action; animation assets do not change gameplay.
- Added allowed marker metadata, server-authority notes, VFX origin/cap contracts, original-sound cue contracts, no-camera-lock rules, and animation QA/performance matrices.
- Added server-confirmed melee strike presentation and hit-react visual actions without making marker callbacks authoritative for damage.
- Bumped network protocol to `23` because the synchronized visual-action vocabulary expanded.
- Added detailed English-only animation director, rig, keyframe, transition, VFX, sound, and QA documentation plus a static review audit that suppresses low-priority talk/route actions during urgent combat or retreat visuals. Runtime Blockbench/GeckoLib/FPS validation remains pending.

## Source milestone 2.0 — Vanilla Rule Policy, Optional Guidance, and Accessibility

- Added server-derived Vanilla rule snapshots for difficulty, Mob Griefing, Keep Inventory, Daylight Cycle, Hardcore, Creative, and Spectator without inventing non-Vanilla mechanics.
- Added a strict no-companion-world-edit policy service and paused Defend planning when Creative, Spectator, or Peaceful rules make survival combat pressure inappropriate.
- Added bounded, persisted optional onboarding for first companion, plan proposal, Stuck Recovery, Safe Mode, and relevant Vanilla rule policy; it never blocks gameplay or controls input.
- Added English-only Team Journal Guide tab, tutorial status/dismiss/resume commands, and current world-rule guidance projection.
- Added `ChatProfile.CRITICAL_ONLY` for P0-alert-only dialogue while preserving HUD, Journal, command, and Safe Mode information.
- Bumped team/blackboard/config versions to `21`/`17`/`10` and network protocol to `22` for the guidance snapshot.
- Added English-only Vanilla-rule/onboarding/accessibility contract and runtime acceptance matrix; no in-game validation is claimed.

## Source milestone 1.9 — Operational Safety, Role Readiness, and Developer Observability

- Added explicit `Stop All Actions`, all-role recall, per-role task reset, and conservative pre-uninstall dismissal command paths; Stop All uses persisted Safe Mode rather than an invisible AI override.
- Corrected live roster addressing so a DOWNED companion remains reachable for player rescue, diagnostics, and duplicate prevention without permitting a replacement spawn.
- Added pause/exit/recovery, authority, baseline-performance, debug-overlay, and bug-report contracts without claiming runtime execution.
- Added Gifted readiness bands, bounded policy-aware non-auto suggestions, Push protected-bystander validation before Energy/cooldown cost, and owner-only Shield range/boundary diagnostics.
- Added factual Guardian after-action review state for completed, cancelled, failed, and load-invalidated plans.
- Added opt-in in-memory timing summaries, owner-only path/task/chat developer view, F8 developer overlay, local diagnostic export detail, and `/riftcompanions_dev perf`; this is not a profiler or benchmark claim.
- Bumped Team SavedData/Blackboard migration markers conservatively and bumped network protocol to `21` for the expanded owner status snapshot.
- Refreshed stale Bible/compliance/readme wording for Team Supply, Hive Surge, optional promises, policy editor, first-home milestones, and source-vs-runtime distinctions.

## Source milestone 1.8 — Mode Policy, Performance Tiers, and Decision Leadership

- Added explicit Survival/Creative/Peaceful/Spectator companion policy and non-punitive command contract.
- Added LIGHT/STANDARD/CINEMATIC performance tiers that scale optional work without raising untested active squad limits.
- Added bounded Team Decision domains, information/safety leads, evidence keys, and discussion cooldowns for event conflict handling.
- Added English-only mode/performance/decision contract and runtime test matrix.

## Source milestone 1.7 — Personality, Will Evidence, and Max Scout Foundation

- Added data-driven companion behavior profiles separating personality, tactical role, capability, and forbidden action metadata.
- Added Will evidence confidence/stress state with LOW/MEDIUM/HIGH signal rules based on new evidence only.
- Added Max scout preconditions for health, downed teammate, boss context, danger score, and loaded-terrain radius limits.
- Added English-only role behavior/capability contract and acceptance matrix.

## Source milestone 1.6 — Encounter Intelligence and Role-Aware Dialogue

- Added persistent bounded encounter context from observed biome, visible threat, and player-approved structure-plan evidence.
- Added role/profile encounter dialogue mapping data, profile transition cooldown, risk escalation memory hooks, and safe formation recommendations.
- Added threat archetype profile helpers for Swarm, Ambusher, Ranged, Explosive, Controller, Heavy, Boss, and Unknown contexts.
- Added English-only encounter intelligence contract and runtime matrix; no hidden-world knowledge or forced plan behavior is claimed.

## Source milestone 1.5 — Mystery Board and Optional Promise Foundation

- Added bounded evidence-only Mystery Board clues from player investigation markers and visible Hive observations.
- Added one-at-a-time optional safe-base companion promises with accept/defer/completion flows and no timer, punishment, loot, or progression lock.
- Added story/promise commands, Journal story summary integration, and English-only contract/runtime test matrix.

## Source milestone 1.4 — Mind Anchor and Supported Mental Effects Foundation

- Added data-driven supported mental-effect profiles with conservative vanilla confusion/darkness baseline.
- Added Scout Grounding through the explicit support registry, plus disabled-by-default Anchor Point, Break Free, and Escape Window source actions.
- Added temporary local Mind Anchor state with Focus costs, safe-position validation, effect-duration reduction only, no block placement, no teleport, and no physical invulnerability.
- Added English-only Mind Anchor contract and runtime test matrix.

## Source milestone 1.3 — Resource Ownership and Team Supply Foundation

- Added conservative ownership classes, expanded item categories, role resource profiles, utility scoring, and bounded resource request dialogue.
- Added optional short-range WORLD_FREE item pickup with ownership/risk/policy/utility validation, item reservation, transaction confirmation, and default-off feature policy.
- Added explicit Team Supply container binding, role whitelist/daily caps, anchor reservation, withdrawal transaction, and journal memory; Team Supply remains disabled by default.
- Preserved strict prohibitions on arbitrary chest access, structure loot opening, rare/unknown auto-take, distant loot navigation, and item-fuel power exploits.
- Added English-only resource ownership contract and runtime acceptance matrix.

## Source milestone 1.2 — Encounter and Hive Surge Foundation

- Added rare explicit Seer Hive Surge modes for Suspend, Redirect, Shatter, and Swarm Freeze using the then tag-only validated channel pipeline; milestone 3.3 later broadens hostile eligibility without removing Safety Gates.
- Added emergency, protected-area, villager, animal, energy, strain, cooldown, recovery, target, and boss-resistance gates for Surge.
- Added advisory threat observable tells and counterplay vocabulary without hidden-world knowledge or permission escalation.
- Added English-only Hive Surge contract and runtime test matrix; no runtime balance or animation result is claimed.
- Added local-only acceptance telemetry commands and Go-rule summary for Test A–F playtest campaigns.

## Source milestone 1.1 — Consequence, Perception, Spatial, and Micro-Scene Foundation

- Added stable non-punitive consequence records for visible player annotations, safe anchors, and approved/deferred plans.
- Added opt-in player identity state with safe nickname validation and play-style commands; alerts still retain player-name safety.
- Added bounded local companion perception signals with source, confidence, actionability, timestamp, and expiry; no wall vision or forced chunk scans.
- Added P0/P1 interruption cleanup for Scout/base/observe work while preserving active rescue safety.
- Added temporary player interaction-radius etiquette so formations/navigation yield chest and interaction space without container access.
- Added camera-safe Campfire/Threshold/Return/Landmark/Silent Walk micro-scene hooks and four original English scene lines.
- Added advisory threat tells/counterplay fields, expanded test contracts, and English-only documentation.

## Source milestone 1.0 — Navigation Safety and Formation Recovery Layer

- Added `CompanionNavigationService`: server-authoritative bounded candidate planning over vanilla path search.
- Added full bounded path-node validation for fluids, hazards, doors, ladders, vines, cobwebs, leaves, fence gates, annotation zones, and unsafe cliff drops.
- Added configuration-controlled replan cadence, no-progress timeout, alternate candidate budget, path-node budget, safe-drop limit, and slot reservation radius.
- Added progress-based Stuck Recovery escalation instead of repeated path creation every tick.
- Added expiring formation slot reservations to prevent companion pileups and camera/interaction block crowding.
- Added navigation cleanup on Safe Mode, logout/session clear, companion discard, and RESTING transition.
- Added navigation architecture/performance/test contract documentation. Runtime playtest remains pending.

## Source milestone 0.9 — Deterministic Resting State, Policy Editor, and Advisory Profiles

- Added transaction-protected server-owned RESTING snapshots for non-selected duo companions. Snapshot restore is restricted to a safe base in the original dimension and cannot become remote/cross-dimension storage.
- Added normal dismissal recovery for a RESTING snapshot so bounded personal items return exactly once to the owner.
- Added player-visible PlanTarget persistence and same-dimension/loaded-target validation for Structure Entry plans and save recovery.
- Added idempotent journal-milestone and companion-unlock action records, including first safe return and first plan from home memories.
- Added per-owner Seer/Gifted/Scout power-policy overrides, safe Journal Policies tab, fixed role-only policy packet, and command surface. World config remains fallback; Safety Gates remain final.
- Added advisory data-pack reloaders for threat profiles and observed-block structure classification. They can add cautious vocabulary only and cannot discover hidden structures or grant permissions.
- Added persisted companion fault records, repeated-fault circuit breaker to Safe Mode, and a local migration backup advisory file for migration failure review.
- Refined compact HUD to show active/downed companions only; RESTING status now states that bounded personal state is preserved.

## Source milestone 0.8 — Reliability, Duo, Arc, and Presentation Foundation

- Added a bounded server-side action ledger, action UUIDs, transaction phases, and expiring reservations for sensitive lifecycle, plan, inventory, power, rescue, duo, and intention operations.
- Added version markers for team, blackboard, plan, companion, inventory, memory, journal, doctrine, and config contracts, plus conservative migration hooks.
- Added pre-recap save recovery, stale navigation/rescue cancellation, invalid inventory-entry reporting, `FAILED_SAFE` plan recovery, and persisted readable Safe Mode.
- Added Safe Mode UI/command controls that preserve Follow, Safe Recall, and Journal while isolating plans, powers, automatic combat, Hive focus, and optional initiative.
- Added six server-validated two-companion duo profiles, safe HOME/REST selector in the Team Journal, squad command, bounded non-damage synergy notes, and safe fallback switching.
- Added data-driven optional companion intentions, four bounded arc trackers, rotating dawn ambience, and 28 new original English dialogue lines (315 total).
- Added safe item-classification data-pack reload support alongside existing dialogue/tags/compatibility infrastructure.
- Added authoritative `CompanionVisualState`, revised animation contract, Safe Mode/duo/intention HUD and Journal projection, asset registry, persona review, and visual/audio QA plan.
- Added fixed chaos scenario briefing command and a pending runtime acceptance campaign; no runtime result is claimed.
- Added English-only reliability, data-pack, chaos, duo/arc, asset, visual, and audio documentation.

## Source milestone 0.5 — Modular Story, Content, and Safety Systems

- Added PERSONAL/PUBLIC content profiles, original public textures, and content-mode configuration.
- Added optional alternate-continuity Story chapters with start/defer/status commands and non-forced milestone hooks.
- Added encounter adapter registry with Unknown/Caution fallback for failed or missing compatibility behavior.
- Added ChatProfile and Low Effects policies; plans/AI remain independent from chat/VFX visibility.
- Added contextual server-derived pings and pressure-plate avoidance.
- Added conversation topic cooldown and context-aware topic availability.
- Expanded the default dialogue pack to 287 original English entries with at least 70 lines per core role.
- Tightened anti-exploit rules for Disrupt, Push, dismissal inventory return, and downed-companion hostile retargeting.
- Added optional compatibility-pack reload infrastructure with Unknown/Caution fallback.
- Added autonomy, combat, recall, individual mute, low-effects, and content-mode profile configuration.
- Added server-derived contextual pings, `why` diagnostics, and permission-2 development test commands.

## Source milestone 0.7 — Encounter Profiles

- Added biome, structure, threat, and journey encounter profile contracts with safe Unknown/Caution fallbacks.
- Added visible-evidence structure classification, built-in vanilla biome profiles, threat archetypes, and recent-plan variety cooldown.

## Source milestone 0.6 — Hive Control Contract

- Added server-authoritative focus/release channels for Suspend, Redirect, Shatter, and Swarm Freeze.
- Added per-target persistent control state, safe release, boss resistance levels, team reactions, HUD timer/status, low-effects particle caps, and original synthesized Hive OGG cues.
- Added boss adapter contract and non-destructive default boss fallback.

## Source milestone 0.4 — English-only runtime conversion

- Converted all packaged runtime UI, feedback, conversation, commands, default localization, and dialogue to English-only.
- Replaced the default dialogue pack with `core_en_us.json` and 287 original English dialogue lines.
- Removed `ar_iq.json`; `en_us` is the only packaged locale.
- Added strict runtime and full-project language audits.

## Source milestone 0.3 — Team Systems Expansion

- Formation Coordinator with Follow/Cave/Combat/Retreat layouts.
- P0–P4 Event Queue, Team Director, Retreat/Defend/Structure Entry plans, and player approval flow.
- Structure/environment assessment, anchors, Base Life v1, annotations, doctrines, trust, recap, quiet windows, and diagnostics.
- Story Downed status bands, rescue reservation, player-death fallback, and safe recovery fallback.
- Six-slot manual personal inventory.
- Will Disrupt, Max Grounding, and per-role power policies.
- Original Hive Walker, Whisperer, Brute, and Anchor source entities with 128px assets.

## Deliberately deferred

- Automatic pickup remains default-off; Team Supply source remains disabled by default pending Forge transaction/save tests. Arbitrary chest access is not planned.
- Transport, portals, and cross-dimension transfer.
- Full boss content/phase ecosystem and runtime balance for source-existing Hive Surge, boss adapters, and advanced Mind Anchor contracts.
- Broad compatibility packs, full Story Graph, full audio production, advanced VFX, controller radial input, and runtime benchmark evidence.
