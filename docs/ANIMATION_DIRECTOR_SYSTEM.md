# Character Animation Director System

> The current faceted head, independent eyes/lids/brows/jaw, secondary hair motion, and role-specific locomotion contract is documented in [`FACETED_CHARACTER_RIG_CONTRACT.md`](FACETED_CHARACTER_RIG_CONTRACT.md). Straight-path body heading and head-only glance/rear-check behavior are documented in [`NATURAL_GAIT_AND_HEAD_LOOK_CONTRACT.md`](NATURAL_GAIT_AND_HEAD_LOOK_CONTRACT.md). These are presentation-only bones and never alter gameplay authority.

## Status and authority

This is the authoritative **source design and asset contract** for the four companion roles in the Forge 1.20.1 source tree.

```text
Authoritative AI / safety state
        -> CompanionVisualState + CompanionAction
        -> CompanionAnimationStateMapper
        -> GeckoLib clip selection
        -> optional local VFX / sound cue
```

The logical server owns AI state, target validation, damage, inventory, plans, rescue, power effects, cooldowns, and save recovery. GeckoLib animation is presentation only.

A marker is a timing label for visual/audio synchronization and QA. It is never a client authority boundary. The server resolves a validated power, rescue, or damage result before it requests a confirmed success animation. A cancelled or rejected action must not play a success clip.

The system deliberately has no forced cutscene, camera lock, input lock, time stop, screen shake, block action, inventory mutation, or client-controlled damage.

Runtime animation blending, marker callback API behavior, visual clipping, and FPS evidence remain unverified until a Java 17 + Forge 47.x + GeckoLib 4 test workspace is allowed.

---

## 1. Shared Blockbench rig

### Canonical rig resource

The machine-readable rig contract is packaged at:

```text
assets/riftcompanions/animation_contracts/shared_humanoid_rig.json
```

All four Geo JSON models use:

```text
riftcompanions_rig_contract = shared_humanoid_v2
```

The geometry upgrade keeps the old `left_arm`, `right_arm`, `left_leg`, and `right_leg` bridge bones as compatibility parents for old clips. New Blockbench work must target the canonical bones below.

### Hierarchy

```text
root
└── body
    └── chest
        ├── head
        │   ├── jaw
        │   └── accessory_1
        ├── legacy left_arm bridge
        │   └── left_arm_upper
        │       └── left_arm_lower
        │           └── left_hand
        ├── legacy right_arm bridge
        │   └── right_arm_upper
        │       └── right_arm_lower
        │           └── right_hand
        └── accessory_2
root
├── legacy left_leg bridge
│   └── left_leg_upper
│       └── left_leg_lower
│           └── left_foot
└── legacy right_leg bridge
    └── right_leg_upper
        └── right_leg_lower
            └── right_foot
```

Role-specific visual pieces such as tapered face planes, eyes, lids, brows, mouth, hair locks, collars, cuffs, and ordinary clothing layers are children of `accessory_1`, `accessory_2`, or an appropriate canonical limb. They never decide hitboxes or gameplay.

### Pivot and constraint table

