# Rift Companions — Minecraft Forge 1.20.1

> **English-only project policy:** all packaged runtime source, resources, menus, feedback, commands, dialogue, configuration labels, and default localization are English-only.

Rift Companions is a source-only Forge 1.20.1 companion framework for an offline/single-player Minecraft experience. It separates gameplay roles, server authority, client presentation, personal content assets, and future compatibility layers.

## Build workspace status

The owner approved a real Forge build workspace on 2026-08-06. The repository now includes the Gradle wrapper, ForgeGradle configuration, Java 17 target, and GeckoLib compile dependency needed for Minecraft 1.20.1 / Forge 47.4.22.

- A clean `./gradlew clean build` completed successfully.
- The project does not bundle Forge, GeckoLib, Spark, or other dependency binaries inside the source tree.
- Generated `build/`, `.gradle/`, `run/`, and cache output are not clean source deliverables.
- A successful build does **not** mean runtime gameplay, Save/Load, animation, performance, or playtest verification has passed.

See [`NO_BUILD_FILES.md`](NO_BUILD_FILES.md), [`docs/BUILD_HANDOFF.md`](docs/BUILD_HANDOFF.md), and [`docs/BUILD_VALIDATION_REPORT.md`](docs/BUILD_VALIDATION_REPORT.md).

## Current implementation

### Visual assets

- Eight companion texture atlases at **512×512 RGBA** (four personal and four public), fully replaced for an original faceted-character presentation: tapered skin planes, separate eyes/lids/brows/mouth, layered hair, articulated clothing, and grounded late-1980s silhouettes:
  - Will / Seer
  - Hopper / Guardian
  - Eleven / Gifted
  - Max / Scout
- Four original faceted GeckoLib characters on the animation-safe shared canonical humanoid v2 backbone, with 68–72 bones and 57–61 cuboid layers per role. Every role has a tapered head, independent eye/eyelid/brow/jaw components, layered hair bones, and articulated body segments; visual geometry remains presentation-only.
- Server-derived GeckoLib animation mapper with authored blink, local eye drift, brow/jaw expression, secondary hair motion, and differentiated role locomotion/combat presentation. All visual bones remain non-authoritative.
- Safe Recall / first-spawn anti-stacking placement: living-entity clearance plus bounded per-owner destination reservations prevent same-square companion recall bursts; a tight-space request safely rejects or holds rather than piling entities together.
- Visible hostile response director: local loaded threats attacking player/companions clear stale formation, assign bounded defensive targets, trigger a small authored dialogue exchange, propose Defend only for player approval, and use Retreat only under existing critical-pressure gates. Companions do not freeze under a stale Follow path.
- Contextual Interaction Director: 198 multi-stage Discovery/Revisit scenes and 1,584 English contextual lead/reply lines react to visible animals, safely observed distant mobs, player-used work blocks, weather/time, biome changes, player-selected structures, and fatigue through nine contextual animation actions. It never controls animals, grants loot, reads containers, uses x-ray, forces a mission, or generates runtime text.
- No custom hostile entities, enemy textures, hostile worldgen, or monster-spawn system are registered. Companions interact with the normal Minecraft hostile world instead.
- Five original synthesized Hive OGG cue assets plus a registered original companion, UI, and ambience cue library for confirmed presentation events; no actor voices or show music.
- UV guide: [`docs/TEXTURE_UV_LAYOUT.md`](docs/TEXTURE_UV_LAYOUT.md)
- Model source preview: [`docs/faceted_character_model_preview.png`](docs/faceted_character_model_preview.png) — source review, not an in-game render
- Character texture atlases are intentionally not bundled; supply replacement textures before using the companion renderer in-game.

### Core team systems

