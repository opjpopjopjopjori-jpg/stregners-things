# Will Control-Heart and Gifted Precision Power Contract

## Purpose

This contract focuses on the two visually sensitive companion roles:

```text
Will / Seer: bounded control of visible hostile mobs through a control-heart budget
Eleven / Gifted: focused Push, Shield, and Rescue presentation
```

The source uses server-owned Safety Gates, high-detail animation contracts, and original audio/VFX assets. It does not claim runtime visual or balance perfection until a real Minecraft client test passes.

## Will target boundary

Will control is no longer restricted to the custom `hive_linked` tag. A valid target must be:

```text
A loaded Mob that implements Minecraft's hostile Enemy interface
Alive, visible to the Seer, within the bounded control range, and actively threatening the owner/owned companion (or a nearby recognized boss pressure)
Not protected by riftcompanions:protected_from_companions
Not a player, companion, villager, animal, or tameable entity
Not rejected by Safe Mode, policy, cooldown, energy, strain, protected-area, or Surge gates
```

Consequently, normal hostile Minecraft mobs such as zombies, skeletons, spiders, creepers, Endermen, and supported hostile mod mobs can enter the same temporary control pipeline when they are an actual nearby threat. The base mod adds no custom hostile entity or enemy spawn system.

This is not permanent monster ownership or passive mob-farm control. Will never creates pets, changes loot, orders an entity to farm, controls players, controls villagers, or controls animals.

## Control-heart budget

The server makes selection deterministic and visible rather than using a hidden success roll.

```text
Default Will control budget: 32 control hearts
Default Swarm Freeze absolute cap: 6 targets
Default Surge bonus: +25% control-heart capacity
Default boss/overload duration ceiling: 25% of the requested mode duration
```

All values are configurable through the English common configuration. `hive_swarm_target_cap` remains a legacy compatibility ceiling; the effective Swarm cap is the lower of that value and `will_control_target_cap`. The older `hive_surge_min_linked_targets` setting remains as a compatibility lower bound; the effective Surge crowd threshold is the higher of it and `will_surge_min_hostile_targets`.

### Target load

A target consumes an effective control-heart load:

```text
ceil(target maximum health / 2)
+ bounded attack-pressure adjustment
+ bounded armor-pressure adjustment
+ 50% pressure for a PARTIAL boss-resistance target
```

Maximum health, not current missing health, is the primary component. A player therefore cannot make a powerful target easy simply by damaging it first.

### Selection and resistance

- Candidates are visible, loaded hostile mobs actively targeting the owner or an owned companion in a 12-block bounded scan.
- Candidates are sorted nearest-first, so a crowded encounter never selects random hidden targets.
- `Suspend`, `Redirect`, `Shatter`, and `Disrupt` choose one eligible target and still use its heart load for duration/cost.
- `Swarm Freeze` chooses the nearest targets that fit the heart budget and target cap.
- More selected targets or stronger targets increase Energy and Strain cost and shorten the control duration.
- A first target too strong for the available budget is not silently ignored: it receives a short resisted `Stagger` opening instead of full control.
- Standard vanilla bosses (`Wither`, `Ender Dragon`) are tagged as `PARTIAL`; they receive only short Stagger control. Extremely high-health threats naturally overflow the heart budget and use the same short path.
- An explicit compatibility adapter can return `IMMUNE` for an externally unsafe entity. It is rejected safely rather than risking a broken boss phase.

The owner HUD receives display-only target count and `Load X/Y hearts (Z%)`. The client cannot alter target selection, the budget, cost, duration, or effect.

## Precision channel sequence

```text
Player command
-> server finds visible eligible hostile targets
-> server calculates control-heart load and cap
-> Focus visual action + original focus cue
-> selected target remains valid until release time
-> server revalidates target, budget, policy, and boss resistance
-> Energy/Strain transaction resolves
-> mode-specific release visual and cue
-> temporary target control state begins
-> recovery or safe cancellation
```

A target death, lost sight, out-of-range state, protected state, Safe Mode, policy change, interruption, or resistance change cancels before the full release cost and before success release presentation.

## Suspend lift

`HiveControlManager` records a bounded lift ceiling for `SUSPEND` and `SWARM_FREEZE`.

During a full-control window it:

- disables target AI only after validation;
- keeps the target in loaded space;
- checks collision before each small upward move;
- raises `SUSPEND` up to 1.15 blocks and `SWARM_FREEZE` up to 0.45 blocks;
- does not teleport the target;
- does not load chunks;
- resets fall distance;
- restores original no-AI/no-gravity state on release.