| Bone | Pivot X,Y,Z | Rotation range X | Rotation range Y | Rotation range Z | Notes |
|---|---:|---:|---:|---:|---|
| `root` | 0,0,0 | -5..5 | -8..8 | -5..5 | No translation animation for gameplay motion. |
| `body` | 0,12,0 | -18..18 | -25..25 | -12..12 | Legacy bridge for torso clips. |
| `chest` | 0,20,0 | -20..20 | -20..20 | -12..12 | Breathing and upper-body secondary motion. |
| `head` | 0,24,0 | -35..35 | -55..55 | -25..25 | Do not use head lock beyond readable target intent. |
| `jaw` | 0,24,-4 | 0..25 | -5..5 | -5..5 | Optional talk/emotion cue only. |
| `left_arm_upper` | 5,22,0 | -115..75 | -65..65 | -75..75 | No exaggerated scale. |
| `left_arm_lower` | 5,16,0 | -10..125 | -20..20 | -15..15 | Elbow bend. |
| `left_hand` | 5,12,0 | -30..30 | -30..30 | -30..30 | VFX origin support. |
| `right_arm_upper` | -5,22,0 | -115..75 | -65..65 | -75..75 | No exaggerated scale. |
| `right_arm_lower` | -5,16,0 | -10..125 | -20..20 | -15..15 | Elbow bend. |
| `right_hand` | -5,12,0 | -30..30 | -30..30 | -30..30 | VFX origin support. |
| `left_leg_upper` | 2,12,0 | -70..70 | -25..25 | -18..18 | Walk/run/stance only. |
| `left_leg_lower` | 2,6,0 | -8..115 | -12..12 | -10..10 | Knee bend. |
| `left_foot` | 2,0,-0.5 | -25..35 | -15..15 | -15..15 | Keep ground readable. |
| `right_leg_upper` | -2,12,0 | -70..70 | -25..25 | -18..18 | Walk/run/stance only. |
| `right_leg_lower` | -2,6,0 | -8..115 | -12..12 | -10..10 | Knee bend. |
| `right_foot` | -2,0,-0.5 | -25..35 | -15..15 | -15..15 | Keep ground readable. |
| `accessory_1` | 0,24,0 | head inherited | head inherited | head inherited | Head gear / hair / role display piece. |
| `accessory_2` | 0,20,0 | chest inherited | chest inherited | chest inherited | Backpack / belt / equipment display piece. |

### Rig invariants

1. Entity hitboxes never follow animated bones.
2. No clip may use large scale pulses that visually clip through terrain or other entities.
3. Accessories may be animated only after core body readability is correct.
4. Do not put gameplay data, target IDs, item NBT, or Java callbacks in a Blockbench bone.
5. New clips target canonical bones. Legacy bridge bones exist only to keep existing clips compatible.
6. A model may add cosmetic child bones, but it may not rename or remove canonical bones without a migration and animation audit.

---

## 2. State machine and transition contract

The complete machine-readable state set is packaged at:

```text
assets/riftcompanions/animation_contracts/companion_animation_states.json
```

### Core state table

| State family | Duration | Playback | Priority | Interruptible | Primary interrupters |
|---|---:|---|---|---|---|
| `IDLE_CALM` | 3.00s | Loop | Ambient | Yes | Alert, combat, rescue, downed |
| `IDLE_ALERT` | 0.45s | Loop | Combat | Yes | Combat, rescue, downed |
| `IDLE_COMBAT_READY` | 1.00s | Loop | Combat | Yes | Attack, power, rescue, downed |
| `WALK_FORWARD` | 0.70s | Loop | Locomotion | Yes | Alert, combat, downed |
| `WALK_CROUCHED` | 0.82s | Loop | Locomotion | Yes | Alert, combat, downed |
| `RUN` | 0.48s | Loop | Combat | Yes | Rescue, downed |
| `TURN_LEFT` / `TURN_RIGHT` | 0.24s | One-shot | Locomotion | Yes | Combat, downed |
| `COMBAT_MELEE_ATTACK_1` | 0.82s | One-shot | Combat | No after confirm | Downed |
| `COMBAT_MELEE_ATTACK_2` | 0.88s | One-shot | Combat | No after confirm | Downed |
| `COMBAT_MELEE_ATTACK_3` | 1.10s | One-shot | Combat | No after confirm | Downed |
| `COMBAT_MELEE_BLOCK` | 0.72s | One-shot | Combat | Yes | Retreat, downed |
| `COMBAT_MELEE_HIT_REACT` | 0.34s | One-shot | Combat | Yes | Rescue, downed |
| `COMBAT_RANGED_DRAW` | 0.36s | One-shot | Combat | Yes | Retreat, target invalid, downed |
| `COMBAT_RANGED_AIM` | 0.80s | Loop | Combat | Yes | Retreat, target invalid, downed |
| `COMBAT_RANGED_RELEASE` | 0.34s | One-shot | Combat | Yes | Downed |
| `COMBAT_DODGE_LEFT/RIGHT/BACK` | 0.32s | One-shot | Combat | Yes | Downed |
| `DOWNED_FALL` | 1.00s | One-shot | Downed | No | None |
| `DOWNED_IDLE` | 1.80s | Loop | Downed | No | Recovery Stand Up |
| `DOWNED_RESCUE_ACCEPT` | 0.60s | One-shot | Rescue | No | Downed |
| `RECOVERY_STAND_UP` | 0.90s | One-shot | Rescue | Yes | Downed |
| `STUCK_STRUGGLE` | 0.72s | Loop | Interaction | Yes | Safe Recall, downed |
| `STUCK_RECALLED` | 0.52s | One-shot | Rescue | Yes | Downed |
| `SPAWN_APPEAR` | 0.68s | One-shot | Interaction | Yes | Downed |
| `DISMISS_FADE` | 0.52s | One-shot | Interaction | Yes | None |
| `INTERACT_TALK` | 1.60s | Loop | Interaction | Yes | Alert, downed |
| `INTERACT_POINT` | 0.72s | One-shot | Interaction | Yes | Alert, downed |
| `INTERACT_PICKUP` | 0.58s | One-shot | Interaction | Yes | Alert, downed |
| `INTERACT_USE` | 1.15s | Loop | Interaction | Yes | Alert, downed |
| `EMOTION_FEAR` | 0.56s | One-shot | Interaction | Yes | Alert, downed |
| `EMOTION_RELIEF` | 0.72s | One-shot | Interaction | Yes | Alert, downed |
| `EMOTION_DETERMINED` | 0.62s | One-shot | Interaction | Yes | Alert, downed |
| `EMOTION_EXHAUSTED` | 1.20s | Loop | Interaction | Yes | Downed, recovery |
| `CALM_LOOK_AROUND` | 2.80s | Loop | Ambient | Yes | Alert, downed |
| `CALM_SIT_REST` | 2.00s | Loop | Ambient | Yes | Alert, downed |

