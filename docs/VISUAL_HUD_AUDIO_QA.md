# Visual Identity, HUD, Audio, Persona, and Asset QA

> See `docs/ANIMATION_DIRECTOR_SYSTEM.md` for the shared rig, clip family, marker, VFX, sound, transition, and animation QA source contract.

## Presentation authority

`CompanionVisualState` derives presentation from authoritative AI state and visual action. The animation layer cannot apply damage, move an entity, spend energy, complete a rescue, write memory, or place a block.

The source visual-state vocabulary is:

```text
IDLE_CALM, FOLLOW, GUARD, OBSERVE, ALERT,
COMBAT_MELEE, COMBAT_RANGED, RETREAT, DOWNED,
RECOVERY, POWER_FOCUS, POINT_ROUTE, INTERACT_ANCHOR
```

Downed and urgent action states interrupt ordinary idle presentation. No forced camera movement, input lock, time stop, or screen shake is implemented.

## Role readability

- **Guardian:** a wider Guard posture and retreat signal communicate a defensive line, not a damage spectacle.
- **Seer:** notice/focus is tied to visible evidence and server-validated hostile targets, never general radar.
- **Gifted:** focus, shield, push, rescue, and exhaustion poses are short enough not to lock movement logic.
- **Scout:** route point, lookout, grounding, and Social Director clips communicate an optional route or visible local context, never off-screen chunk scouting.
- **Social pair direction:** campfire, weather, horizon, base, work, cave, village, travel, and calm exchanges use authored lead/reply text plus interruptible display-only clips; no player input or gameplay effect is attached.

Every important visual cue still needs HUD/chat/state backup. Low Effects changes particle count only; it must not change safety logic or remove the readable state.

## HUD under pressure

The compact HUD renders active/downed companions only, with danger/plan context, a role code, health-state color, short state, and energy. The Team Journal exposes details, reasons, safe-mode status, duo context, optional intention state, per-owner policy overrides, memories, and diagnostics.

Safe Mode is visibly announced in both HUD and Journal. A client cannot clear it silently; the server validates the clear command. Personal policy cards also send only a fixed role request; the logical server chooses the next valid policy and still applies every Safety Gate.

## Audio policy

The packaged milestone contains original synthetic Hive cues only: notice, focus, release, resist, and recovery. There is no actor imitation, show audio, licensed soundtrack, or full voice acting.

Future alert, plan, route, and power cues must remain original, short, subtitle-backed when important, independently volume-controlled, and less frequent than high-priority alerts. `AUDIO_TEST_01` remains runtime pending.

## Persona review board

Before accepting dialogue, a state cue, or a power change, review:

### Guardian

- Does it communicate practical protection rather than power display?
- Does it preserve the player’s choice outside a lethal emergency?
- Does it avoid aggression toward the team?

### Seer

- Does it state evidence or uncertainty clearly?
- Does it avoid a cheat-radar promise?
- Does it treat observation as useful rather than helplessness?

### Gifted

- Does the power assist rather than solve every encounter?
- Does it preserve personal choice rather than treat the role as a battery?
- Does the wording avoid constant childishness or naivety?

### Scout

- Is an independent route a practical proposal rather than disobedience?
- Is humor rare, safe, and context-appropriate?
- Is the role more than a speed or ranged class?

## Asset definition of done

See `docs/ASSET_REGISTRY.md`. An asset is not accepted merely because a file exists. It needs correct identifiers, source/license record, fallback cue, model/animation state linkage, no missing texture, safe hitbox/navigation behavior, and a real runtime performance check.