- Persistent lifecycle, UUID duplicate prevention, durable action IDs, bounded transaction ledger, and expiring reservations.
- Versioned team/plan/companion/inventory/roster data with conservative migration, target validation, local backup advisory, and pre-recap load recovery.
- Per-owner Safe Mode that preserves Follow, Safe Recall, and Journal while isolating optional risky systems and repeated companion fault circuit breakers. Stop All Actions is an explicit persisted manual Safe Mode control.
- Follow, hold, guard, regroup, retreat, return-home, base activity, Safe Recall, bounded Stuck Recovery, and one-role Task Reset fallback.
- Server-authoritative custom navigation safety layer over vanilla path search: bounded alternatives, door/ladder/fluid/cliff rejection, path-node validation, rate-limited replans, progress monitoring, and Safe Recovery escalation.
- Formation Coordinator: FOLLOW, CAVE, COMBAT, and RETREAT layouts with interaction-block/annotated-zone avoidance and expiring slot reservations.
- Event queue with P0–P4 priorities, merge/expiry behavior, Team Director coordination, explicit decision domains, gameplay-mode policy, and performance tiers.
- Server-derived Vanilla rule policy for Peaceful, Creative, Spectator, Mob Griefing, Keep Inventory, Daylight Cycle, and Hardcore; companion world edits remain disabled by design.
- Optional persistent English guidance, a Team Journal Guide tab, and an alert-only chat accessibility profile that never changes AI authority.
- Retreat, Defend, and Structure Entry plans with player approval, plan A/B, abort conditions, timeout, and cancellation paths.
- Bounded local threat, biome, structure-plan, and encounter-context assessment with role-aware formation/dialogue mappings. No x-ray, chest reading, hidden structure discovery, or forced chunk loading.
- Home, rest, entry, guard, journal, supply, lookout, quiet, and memorial anchors.
- Player annotations, doctrine candidates, optional alternate-continuity story chapters, and player-deferrable story hooks.
- Data-driven optional intentions, bounded companion-arc memories, first-safe-return/base-plan milestones, rotating dawn ambience, safe item classifications, advisory threat profiles, observed-block structure overrides, non-punitive consequences, local perception signals, and dynamic silence/micro-scenes.
- Data-driven personality/tactical behavior profiles, opt-in player identity data, per-player power policies, active two-companion duo selection at safe HOME/REST anchors, bounded RESTING snapshots, and cooldown-bound non-damage synergies.
- Gifted readiness bands with policy-aware non-auto suggestions, Push protected-bystander checks, owner-only Shield range checks, and Guardian factual after-action review cards.
- Trust summaries based on shared events, never on hidden dialogue penalties.
- A bounded Companion Social Director for two active nearby companions: authored lead/reply exchanges around campfires, weather, horizon, safe base, player-selected visible work blocks, cave/village context, travel, and calm moments. It never reads containers, scans hidden terrain, changes player input, or auto-casts gameplay actions.

### Companion gameplay

- **Seer:** Hive Sense plus Will Disrupt/control modes for active visible hostile mobs, using a deterministic control-heart budget, target cap, energy/strain, cooldown, recovery, protected-boundary checks, and short partial boss resistance.
- **Guardian:** practical guard/retreat lead, formation frontline, focus-target support, and explicit short-range Brace mitigation with no invulnerability or farm path.
- **Gifted:** limited Push, Shield, and Emergency Rescue with policy checks.
- **Scout:** short-range loaded-chunk scout, temporary visible-hostile Signal marking, Grounding, and disabled-by-default advanced Mind Anchor actions for explicit supported mental effects only.
- Story Downed with stable/danger/unreachable/rescuing/recovering states, priority scoring, short-lived rescue reservation, role-readable support choreography, safe fallback, and recovery limits.
- Six-slot personal companion inventory with explicit transaction-confirmed transfer, conservative ownership classes, optional short-range WORLD_FREE pickup disabled by default, and safe-base/original-dimension RESTING preservation only. There is no default auto-loot, private-chest access, remote storage, or cross-dimension inventory restore.

