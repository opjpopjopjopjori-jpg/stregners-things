# Safe Recall Placement and Anti-Stacking Contract

## Problem addressed

A burst Safe Recall, fresh multi-role spawn, save-recovery relocation, or Stuck Recovery can previously place more than one companion at the same block center when each request independently chooses the first block-safe square near the player.

## Required outcome

Two companions must never intentionally occupy the same destination AABB or centered block position after a valid recall/spawn burst. If local terrain cannot safely support every requested companion, a later request must fail or hold safely rather than stack entities, load terrain, move a player, or bypass hazards.

## Server-only algorithm

`SafeTeleport` now applies all of the following before a companion recall or first spawn:

1. Search only loaded local candidates in the existing bounded radius and vertical range.
2. Validate sturdy floor, head clearance, no fluid, no listed hazard, and collision-safe entity bounding box.
3. Reject a candidate whose destination AABB intersects another alive non-spectator living entity.
4. Reject a candidate claimed by a different companion of the same owner within the short separation radius.
5. Claim the selected destination before the teleport/spawn is committed.
6. Keep the claim for only 60 ticks, then expire it automatically.
7. Clear owner claims at logout; release an individual claim if a lifecycle action discards the companion before expiry.
8. Clear a recalled companion's stale formation slot so Team Director recomputes a distinct local formation destination.

Claims are server-only, non-persistent, bounded to four per owner, and never transmitted to the client. They contain no HUD coordinate, inventory, path, or cross-dimension state.

## Scope boundary

- No chunk loading.
- No automatic portal or cross-dimension travel.
- No block placement, block breaking, teleporting a player, or forcing an entity through a collision.
- No replacement entity if an active companion is unloaded.
- No guarantee that a recall succeeds in a one-square corridor or unsafe local terrain; safe rejection is correct behavior.

## Runtime matrix

| Case | Expected result |
|---|---|
| Recall All in open terrain | Every active companion uses a distinct nearby safe square. |
| Fresh second-role spawn | Spawn reservation avoids a first companion's chosen square. |
| Two Stuck Recoveries in one tick | Each uses a separate claim or the later one holds safely. |
| One-block corridor | One companion may receive a slot; another rejects/holds, never overlaps. |
| Existing villager/player/companion on candidate | Candidate rejects because living-entity clearance fails. |
| Safe Recall then immediate formation tick | Recalled companion clears stale slot and receives a fresh non-reserved formation slot. |
| Logout during claim lifetime | Owner claims clear immediately and do not persist. |
| Save/load | Claims are intentionally not saved; normal loaded-entity and Safe Recall safety checks run again. |

This contract is static/build validated. The matrix remains pending real integrated-client runtime evidence.
