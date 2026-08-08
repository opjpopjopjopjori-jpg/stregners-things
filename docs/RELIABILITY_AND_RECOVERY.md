# Reliability, Transactions, Migration, and Recovery

## Scope and authority

All state-changing companion logic runs on the logical server, including an integrated single-player server. The client may request a bounded command and render the synchronized result, but it cannot decide damage, item ownership, plan completion, a recall destination, a power hit, or a rescue result.

This source milestone adds a durable per-owner `ActionLedger`, a short-lived `ReservationBook`, versioned data markers, load recovery, and a persisted Safe Mode state. These are source implementations; Forge save/load execution remains **runtime unverified** until real Java 17 / Forge 47.x world tests pass.

## Transaction contract

`ActionTransaction` has an immutable action UUID and the following monotonic phases:

```text
PREPARED -> RESERVED -> APPLIED -> COMMITTED
                  \-> ROLLED_BACK
PREPARED/RESERVED/APPLIED -> EXPIRED or ROLLED_BACK during recovery
```

A service must not report success until its own postcondition is confirmed. The ledger is intentionally bounded to 96 entries per team; terminal entries are discarded before open work, so an unfinished action is never silently evicted.

| Sensitive operation | Source path | Confirmation before commit |
|---|---|---|
| Spawn / Recall / Dismiss | `CompanionLifecycleService` | Entity placement/recall or explicit dismissal state is confirmed |
| Safe-base Rest / Restore | `CompanionLifecycleService` + roster snapshot | Resting state is saved before discard; it can restore only at the original-dimension safe base |
| Manual give / withdraw | `CompanionInventoryService` | Source/inventory transfer path completed on the server |
| Plan create / activate / finish | `TeamPlanService` + durable `TeamPlan.actionId` | State transition is accepted by the plan state machine |
| Player rescue | `RescueService` + `CompanionEntity` | Reservation and rescue start, then separate revive completion record |
| Explicit power request | `AbilityService` | Server safety gate and ability result succeeded |
| Duo switch | `DuoDynamicsService` | Both selected roles are loaded after safe-base validation |
| Intention completion | `IntentionService` | A matching server-observed completion key was received |

The ledger is not a remote storage system. It records only compact metadata, never arbitrary item NBT or private chest content.

## Reservations

Reservations are type-scoped and expire automatically. Current source uses `RESCUE_TARGET` in the live rescue flow; generic item, plan, and squad reservation types are available for future explicitly approved systems. A reservation cannot grant a block-breaking, chest, farming, or cross-dimension permission.

A save reload treats an unconfirmed action as incomplete, never as success. Recovery rolls it back and releases stale local rescue state instead of inventing a second item, a second plan, or a second companion.

## Resting companion snapshot boundary

A duo change can place a non-selected companion in `RESTING` only at a validated HOME or REST anchor. The server saves a bounded personal snapshot before the live entity discards. The snapshot contains personal resources, bounded inventory, cooldown values, and original dimension; it deliberately excludes world position, navigation, targets, rescue state, and any player container data.

A snapshot can restore only at a safe base in its original dimension. It is not a portable chest, cross-dimension storage mechanism, chunk loader, or off-screen adventure simulation. A normal explicit dismissal consumes the resting snapshot and returns its bounded items exactly once to the owner.

## Save/load recovery checklist

`SaveRecoveryService.recoverAfterLogin` runs before the login recap and status sync.

1. The existing roster UUID gate rejects a duplicate companion entity.
2. The action ledger rolls back unconfirmed work instead of committing it.
3. Expired reservations are released.
4. An invalid or expired open plan becomes `FAILED_SAFE` and formation returns to `FOLLOW`.
5. Companion scout/return/base navigation tasks that cannot be trusted after load are cancelled to `FOLLOW`.
6. A stale in-progress rescue is reset and its reservation released.
7. A companion in an unsafe local position attempts the existing validated safe relocation; otherwise it holds safely.
8. Invalid companion inventory references are ignored and reported; no replacement item is created.
9. A RESTING snapshot missing its original dimension marker is not restored remotely; recovery reports it for explicit owner review.
10. A player-visible plan target must still be in the same loaded dimension or the plan becomes `FAILED_SAFE`.
11. A readable `RECOVERY` memory and Safe Mode reason are created when recovery changed state.
12. Only then may the normal recap/HUD status be sent.

This deliberately prefers a safe cancellation over a guessed continuation.

## Versioned data

`SaveVersions` currently identifies independent version domains:

- team root data;
- blackboard / journal / doctrine / memory metadata;
- team plan;
- companion entity;
- personal inventory and safe-base resting snapshot;
- roster data;
- fault circuit-breaker and player policy state;
- configuration contract.

`SaveMigrationService` preserves old fields where possible, supplies safe empty/default fields where a known field is missing, and does not erase unknown values. A save newer than this source version or an unexpected migration exception does not silently crash the world: the board is marked for Safe Mode with a readable migration reason.

On a migration failure, `MigrationBackupAdvisory` writes a local `migration-backup-required.txt` advisory in the world report directory. It explicitly asks the owner to create a complete manual world-folder backup before clearing Safe Mode; it does not pretend that copying a live world automatically is safe.

A real migration matrix still needs runtime testing against retained alpha-world copies. Do not treat the source migration path as a proven compatibility guarantee.

## Safe Mode

Safe Mode is per-owner and persisted in `SafeModeState`. It retains the base player tools:

- Follow;
- validated Safe Recall;
- Team Journal and diagnostics;
- explicit player visibility of the reason.

It disables or isolates optional/risky behavior:

- power requests and active Hive focus;
- active team plans;
- automatic combat;
- scout/plan initiative through the Team Director;
- future inventory automation and block-action extensions.

A bounded `CompanionFaultService` is also present: repeated companion-owned exceptions or repeated invalid-world relocation failures place the team in Safe Mode after a short circuit-breaker threshold. The player receives a readable reason; the local log retains the full throwable for diagnosis.

It can be enabled from the Team Journal or `/companions safe_mode`, and cleared only by an explicit player action after review:

```mcfunction
/companions safe_mode clear
```

A manual item transfer remains an explicit owner action, not inventory automation. It remains protected by its own server transaction and bounded six-slot inventory.

## Player emergency controls

`/companions stop_all_actions` enters persisted manual Safe Mode with the explicit `PLAYER_STOPPED_ACTIONS` reason. `/companions reset_task <role>` cancels one local task and reaches Follow, validated Safe Recall, or safe Hold without inventing a path. `/companions recall` recalls all loaded roles only through existing safe-destination validation. `/companions dismiss_all` is a conservative pre-uninstall helper: it deliberately defers a DOWNED roster entry instead of discarding an unverified live downed entity.

The Command Wheel and Team Journal are non-pausing screens. A normal integrated-server pause is treated as no ticks, not as a transaction completion. See `docs/OPERATIONAL_SAFETY_AND_OBSERVABILITY.md` for the complete pause/exit/runtime matrix.

## Non-claims

This document does **not** claim that real Forge chunk unloads, crashes, entity NBT serialization, power interruptions, or world migration have been executed successfully. The Forge build now compiles, but those runtime gates remain pending in `docs/CHAOS_AND_ACCEPTANCE_TESTS.md`.