### Required blend targets

| Transition | Preferred blend |
|---|---:|
| Idle -> Walk | 0.20s |
| Walk -> Run | 0.30s |
| Any readable state -> Combat | 0.15s |
| Combat -> Recovery | 0.30s |
| Any state -> Downed | 0.10s |
| Power Focus -> clean cancel | 0.10s |
| Calm emote -> Alert | 0.10s |
| Stuck Struggle -> Safe Recall visual | 0.10s |

`CompanionAnimationStateMapper` expresses the requested clip, priority, playback mode, and preferred blend. Exact GeckoLib blend implementation must be verified after the runtime workspace exists; it must never delay safety or navigation state.

### Forbidden transitions

- `DOWNED_IDLE` -> attack, power focus, plan cue, or movement loop.
- `CALM_SIT_REST` -> melee attack without passing through alert/combat readiness.
- `POWER_FOCUS` -> release success if target/policy/range/server validation failed.
- `DISMISS_FADE` -> spawn without a lifecycle transaction and new entity validation.
- Any animation -> block place, inventory edit, plan start, memory write, or arbitrary damage.

---

## 3. Role-specific combat and ability keyframes

All frame references below use **20 frames per second** for Blockbench authoring. They map to the exact seconds in the packaged animation assets. Server action outcome is independent of client frame delivery.

### Guardian

#### `animation.guardian.melee_attack_1` — 0.82s / 16 frames

| Phase | Frames | Time | Primary bones | Purpose |
|---|---:|---:|---|---|
| Anticipation | 0–4 | 0.00–0.18 | chest +8°, right upper arm +22° | Heavy wind-up, not fast aggression. |
| Strike | 4–8 | 0.18–0.42 | chest -10°, right upper arm -72° | Defensive close strike. Visual marker at frame 8. |
| Recovery | 8–16 | 0.42–0.82 | chest and arms return neutral | Readable reset, no locked locomotion. |

Marker: `APPLY_MELEE_DAMAGE` at **0.42s / frame 8**. The actual vanilla damage result has already been resolved and confirmed on the server; the marker is a presentation alignment point.

#### `animation.guardian.melee_block` — 0.72s / 14 frames

| Phase | Frames | Time | Bones |
|---|---:|---:|---|
| Raise | 0–4 | 0.00–0.18 | chest -8°, both upper arms -76° with outward Z angle |
| Hold | 4–10 | 0.18–0.48 | chest remains forward, arm angle softens to -72° |
| Lower | 10–14 | 0.48–0.72 | all return neutral |