### English-only player experience

- Default dialogue: [`core_en_us.json`](src/main/resources/data/riftcompanions/companions_dialogue/core_en_us.json)
- **534 original English dialogue entries** (at least 70 for each core role), including authored two-companion social lead/reply exchanges.
- English-only HUD, Team Journal, Command Wheel, expanded Conversation screen, feedback, diagnostics, recap, tutorial guidance, Guide tab, and Social tab.
- `CRITICAL_ONLY`, `MINIMAL`, `STANDARD`, and `CINEMATIC` dialogue profiles; presentation settings never change safety logic.
- One packaged locale: `en_us`.
- Conversation topics: plan, health, memory, place, role limits, accept/delay plan, rest, read the world, team check, last encounter, and check in.

### Natural-world integration

Rift Companions does not add hostile creatures. Will, Guardian, Gifted, and Scout react to existing Minecraft hostile mobs and compatible hostile mod mobs through visible, loaded, server-validated context only.

## Team commands

> Commands require the normal single-player command permission level.

```mcfunction
/companions spawn guardian
/companions spawn seer
/companions spawn gifted
/companions spawn scout

/companions follow
/companions regroup
/companions hold guardian
/companions guard guardian
/companions focus
/companions ping target
/companions ping route
/companions ping danger
/companions ping investigate
/companions ping item
/companions retreat
/companions cancel_plan
/companions plan accept
/companions plan decline
/companions structure
/companions recall
/companions recall scout
/companions unstuck gifted
/companions reset_task guardian
/companions stop_all_actions
/companions dismiss seer
/companions dismiss_all
/companions combat on
/companions combat off
/companions safe_mode
/companions safe_mode clear
/companions tutorial status
/companions tutorial dismiss
/companions tutorial resume
/companions squad select guardian seer
/companions intention status
/companions intention accept
/companions intention defer
/companions identity nickname "Trail Guide"
/companions identity style explorer
/companions identity status
/companions playtest rate clarity 5
/companions playtest status
/companions policy cycle seer
/companions policy reset gifted
/companions rescue
/companions return_home
/companions why guardian
/companions export_memory
/companions repair_save

# Permission-2 development test harness
/riftcompanions_dev dump
/riftcompanions_dev perf
/riftcompanions_dev event hostile_crowd
/riftcompanions_dev resource seer 50
/riftcompanions_dev force_stuck scout
/riftcompanions_dev chaos logout_during_active_plan
/riftcompanions_dev behavior guardian_low_health_known_route
/riftcompanions_dev fault scout

/companions anchor set home
/companions anchor set rest
/companions anchor set guard_post
/companions anchor clear home

/companions mark safe_route
/companions mark danger
/companions mark machine_no_go
/companions mark protected
/companions mark investigate
/companions unmark

/companions doctrine status stay_together
/companions doctrine accept mark_safe_routes
/companions doctrine decline return_before_night

/companions story status
/companions story start a_way_back
/companions story defer signs_in_the_world
/companions mystery status
/companions mystery defer clue_id
/companions promise status
/companions promise accept
/companions promise defer

/companions inventory give guardian
/companions inventory withdraw guardian 0
/companions inventory status guardian
/companions supply bind
/companions supply withdraw guardian
/companions supply status scout

/companions ability sense
/companions ability disrupt
/companions ability suspend
/companions ability redirect
/companions ability shatter
/companions ability swarm_freeze
/companions ability surge_suspend
/companions ability surge_redirect
/companions ability surge_shatter
/companions ability surge_swarm_freeze
/companions ability push
/companions ability shield
/companions ability rescue
/companions ability brace
/companions ability scout
/companions ability signal
/companions ability ground
/companions ability anchor_point
/companions ability break_free
/companions ability escape_window
```

## Still deliberately deferred

These systems are not falsely marked as complete:

- Automatic pickup remains disabled by default, and Team Supply remains a disabled-by-default source foundation pending runtime save/transaction tests; neither provides arbitrary chest access.
- Boats, mounts, minecarts, portals, and automatic cross-dimension transport.
- Full boss content/phase ecosystem and runtime balance for source-existing Hive Surge, boss-adapter, and advanced Mind Anchor contracts.
- Full Story Graph, long relationship arcs, and optional cinematic content. Current mystery/arc/intention/promise systems are bounded source foundations only.
- Full off-screen companion simulation, storage, and autonomous activity. Current RESTING snapshots are bounded, original-dimension, safe-base-only personal state; companions do not adventure, farm, chunk-load, or access player storage while inactive.
- Full audio production, advanced VFX networking, controller radial input, and compatibility packs.
- Forge runtime, Save/Load, TPS/MSPT, Spark, migration, chaos, visual, audio, and acceptance tests. The Forge build now succeeds, but these runtime gates remain unexecuted.

## Offline verification

Run these source/resource validation scripts without launching Minecraft:

```bash
python3 tools/validate_source_tree.py
python3 tools/verify_user_requirements.py
```

The first validates the 512px faceted-character texture/Geo/face/hair-animation contract, assets/resources/source structure, and the approved build scaffold. The second verifies Forge 1.20.1 metadata, GeckoLib build dependency setup, eight 512×512 textures, English-only project text, English dialogue, and expected core modules.

## Build and runtime handoff

The approved workspace currently uses:

- Java 17
- Minecraft 1.20.1
- Forge 47.4.22
- GeckoLib Forge 4.7.1.1

Read [`docs/BUILD_HANDOFF.md`](docs/BUILD_HANDOFF.md) and [`docs/BUILD_VALIDATION_REPORT.md`](docs/BUILD_VALIDATION_REPORT.md) before beginning runtime world tests.

## Documentation

