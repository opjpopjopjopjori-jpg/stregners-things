# Asset Registry and Definition of Done

## Ownership and publication boundary

Personal presentation assets are private alternate-continuity companion material for the requested offline project. Their late-1980s final-arc mood is original visual direction only. Public-mode assets use original generic role names and presentation. No actor likeness, actor voice imitation, exact costume, show dialogue, soundtrack, logo, or copied episode material is included.

## Companion assets

| Asset ID | Association | Source / license note | States | Tier | Fallback |
|---|---|---|---|---|---|
| `personal/will_seer` | Seer | Project-generated private presentation | observe, Hive focus, recovery | 512×512 RGBA | `public/seer_public` |
| `personal/hopper_sheriff` | Guardian | Project-generated private presentation | guard, retreat, recovery | 512×512 RGBA | `public/guardian_public` |
| `personal/eleven_gifted` | Gifted | Project-generated private presentation | focus, shield, rescue, exhausted | 512×512 RGBA | `public/gifted_public` |
| `personal/max_scout` | Scout | Project-generated private presentation | route point, lookout, grounding | 512×512 RGBA | `public/scout_public` |
| `public/seer_public` | Seer | Original generic public role presentation | core visual states | 512×512 RGBA | role-color HUD |
| `public/guardian_public` | Guardian | Original generic public role presentation | core visual states | 512×512 RGBA | role-color HUD |
| `public/gifted_public` | Gifted | Original generic public role presentation | core visual states | 512×512 RGBA | role-color HUD |
| `public/scout_public` | Scout | Original generic public role presentation | core visual states | 512×512 RGBA | role-color HUD |
| `faceted_character_512_v2` | All companions | Original faceted visual extension over `shared_humanoid_v2` | tapered head, eyes, lids, brows, jaw, mouth, layered hair, articulated clothing | 512px Geo JSON / Blockbench | canonical backbone and safe idle fallback |
| `social_world_clip_family` | All companions | Original authored animation expansion | campfire, weather, horizon, base, work, cave, village, travel, calm, listen, point, reassurance, gear, observe | GeckoLib JSON | text/state fallback and urgent interruption |
| `core_animation_timeline` | All companions | Original project marker/VFX/sound metadata | server-confirmed visual timing only | JSON contract | HUD/chat/state fallback |

## Seer control audio assets

| Asset ID | Association | Source / license note | States | Tier | Fallback |
|---|---|---|---|---|---|
| `hive/notice` | Seer control cue | Original synthesized OGG | notice | low-cost cue | Will Link HUD text |
| `hive/focus` | Seer control cue | Original synthesized OGG | focus | low-cost cue | Will Link HUD text |
| `hive/release` | Seer control cue | Original synthesized OGG | release | low-cost cue | Will Link HUD text |
| `hive/resist` | Seer control cue | Original synthesized OGG | resistance | low-cost cue | Will Link HUD text |
| `hive/recovery` | Seer control cue | Original synthesized OGG | cancellation/recovery | low-cost cue | Will Link HUD text |
| `companions/*` | Companion action cues | Original deterministic synthesized OGG | guard, melee, focus, power, rescue, route, anchor | server-confirmed presentation cue | HUD/chat/animation fallback |
| `ui/*` | Plan and Safe Mode cues | Original deterministic synthesized OGG | confirmed plan / Safe Mode | low-volume UI cue | readable feedback card |
| `ambience/*` | Safe-base mix candidates | Original deterministic synthesized OGG | guardian/seer/gifted/scout ambience | packaged but not auto-looped | no gameplay dependence |

See `docs/ORIGINAL_AUDIO_CUE_LIBRARY.md` for the full asset list and runtime mix boundary.

## Runtime acceptance gate

Each model/texture/animation/sound remains runtime unverified until it has been checked in the future Forge test scene for missing textures, hitbox/nav interference, FPS impact, Low Effects behavior, and readable fallback cue. Asset registry status is documentation, not a completed benchmark.