The state is visual defense readability only. It grants no invulnerability and cannot modify a block.

#### `animation.guardian.guard_stance` — 1.40s loop

- Chest: 5° forward with a 2° breathing shift.
- Head: scans -7° to +7° yaw.
- Arms: held wide at -32° with small settle to -28°.
- Tiny lateral motion belongs to navigation/formation, not the clip.

#### `animation.guardian.retreat_signal` — 0.72s / 14 frames

- 0.00–0.20: right arm rises to -92°.
- 0.20–0.48: arm holds at -82°, head pitches -8°.
- 0.48–0.72: arm returns.
- Marker: `PLAY_SOUND` at **0.28s**; original cue only, medium volume, subtitle/HUD backup required.

### Seer

#### `animation.seer.anomaly_notice` — 0.78s / 16 frames

| Phase | Frames | Time | Bones |
|---|---:|---:|---|
| Stop/read | 0–6 | 0.00–0.28 | head -11° pitch, +12° yaw; chest -4° |
| Cautious settle | 6–16 | 0.28–0.78 | head returns only to -3°/+4° |

The source does not move the entity backward. Any actual path change remains AI/navigation-owned.

#### `animation.seer.hive_resist` — 0.68s / 14 frames

- 0.00–0.18: chest 7° back and 5° side tension; both arms rise slightly.
- 0.18–0.44: shoulders oppose direction with small counter-motion.
- 0.44–0.68: return neutral.
- No power marker. This is evidence/stress presentation only.

#### `animation.seer.hive_focus` — 1.25s loop

- Chest: -8° forward settle to -6°.
- Right arm: extends -78° then rests around -72°.
- Left arm: support at -33° then -30°.
- Head: limited -9° target attention, never a camera lock.
- Cancellation: target death, range/tag failure, policy failure, Safe Mode, damage interruption, or channel cancel return immediately through the mapper.

#### Seer release variants

| Clip | Duration | Key action | Marker |
|---|---:|---|---|
| `hive_release_suspend` | 0.62s | Upward push through right arm | `APPLY_POWER_EFFECT` at 0.28s |
| `hive_release_redirect` | 0.68s | Side pull and chest yaw | `APPLY_POWER_EFFECT` at 0.30s |
| `hive_release_shatter` | 0.76s | Heavier two-arm release, visible exertion | `APPLY_POWER_EFFECT` at 0.34s |

The server validates target visibility, tag, policy, energy, strain, cooldown, area, and boss resistance before the confirmed release cue. If validation fails before release, the clip cancels and no success VFX/sound plays.

#### `animation.seer.hive_recovery` — 1.20s

- 0.00–0.36: chest moves from -8° forward to +11° recovery, head from -10° to +12°, right arm lowers.
- 0.36–1.20: return neutral.
- If strain is high, use exhausted/recovery visual state; never force a fall, camera control, or player pause.

### Gifted

#### `animation.gifted.power_notice` — 0.48s / 10 frames

- 0.00–0.18: head rises -10°, right arm begins to lift -28°.
- 0.18–0.48: right arm settles at -42°.
- No effect marker. This only communicates readiness.

#### `animation.gifted.power_focus` — 0.90s loop

- Chest: -3° to -7° controlled lean.
- Both upper arms: -58° to -69° forward focus.
- Cancelled cleanly on Safe Mode, target failure, policy cancellation, damage interruption, or action expiry.

#### `animation.gifted.push_release` — 0.42s / 8 frames

| Phase | Frames | Time | Bones |
|---|---:|---:|---|
| Set | 0–2 | 0.00–0.12 | arms at focus pose, chest -7° |
| Release | frame 2 | 0.12 | arms reach -102°, chest -14° |
| Recovery | 2–8 | 0.12–0.42 | arms/chest return neutral |

Marker: `APPLY_POWER_EFFECT` at **0.12s**. `AbilityService` has already checked target, combat context, protected bystander boundary, energy, and policy. A rejected Push does not play this success release.

