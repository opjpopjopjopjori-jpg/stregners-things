# Navigation and Stuck Recovery Contract

## Goal

Minecraft's vanilla pathfinder remains the low-level path search, but it is no longer trusted as the entire companion movement policy. `CompanionNavigationService` is a server-authoritative safety layer around it. The client renders state only and never chooses a route, a recovery teleport, or a formation fallback.

This source implementation is designed to make a safe cancellation preferable to a reckless movement attempt. It does not promise flawless NPC boat, ladder, door, portal, or cross-dimension intelligence.

## Layered navigation pipeline

```text
AI intent
  -> bounded target candidates
  -> local safety/annotation/reservation checks
  -> vanilla PathNavigation path creation
  -> full bounded path-node footprint validation
  -> rate-limited move request
  -> progress monitor
  -> alternate candidate / Stuck Recovery / validated Safe Recall fallback
```

### 1. Intent ownership

`NavigationIntent` identifies why a path exists:

```text
FOLLOW, FORMATION, GUARD, RETREAT,
RETURN_HOME, BASE_ACTIVITY, SCOUT, STUCK_RECOVERY
```

AI state decides intent. Animation, HUD, dialogue, and client input do not.

### 2. Bounded candidates

The requested goal plus a limited ordered ring of nearby alternatives is considered only in already loaded chunks. This solves common cases where the player, a formation slot, or a target block is technically unreachable but a safe square one block away is valid.

No candidate scan loads chunks, reads a private container, edits a block, opens a door, or chooses a location outside the configured bounded set.

### 3. Path safety validation

A path is accepted only when its bounded node count is within the configured budget and every node passes local checks:

- loaded chunk;
- no player annotation marked as danger/protected/machine no-go;
- no fluid at floor/feet/head;
- collision space for companion feet and head;
- no leaves, doors, ladders, vines, cobwebs, fence gates, hazards, berry bushes, or powder snow traversal;
- no downward node step above the configured safe-drop threshold.

Stairs and normal partial solid surfaces are deliberately allowed when collision space is valid. Companions do not autonomously open doors or attempt ladder/vine intelligence; they route around them or safely stop.

### 4. Formation reservations

`FormationSlotReservationService` keeps a short server-only claim for each companion slot. A candidate too close to another companion's live reservation is rejected. This prevents two companions from repeatedly selecting one "safe" square and pushing each other into a loop.

Reservations expire, release on formation clearing, and clear on session/Safe Mode cleanup. They are not a world ownership claim.

### 5. Progress monitor and recovery ladder

The progress monitor is intentionally cheap: it compares block position and distance at a configured cadence instead of scanning world blocks every tick.

```text
Path active
  -> no progress timeout
  -> bounded alternate candidate replan
  -> configured retry budget exhausted
  -> CompanionState.STUCK_RECOVERY
  -> route to last safe waypoint
  -> validated Safe Recall, or HOLD when recall policy forbids it
```

No movement recovery breaks blocks, opens doors, places bridges, uses TNT, enters a portal, loads chunks, or teleports across dimensions.

## Configuration

The common `navigation` section exposes:

```text
custom_navigation_enabled
repath_interval_ticks
stuck_timeout_ticks
max_recovery_attempts
max_safe_drop
max_candidates
max_path_nodes
max_path_attempts
target_shift_repath_distance
slot_reservation_radius
```

These are bounded safety/performance controls. Disabling `custom_navigation_enabled` enables only the legacy path route for isolation testing; it is not a gameplay recommendation.

## Performance budget

- Normal replan requests are rate-limited.
- Candidate count and vanilla path-node count are capped.
- Every accepted path is locally validated; no global block scan occurs.
- Progress monitoring is arithmetic state tracking, not path rebuilding each tick.
- Formation assignments run at the Team Director cadence and use expiring reservations.
- Navigation state clears on logout/Safe Mode and when a companion is discarded/rested.

## Required runtime matrix

Runtime validation remains pending real Java 17 + Forge 47.x world tests. Execute at minimum:

| Scenario | Expected safe result |
|---|---|
| Closed door | Route around or hold/recover; no autonomous door use |
| Staircase | Valid stair footprint can be used without repeated replan loop |
| Ladder/vine | Route around or stop; no fake climbing intelligence |
| Narrow cave | CAVE formation reduces slot collision; reservations avoid pileup |
| Leaves/cobweb | Candidate/path rejected before loop; recovery stays bounded |
| Cliff / ravine | Unsafe downward path node rejected; no deliberate large drop |
| Water / lava | No unsafe fluid route; Safe Recall only after normal validation |
| Two-companion follow | Distinct reserved formation slots, no player camera center pileup |
| Destroyed waypoint | Target invalidates to safe stop/recovery, never block placement |
| Chunk unload | No forced load; plan/task cancels or holds safely |

Do not claim stable navigation until this matrix has run in an actual Forge game with diagnostics and performance capture.
