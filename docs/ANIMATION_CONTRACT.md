# Animation and VFX Contract

> The full rig, clip, marker, VFX, sound, state-machine, and QA specification is in `docs/ANIMATION_DIRECTOR_SYSTEM.md`.

## Authority

- The logical server owns state, safety, cooldown, target validation, damage, inventory, teleport decisions, plans, and recovery.
- GeckoLib and the client render only models, poses, keyframes, particles, and sound cues.
- Animation markers must never place blocks, change inventory, damage arbitrary entities, write memory, or complete an ability.
- The server-derived `CompanionVisualState` is the presentation vocabulary; animation is never an AI state machine.

## Presentation state vocabulary

```text
IDLE_CALM
FOLLOW
GUARD
OBSERVE
ALERT
COMBAT_MELEE
COMBAT_RANGED
RETREAT
DOWNED
RECOVERY
POWER_FOCUS
POINT_ROUTE
INTERACT_ANCHOR
```

Urgency order is intentional:

1. `DOWNED` interrupts every other presentation.
2. Rescue/power/alert cues interrupt idle/emote behavior.
3. Long cosmetic movement may not block combat navigation.
4. A player-facing cue always has HUD/chat/state fallback when it matters to gameplay.

## Companion animation families

| Role | Key animations | Body-language goal |
|---|---|---|
| Seer | anomaly notice, Hive focus/release/recovery | cautious evidence direction, not universal radar |
| Guardian | guard stance, protect, retreat signal | a practical defensive line and perimeter watch |
| Gifted | power notice/focus, push, shield, rescue pull, exhausted recovery | clear short focus with no long movement lock |
| Scout | route point, lookout, ranged-ready, grounding call | optional route/height marker rather than constant chat |

Each companion additionally has shared idle-alert, crouched walk, run, turn, melee, hit reaction, dodge, interaction, stuck, spawn/dismiss presentation, emotion, calm, downed/recovery, and social/world clip contracts. The social family includes campfire, weather, horizon, base, work, cave, village, travel, calm, listen, point, reassurance, gear-check, and observation direction. Not every future capability clip is gameplay-enabled; the source mapper uses only server-derived states and safe fallbacks.

## Performance and accessibility

- Do not use unsuitable high-poly models for Minecraft.
- Do not run complex keyframe work or high-rate particles for distant/non-visible companions.
- Particle counts are capped per power and reduced by Low Effects; Low Effects must preserve state clarity.
- No forced camera movement, input lock, time stop, or forced screen shake.
- No visual success cue may play after the server safety gate rejected the action.
- Important sound cues need a HUD/subtitle/text fallback.

## Runtime verification status

The source mapping and JSON resources are statically checked. Actual FPS, camera obstruction, animation blending, missing-resource handling, and audio mix are runtime pending; use `docs/CHAOS_AND_ACCEPTANCE_TESTS.md` after the build workspace exists.