#### `animation.gifted.shield_hold` — 1.15s loop

- Both arms hold -84° with a small settle to -76°.
- No repeated effect marker. Shield duration and mitigation are server state.
- Interruption: Safe Mode, feature flag change, downed state, or action end.

#### `animation.gifted.rescue_pull` — 0.66s / 13 frames

- 0.00–0.24: chest -5° to -12°, arms -58° to -95°.
- Marker: `APPLY_RESCUE` at **0.24s**.
- 0.24–0.66: return to near-neutral.
- The safe destination must already pass server validation. The animation cannot pull an arbitrary entity.

#### `animation.gifted.exhausted_recovery` — 1.35s

- Chest 12° to 7° then neutral.
- Head 17° to 8° then neutral.
- No permanent collapse, no input lock, no repeated dramatic fall.

### Scout

#### Ranged set

| Clip | Duration | Frames | Key action | Marker |
|---|---:|---:|---|---|
| `ranged_draw` | 0.36s | 7 | arms enter aim posture | none |
| `ranged_aim` | 0.80s loop | 16 | chest stabilizes, aim breathes | cancelable |
| `ranged_release` | 0.34s | 7 | right arm releases at frame 2 | `APPLY_PROJECTILE` at 0.11s |

No autonomous projectile source is enabled in the current source milestone. This is an animation asset and future capability contract, not a claim that Scout currently fires arrows.

#### `animation.scout.route_point` — 0.76s

- 0.00–0.22: right arm rises to -92°, head yaws +14°.
- 0.22–0.52: held readable point at -85°.
- Marker: `SPAWN_MARKER` at **0.38s**.
- The marker may draw a local ground arrow/icon only after the server has already recorded a loaded-terrain route observation. It never loads a chunk.

#### Mind Anchor

| Clip | Duration | Action | Marker |
|---|---:|---|---|
| `mind_anchor_call` | 0.68s | stable two-arm call | `APPLY_ANCHOR_EFFECT` at 0.32s |
| `mind_anchor_hold` | 0.92s loop | maintained calm posture | no repeated effect marker |

`MindAnchorService` remains the only authority for supported effects, Focus, cooldown, safe position, and temporary radius.

---

## 4. Marker specification

### Allowed markers

```text
APPLY_POWER_EFFECT
APPLY_MELEE_DAMAGE
APPLY_PROJECTILE
APPLY_RESCUE
APPLY_ANCHOR_EFFECT
PLAY_SOUND
SPAWN_PARTICLE_BURST
SET_TARGET_VISUAL_STATE
BEGIN_RECOVERY
SPAWN_MARKER
```

### Forbidden marker behavior

```text
MODIFY_INVENTORY
PLACE_BLOCK
DAMAGE_RANDOM_ENTITY
START_PLAN
WRITE_MEMORY
CHANGE_AI_STATE
```

No forbidden behavior appears in `AnimationMarker`, `core_animation_timeline.json`, or the animation controller source.

### Cancellation rules

| Before marker | Result |
|---|---|
| Target dies, leaves range, loses required tag, or becomes protected | Cancel success release; no effect/VFX success cue. |
| Server Safety Gate rejects policy, cooldown, energy, strain, area, or bystander check | Do not request the success clip. |
| Safe Mode activates | Cancel focus/hold clip and return to safe presentation. |
| Companion goes down | Downed clip pre-empts every action. |
| Player cancels plan or task | Return to the next server-derived safe visual state. |

The asset timeline is stored at:

```text
assets/riftcompanions/animation_contracts/core_animation_timeline.json
```

---

## 5. VFX integration points

All VFX are original abstract shapes. They do not use show imagery, actor likeness, full-screen filters, or color-only communication.

