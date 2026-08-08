# Faceted Character Rig and Expression Contract

## Purpose

The four companion models use the original `faceted_character_512_v2` presentation extension over the stable `shared_humanoid_v2` animation backbone. It replaces the earlier one-block head and flat-skin approach with a bounded cuboid-only faceted character rig compatible with GeckoLib.

## Model composition

Every role contains:

- A tapered multi-plane head: core, forehead, cheeks, chin, ears, nose, and jaw.
- Separate visual components for eye whites, iris/pupil, glint, upper/lower lids, brows, and mouth.
- Segmented upper/lower arm, hand, upper/lower leg, and foot geometry.
- Role-specific layered hair bones and ordinary grounded clothing layers.
- Existing canonical root/body/chest/head/jaw/arm/leg/accessory bones needed by current clips and state mapping.

The model remains faceted rather than using an external smooth mesh runtime. This is an intentional compatible approximation within the Minecraft/GeckoLib renderer, not an imported third-party model format.

## Layered controller architecture

Three GeckoLib controllers own non-overlapping bone sets:

| Controller | Bone set | Responsibility |
|---|---|---|
| `companion_body` | Canonical body/action bones | Locomotion, combat, interaction, recovery, and power pose |
| `companion_face` | Eyes, lids, brows, jaw, mouth | Blink, local gaze, talk, alert, combat, power, and recovery expression |
| `companion_secondary` | Role hair locks and clothing tails/collars | Idle, walk, run, combat, power, and recovery secondary motion |

This prevents a one-shot body action from freezing facial expression or hair movement. Controllers use only synchronized companion state/action facts and never make a gameplay decision.

## Facial presentation

- Blink is authored by moving upper/lower lid bones in the independent face loop.
- Pupil drift is local authored presentation only. It does not target players, read entity state, search terrain, or expose new perception.
- Brows provide alert, determined, combat, fear, and fatigue emphasis.
- Jaw motion is attached only to presentation talk clips. Dialogue delivery, chat, and gameplay remain independent.
- No facial bone controls collision, hitbox, damage, power, AI, camera, or player input.

## Hair presentation

Hair is split into role-specific crown/back/side/front locks. Idle, walk, and run clips apply small authored secondary rotations. This is animation, not a physics simulation; it has no collision, block interaction, wind scan, entity scan, or runtime dependency.

## Role movement direction

| Role | Idle / locomotion direction | Combat direction |
|---|---|---|
| Seer | Restrained shoulders, measured stride, modest hair motion | Guarded focus and compact motion |
| Guardian | Heavier body bob, broad planted stride | Weighty protective stance and deliberate strikes |
| Gifted | Centered posture and controlled steps | Precise, contained upper-body intent |
| Scout | Quick athletic cadence and greater hair motion | Fast lean, side-dodge emphasis, agile strikes |

## Validation requirements

1. Every animation bone reference must exist in the matching Geo model.
2. All Geo parents must resolve without a cycle.
3. UV rectangles must remain inside the 512×512 atlas.
4. At least the normal idle clip must drive authored blink, gaze, and brow components.
5. Walk and run must contain secondary role hair motion.
6. Actual Minecraft review remains required for culling, close-range face readability, eye/lid alignment, clipping, blend quality, and FPS/VRAM cost.