A resisted `Stagger` does not disable a boss's AI or gravity and never lifts it.

## Redirect

Redirect only moves a fully validated target away from the Seer in small collision-safe steps. It rejects unloaded, fluid, and collision positions and never becomes automatic transport.

Visual action:

```text
SEER_RELEASE_REDIRECT
animation.seer.hive_release_redirect
Marker: APPLY_POWER_EFFECT at 0.30 seconds
Cue: seer_release
```

## Non-graphic structural shatter

Shatter does not depict gore, broken bones, execution, or permanent damage. It is an original enemy-disruption treatment:

```text
SEER_RELEASE_SHATTER
animation.seer.hive_release_shatter
Marker: APPLY_POWER_EFFECT at 0.34 seconds
Cue: seer_shatter
Effects: bounded Weakness + Slowness + original crack/fragment VFX
```

It produces no extra loot, farm automation, item change, or arbitrary damage. A resistant or overloaded target receives `Stagger` rather than full Shatter control.

## Gifted precision actions

### Focus and Push

```text
GIFTED_NOTICE -> GIFTED_FOCUS -> GIFTED_PUSH
animation.gifted.power_notice
animation.gifted.power_focus
animation.gifted.push_release
Marker: APPLY_POWER_EFFECT at 0.12 seconds
Cue: gifted_notice / gifted_focus / gifted_push
```

Push requires existing server checks for visible combat context, protected bystanders, energy, cooldown, policy, and sensitive areas. A rejected Push produces no confirmed release cue.

### Shield

```text
GIFTED_SHIELD
animation.gifted.shield_hold
Cue: gifted_shield
```

Shield is owner-only, range checked, temporary, interruptible, and not invulnerability. The sound service uses a long action cooldown to prevent loop spam.

### Rescue

```text
GIFTED_RESCUE
animation.gifted.rescue_pull
Marker: APPLY_RESCUE at 0.24 seconds
Cue: gifted_rescue
```

The safe destination is resolved server-side before the presentation cue. The animation never pulls an arbitrary entity or creates a teleport exploit.

## Original audio and VFX

| Action | Animation | Cue | Visual fallback |
|---|---|---|---|
| Will focus | `hive_focus` | `seer_focus` | Will Link HUD + focus pose |
| Will suspend | `hive_release_suspend` | `seer_release` | target lift + particle shape |
| Will redirect | `hive_release_redirect` | `seer_release` | target motion + HUD state |
| Will shatter | `hive_release_shatter` | `seer_shatter` | non-graphic fragment shape + target state |
| Gifted push | `push_release` | `gifted_push` | outward arc + HUD/state |
| Gifted shield | `shield_hold` | `gifted_shield` | ring outline + HUD/state |
| Gifted rescue | `rescue_pull` | `gifted_rescue` | directional line + HUD/state |

`companion_audio_enabled` can disable presentation cues without changing gameplay. Low Effects reduces particle counts but retains animation, HUD, and state readability.

## Required runtime acceptance tests

1. Normal vanilla zombie, skeleton, creeper, spider, Enderman, and custom hostile mob can begin a valid Will focus when actively hostile, visible, and in range.
2. Players, pets, villagers, animals, companions, and `protected_from_companions` types remain rejected.
3. A low-health group fits only the available Swarm heart budget and target cap; HUD shows the same count/load chosen by the server.
4. A high-health hostile that exceeds the budget receives only a short Stagger window.
5. Wither and Ender Dragon use partial resistance: no lift, no redirect, no full AI suppression, and no kill/loot bypass.
6. Target death, range loss, protected-state change, or adapter resistance change before release costs no full power and plays no success release.
7. Suspend lift stays collision-safe, loaded, bounded, and non-teleporting.
8. Release restores target gravity/AI state and cannot cause a fall-damage exploit.
9. Shatter uses non-graphic original cue/VFX and gives no loot/damage farm advantage.
10. Gifted Push rejects near protected bystanders before cost/release cue; Shield and Rescue remain owner/safe-destination limited.
11. Audio disabled, Low Effects, muted master volume, and HUD fallback all remain readable.

## Non-claims

No real client animation, lift trajectory, sound mix, particle visibility, boss compatibility, combat balance, or performance result is claimed until `runClient` and the written runtime tests complete.