| Capability | Bone + offset | Duration | Normal cap | Low Effects cap | Shape backup |
|---|---|---:|---:|---:|---|
| Gifted Push | `right_hand` + 0,0,-0.25 | 0.20s | 18 | 4 | outward arc |
| Gifted Shield | `chest` + 0,0.6,0 | 4.50s | 30 | 7 | thin ring outline |
| Gifted Rescue | `left_hand` + 0,0,-0.20 | 0.35s | 24 | 6 | directional pull line |
| Seer Hive Focus | `right_hand` + 0,0,-0.20 | 0.80s | 16 | 4 | target-link icon |
| Seer Suspend | `right_hand` + 0,0,-0.20 | 0.25s | 20 | 5 | upward ticks |
| Seer Shatter | `right_hand` + 0,0,-0.20 | 0.35s | 28 | 7 | outward segments |
| Scout Mind Anchor | `left_hand` + 0,-1,0 | 0.75s | 20 | 5 | ground circle icon |
| Scout Route Point | `right_hand` + 0,-1.1,0 | 0.50s | 12 | 3 | ground arrow icon |

Global cap per cast: **50 particles maximum**. Low Effects reduces trails and bursts but keeps a shape/icon/HUD/text backup. Screen shake remains off by default and is not implemented.

---

## 6. Sound integration contract

The project packages original synthesized Hive cues plus an original companion, UI, and ambience cue library. See `docs/ORIGINAL_AUDIO_CUE_LIBRARY.md` for generation provenance, cue names, action mapping, privacy, and runtime mix gates.

| Cue family | Marker | Volume | Category | Source status |
|---|---|---|---|---|
| `guardian_guard_signal` / `guardian_retreat` | 0.28s visual signal | Medium | Plan | Packaged original asset |
| `guardian_melee_swing` / `guardian_impact` | confirmed action cue | Low | Combat | Packaged original asset |
| Gifted focus/push/shield/rescue cues | matching visual action | Medium | Power | Packaged original asset |
| Scout route/anchor cues | matching visual action | Low | Route | Packaged original asset |

Rules:

- No show music, actor voices, voice imitation, clips, or extracted models.
- Alert cues take precedence over ambient cues.
- Swarm contexts must not stack an unbounded cue per target.
- Important audio needs a subtitle/HUD/chat/shape fallback.
- Audio options must be respected when a real audio mix is implemented.

---

## 7. GeckoLib resource topology

Actual runtime model resources remain one animation JSON per role because `CompanionGeoModel` selects one GeckoLib animation resource per companion role:

```text
assets/riftcompanions/
├── geo/
│   ├── guardian.geo.json
│   ├── seer.geo.json
│   ├── gifted.geo.json
│   └── scout.geo.json
├── animations/
│   ├── guardian.animation.json
│   ├── seer.animation.json
│   ├── gifted.animation.json
│   └── scout.animation.json
└── animation_contracts/
    ├── shared_humanoid_rig.json
    ├── companion_animation_states.json
    └── core_animation_timeline.json
```

Each role file contains the complete role clip family. This avoids duplicate source-of-truth files that a single `GeoModel` cannot load together without an additional merge/runtime layer.

Artist workflow:

```text
1. Edit a Blockbench copy using shared_humanoid_v2 and faceted_character_64_v2.
2. Export/update the role Geo JSON and aggregated role animation JSON.
3. Run tools/rebuild_faceted_character_assets.py to restore original geometry, all texture atlases, facial/hair motion, and source review output.
4. Run tools/validate_source_tree.py and tools/verify_user_requirements.py.
5. Later, run the real Forge visual/animation runtime matrix.
```

`tools/rebuild_faceted_character_assets.py` is the canonical visual rebuild entry point. `tools/upgrade_animation_rig.py` remains a legacy migration guard for an old export that lost its canonical backbone.

---

## 8. Java animation structure

```text
animation/
├── AnimationPlaybackMode.java
├── AnimationPriority.java
├── AnimationMarker.java
├── AnimationMarkerPlan.java
├── AnimationClip.java
├── AnimationStateMapper.java
├── CompanionAnimationStateMapper.java
├── CompanionAnimationHandler.java
└── CompanionAnimationController.java
```

### `CompanionAnimationStateMapper`

Maps only:

```text
CompanionState + CompanionVisualState + CompanionAction + movement flag
        -> AnimationClip key / playback / priority / preferred blend
```

It has a safe fallback for every visual state. Downed and Stuck Recovery pre-empt ordinary visual selection. It is not an AI planner.

### `CompanionAnimationController`

