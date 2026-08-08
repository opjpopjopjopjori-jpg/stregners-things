# Animation System Review Audit

## Scope

This is a static source review of the shared rig, expanded clip assets, mapper, marker metadata, VFX/sound contract, and presentation safety boundaries. It is not a claim that GeckoLib playback, Blockbench export, or runtime FPS has been tested.

## Reviewed invariants

| Invariant | Static result | Evidence |
|---|---|---|
| AI is the animation source | Pass | `CompanionAnimationStateMapper` reads `CompanionState`, `CompanionVisualState`, `CompanionAction`, and movement only. |
| Animation cannot apply gameplay | Pass | `CompanionAnimationHandler.mayApplyGameplayFromMarker()` is false; marker plans are metadata only. |
| Server Safety Gate owns effects | Pass | `AbilityService`, `HiveChannelManager`, `MindAnchorService`, and melee logic resolve server state before success presentation. |
| No block or inventory marker | Pass | Timeline validation rejects forbidden marker names. |
| Downed interrupts all | Pass | Mapper resolves DOWNED before visual action selection. |
| Stuck has a bounded visual fallback | Pass | `STUCK_RECOVERY` maps to `stuck_struggle`; actual Safe Recall/hold remains server-owned. |
| Low Effects remains meaningful | Pass in contract | Every new VFX profile has lower cap plus shape/icon/HUD/chat fallback. Runtime visibility is pending. |
| Camera/input lock absent | Pass in source contract | No animation class mutates camera or player input. |
| Each role reads differently | Pass in asset contract | Guardian guard/weight, Seer restraint/focus, Gifted focused force, Scout route/ranged motion have separate clips. |
| Social world clips remain display-only | Pass in source contract | `CompanionSocialDirector` uses visible local cues and appended `SOCIAL_*` actions; clips cannot alter gameplay. |
| Social clips yield to danger | Pass in mapper | Any `SOCIAL_*` action is suppressed under urgent combat, retreat, downed, recovery, or invalid pair context. |
| Legacy clips survive rig upgrade | Pass in source structure | Bridge arm/leg bones remain parents of canonical bones. Runtime visual verification is pending. |

## Transition conflict review

### Resolved source conflict: low-priority interaction under threat

A conversation or Scout point cue could otherwise remain active briefly after an alert/combat state began. `CompanionAnimationStateMapper` now suppresses these lower-priority actions when the derived state is:

```text
ALERT
COMBAT_MELEE
COMBAT_RANGED
RETREAT
FIGHTING
RETREATING
```

The mapper falls back to the urgent visual state. This preserves player sightlines and prevents a talk/route gesture from masking active danger.

### Intentional precedence

```text
DOWNED
> STUCK_RECOVERY safety presentation
> confirmed power/rescue/strike action
> combat/retreat visual state
> interaction
> locomotion
> ambient
```

A confirmed power/recovery action may still be interrupted by DOWNED. A power focus must cancel if the server no longer has a valid target or policy state.

## Marker review

A client frame cannot be a trusted authority for damage or power. The source therefore uses this safe interpretation:

```text
Server validates and resolves action transaction
-> server requests confirmed visual action
-> marker aligns client VFX/sound and QA timing
```

This differs from a naive client-marker damage design because it prevents a missed, replayed, delayed, or malicious animation callback from changing gameplay.

| Clip | Marker | Time | Cancellation result |
|---|---|---:|---|
| Guardian melee attack 1 | `APPLY_MELEE_DAMAGE` | 0.42s | Damage was already server-confirmed; no delayed second hit. |
| Guardian retreat signal | `PLAY_SOUND` | 0.28s | No plan mutation; visual/sound cue simply stops. |
| Seer Suspend / Redirect / Shatter | `APPLY_POWER_EFFECT` | 0.28 / 0.30 / 0.34s | No success release if target/policy/tag validation fails. |
| Gifted Push | `APPLY_POWER_EFFECT` | 0.12s | No success cue after protected-bystander or safety rejection. |
| Gifted Rescue | `APPLY_RESCUE` | 0.24s | No pull cue after invalid emergency/range/safe-ground result. |
| Scout Route Point | `SPAWN_MARKER` | 0.38s | No route cue creates a chunk load or hidden-world result. |
| Scout Mind Anchor | `APPLY_ANCHOR_EFFECT` | 0.32s | No anchor cue bypasses supported-effect/focus/cooldown rules. |

## Role review

### Guardian

- Stronger anticipation and longer recovery communicate weight without slow-locking movement.
- Guard stance is readable but does not confer a hidden block/invulnerability state.
- Retreat signal is a team readability cue; the plan remains player/server owned.

### Seer

- Notice and resist avoid confident aggressive body language.
- Focus has restrained head direction and clean cancellation.
- Release variants are effortful rather than celebratory.
- No clip turns ordinary Vanilla mobs into an anomaly signal.

### Gifted

- Focus/push/shield/rescue clips are short and intentional.
- No success clip follows a failed power gate.
- Exhaustion is recovery posture, not forced collapse or a trauma resource.

### Scout

- Ranged and route poses remain practical, brief, and interruptible.
- Route point is a local readable marker, never a long-range scout permission.
- Mind Anchor uses a stable call/hold, not a monster-control or teleport presentation.

## Remaining runtime risks to test

1. Geo parent conversion can reveal clipping around hats, backpack, rune bands, satchels, belts, and arms.
2. GeckoLib one-shot reset behavior must be checked under repeated attack/hit events.
3. Actual preferred blend durations require GeckoLib runtime tuning.
4. VFX/sound marker dispatch needs real client integration before it can be called complete.
5. Animation controller cache/FPS behavior must be profiled with one and two active companions.
6. Save/load during one-shot focus/recovery must derive a safe visual state without a stale clip or delayed effect.
7. UI conversation while danger begins must use the newly added action-suppression rule without a visual pop.

## Exact conclusion

The source contract is internally coherent: animation is a server-derived presentation layer, not a gameplay authority. The remaining work is runtime validation and production polish, not permission expansion.
