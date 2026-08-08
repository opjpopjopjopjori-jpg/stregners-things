# Consequence, Perception, Interrupt, Spatial, and Micro-Scene Contract

## Consequence without punishment

`ConsequenceService` records only visible, bounded decision effects. Current source hooks include player annotations, approved/deferred plans, and selected base anchors. A consequence can create a tactical reference, a memory, a small transparent relationship clarity event, a world annotation meaning, or an optional story/intention hook.

It must not:

- silently lower trust because a suggestion was ignored;
- lock core content behind an unclear choice;
- kill or permanently remove a companion without a rescue path;
- grant destructive power, loot, chest access, or hidden punishment.

Each record uses a stable key, so a reload cannot write the same milestone consequence twice.

## Player identity layer

`PlayerIdentityState` stores only opt-in identity data:

```text
approved nickname (2–16 safe English characters)
nickname enabled flag
play style: explorer / cautious / builder / fighter / protector / unset
accepted doctrines and milestones remain separate team facts
```

Alerts continue to use the actual player name. A nickname is reserved for optional calm presentation and can be cleared. It never changes mechanics, permissions, power, trust, or safety.

Commands:

```mcfunction
/companions identity nickname "Trail Guide"
/companions identity clear_nickname
/companions identity style explorer
/companions identity status
```

## Trust is not obedience

Trust remains a source of clearer memories, reflections, and cooperative plan presentation. It is not permission to bypass role limits or Safety Gates. A command still passes role, policy, target, range, energy, strain, cooldown, rescue, navigation, and Safe Mode checks.

A clear refusal is preferable to a hidden penalty or an unsafe action. The Team Journal, command feedback, and diagnostics expose reason codes rather than pretending a companion ignored the player without explanation.

## Perception and attention

`CompanionPerceptionService` performs a bounded local sight check at a configurable server interval. It records expiring `PerceptionSignal` entries with:

```text
observer role
source: vision / hearing / memory / team report / tag
confidence: low / medium / high
actionability: observe / caution / alert / plan support
timestamp and expiry
```

Current runtime source implements vision and visible Seer Hive-tag evidence. It does not see through walls, force-load chunks, infer precise sound coordinates, or treat a teammate report as direct sight. Advisory threat profiles may add observed-tell vocabulary but cannot remove conservative Unknown/Caution behavior.

## Interrupt contract

`ActionInterruptService` consumes Team Event urgency before ordinary reactions:

```text
P0 / P1: cancel Scout and low-value base/observe work; preserve a valid rescue
P2: player command / plan change owns its normal cleanup
P3 / P4: no safety work is interrupted for discovery or ambient content
```

P0 can safely cancel a Hive focus when immediate player fall/health danger makes continuing unsafe. Interrupted work returns through existing state cleanup; no cooldown, energy, reservation, or target result is guessed complete.

## Spatial etiquette

A real player block interaction creates a short local interaction-radius reservation. Formation and navigation candidates yield that radius without inspecting the block inventory. Combined with interaction-block path rejection and formation slot reservations, companions avoid standing on or crowding:

```text
chests, beds, pressure plates, crafting tables,
furnaces, enchanting tables, doors, fence gates
```

No companion opens a container, uses a door, or edits a block to make spatial etiquette work.

## Micro-scenes and dynamic silence

`SetPieceService` supports bounded in-world moments:

```text
CAMPFIRE_CHECK
THRESHOLD_MOMENT
RETURN_HOME
SILENT_WALK
LANDMARK_MEMORY
```

They are a single safe-time line or a quiet window, not a cutscene. They never lock input, move the camera, freeze time, force player movement, or start while hostiles are close. Downed/failure events can begin a quiet window so ambient jokes do not undercut recovery.

Current dialogue triggers are English-only and limited. Large mysteries, boss scenes, cinematic mode, photo album, advanced audio mixing, and long personal arcs remain deferred pending runtime playtests.

## Required runtime tests

- Annotation/plan/base consequence is visible and non-punitive after reload.
- Invalid nickname is rejected; player name remains safe default.
- Zombie behind a wall without visible/tag evidence produces no false vision signal.
- Visible Hive tag produces a Seer tag signal only with line-of-sight.
- P0 danger interrupts Scout but does not cancel an active player rescue.
- Player right-clicks a chest: companions yield the interaction radius without container access.
- Threshold/return/landmark lines never appear during combat and never change camera or input.