Is the GeckoLib bridge called by three `CompanionEntity` predicates:

```text
companion_body       -> primary state/action clip
companion_face       -> face_idle / alert / talk / combat / power / recovery loop
companion_secondary  -> hair/clothing idle / walk / run / combat / power / recovery loop
```

The controllers own non-overlapping bone sets, so a body action does not freeze blink, local gaze, jaw-talk, or secondary hair/clothing motion. They request `thenLoop` or `thenPlay` from synchronized state only and never touch a target, entity health, inventory, block, plan, memory, or safety policy.

### `AnimationStateMapper`

Is the stable façade for integrations. It delegates to `CompanionAnimationStateMapper` and exposes no mutable state.

### `CompanionAnimationHandler`

Exposes marker timing plans to local VFX/audio and QA code. Its `mayApplyGameplayFromMarker()` contract is permanently `false`; marker handling cannot mutate inventory, blocks, plans, memory, AI state, or damage.

### `AnimationMarkerPlan`

Contains source marker timing metadata for actions such as validated Push, Rescue, Hive release, route point, recovery, retreat signal, and melee visual sync. It documents that the server owns the effect result before the marker presentation.

### Save/load behavior

No raw GeckoLib playback cache is persisted. On reload, existing save recovery restores a safe AI state; the mapper derives a safe current visual clip from that state. An interrupted focus or stale task never becomes a successful effect because animation data is not authoritative.

---

## 9. Performance budget

| Budget | Source rule |
|---|---|
| Geometry | Original faceted character layers use the shared 64×64 texture/Geo contract: tapered head, independent face bones, hair locks, and articulated clothing. No external mesh runtime or unbounded detail path is permitted. |
| Animation evaluation | One mapped body controller per active companion. |
| Active companions | Safe source limit remains two. |
| Distant presentation | Above 32 blocks, optional VFX/trails must be reduced or omitted; no gameplay change occurs. |
| Particles | Maximum 50 per cast; lower caps in Low Effects. |
| Per-tick work | Animation mapper reads existing state only; it does not scan blocks, entities, chunks, or inventories. |
| Cache | No custom unbounded animation cache; GeckoLib cache runtime behavior still requires test evidence. |
| Benchmark | FPS/TPS/MSPT/Spark claims remain deferred until real runtime profiling. |

A future runtime target may use a 10% animation-related FPS regression as a review threshold, but no pass/fail claim is valid until a baseline hardware profile exists.

---

## 10. QA checklist

### Static source checks

- [ ] Every four Geo files has the shared canonical rig marker and all required bones.
- [ ] All animation JSON parses.
- [ ] Mapper-selected clip keys exist in the appropriate role file.
- [ ] Marker definitions contain only allowed semantic marker names.
- [ ] No marker contract includes inventory, block, plan, memory, arbitrary damage, or AI mutation.
- [ ] All authored text is English-only.
- [ ] No actor voice, show music, show image, or extracted show model is introduced.

### Future runtime checks

- [ ] No floor, limb, accessory, weapon, or backpack clipping from front, side, rear, and low camera angles.
- [ ] Idle/walk/run/combat/downed/recovery transitions do not pop visually.
- [ ] Cancel before a planned marker produces no successful effect or success VFX.
- [ ] Server rejection does not play a success clip.
- [ ] Low Effects preserves readable icon/shape/HUD backup.
- [ ] No clip hides the player crosshair or combat target for an unreasonable duration.
- [ ] Downed remains readable without particles.
- [ ] No camera lock, input lock, forced screen shake, or cutscene occurs.
- [ ] Sound category and visual fallback remain understandable under audio reduction.
- [ ] Save/load returns to a safe derived visual state.
- [ ] No persistent animation cache memory growth appears in a long session.
- [ ] Spark/TPS/MSPT/FPS evidence is recorded before a performance claim.

---

## Non-claims

This source milestone does not claim that new clips have been viewed inside Minecraft, blended by GeckoLib, benchmarked, paired with finished original sound assets, or validated in Blockbench on every camera angle. It provides the rig, clip, mapping, marker, VFX, sound, and QA source contracts needed for that future runtime gate.