- [`docs/IMPLEMENTATION_STATUS.md`](docs/IMPLEMENTATION_STATUS.md)
- [`docs/TEAM_SYSTEMS_SPRINT.md`](docs/TEAM_SYSTEMS_SPRINT.md)
- [`docs/TEST_PLAN.md`](docs/TEST_PLAN.md)
- [`docs/UI_NETWORK_RESCUE_ANCHORS.md`](docs/UI_NETWORK_RESCUE_ANCHORS.md)
- [`docs/ANIMATION_CONTRACT.md`](docs/ANIMATION_CONTRACT.md)
- [`docs/ANIMATION_DIRECTOR_SYSTEM.md`](docs/ANIMATION_DIRECTOR_SYSTEM.md)
- [`docs/ANIMATION_REVIEW_AUDIT.md`](docs/ANIMATION_REVIEW_AUDIT.md)
- [`docs/TEXTURE_ART_DIRECTION_CONTRACT.md`](docs/TEXTURE_ART_DIRECTION_CONTRACT.md)
- [`docs/SOCIAL_DIALOGUE_AND_INTERACTION_DIRECTOR.md`](docs/SOCIAL_DIALOGUE_AND_INTERACTION_DIRECTOR.md)
- [`docs/ORIGINAL_AUDIO_CUE_LIBRARY.md`](docs/ORIGINAL_AUDIO_CUE_LIBRARY.md)
- [`docs/BIBLE_GAP_AUDIT.md`](docs/BIBLE_GAP_AUDIT.md)
- [`docs/DIALOGUE_SCHEMA.md`](docs/DIALOGUE_SCHEMA.md)
- [`docs/SUPPORT_PACK_GUIDE.md`](docs/SUPPORT_PACK_GUIDE.md)
- [`docs/HIVE_CONTROL_CONTRACT.md`](docs/HIVE_CONTROL_CONTRACT.md)
- [`docs/HIVE_SURGE_CONTRACT.md`](docs/HIVE_SURGE_CONTRACT.md)
- [`docs/WILL_GIFTED_PRECISION_POWER_CONTRACT.md`](docs/WILL_GIFTED_PRECISION_POWER_CONTRACT.md)
- [`docs/MIND_ANCHOR_CONTRACT.md`](docs/MIND_ANCHOR_CONTRACT.md)
- [`docs/ENCOUNTER_PROFILE_CONTRACT.md`](docs/ENCOUNTER_PROFILE_CONTRACT.md)
- [`docs/ENCOUNTER_INTELLIGENCE_CONTRACT.md`](docs/ENCOUNTER_INTELLIGENCE_CONTRACT.md)
- [`docs/ROLE_BEHAVIOR_AND_CAPABILITY_CONTRACT.md`](docs/ROLE_BEHAVIOR_AND_CAPABILITY_CONTRACT.md)
- [`docs/ROLE_READINESS_AND_AFTER_ACTION_CONTRACT.md`](docs/ROLE_READINESS_AND_AFTER_ACTION_CONTRACT.md)
- [`docs/MODE_PERFORMANCE_AND_DECISION_CONTRACT.md`](docs/MODE_PERFORMANCE_AND_DECISION_CONTRACT.md)
- [`docs/WORLD_RULES_ONBOARDING_ACCESSIBILITY_CONTRACT.md`](docs/WORLD_RULES_ONBOARDING_ACCESSIBILITY_CONTRACT.md)
- [`docs/OPERATIONAL_SAFETY_AND_OBSERVABILITY.md`](docs/OPERATIONAL_SAFETY_AND_OBSERVABILITY.md)
- [`docs/SCOPE_GOVERNANCE_AND_PUBLICATION_POLICY.md`](docs/SCOPE_GOVERNANCE_AND_PUBLICATION_POLICY.md)
- [`docs/NAVIGATION_AND_STUCK_RECOVERY.md`](docs/NAVIGATION_AND_STUCK_RECOVERY.md)
- [`docs/CONSEQUENCE_PERCEPTION_SCENES.md`](docs/CONSEQUENCE_PERCEPTION_SCENES.md)
- [`docs/STORY_MYSTERY_PROMISE_CONTRACT.md`](docs/STORY_MYSTERY_PROMISE_CONTRACT.md)
- [`docs/STORY_JOURNAL_PRESENTATION_CONTRACT.md`](docs/STORY_JOURNAL_PRESENTATION_CONTRACT.md)
- [`docs/RESOURCE_OWNERSHIP_AND_TEAM_SUPPLY.md`](docs/RESOURCE_OWNERSHIP_AND_TEAM_SUPPLY.md)
- [`docs/TEAM_SUPPLY_STATUS_UI_CONTRACT.md`](docs/TEAM_SUPPLY_STATUS_UI_CONTRACT.md)
- [`docs/PLAYTEST_CAMPAIGN.md`](docs/PLAYTEST_CAMPAIGN.md)
- [`docs/RELIABILITY_AND_RECOVERY.md`](docs/RELIABILITY_AND_RECOVERY.md)
- [`docs/FEATURE_FLAGS_AND_DATA_PACKS.md`](docs/FEATURE_FLAGS_AND_DATA_PACKS.md)
- [`docs/CHAOS_AND_ACCEPTANCE_TESTS.md`](docs/CHAOS_AND_ACCEPTANCE_TESTS.md)
- [`docs/BEHAVIOR_ACCEPTANCE_CATALOG.md`](docs/BEHAVIOR_ACCEPTANCE_CATALOG.md)
- [`docs/DUO_ARCS_INTENTIONS.md`](docs/DUO_ARCS_INTENTIONS.md)
- [`docs/VISUAL_HUD_AUDIO_QA.md`](docs/VISUAL_HUD_AUDIO_QA.md)
- [`docs/ASSET_REGISTRY.md`](docs/ASSET_REGISTRY.md)
- [`CHANGELOG.md`](CHANGELOG.md)
