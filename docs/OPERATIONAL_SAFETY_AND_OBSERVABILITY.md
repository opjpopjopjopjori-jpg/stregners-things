# Operational Safety, Save Authority, Performance, and Developer Observability

## Status and scope

This document describes **source implemented contracts** for the offline, integrated-server design. The approved Forge Java 17 workspace now builds cleanly, but this is not evidence that an actual Forge world has passed these flows.

The mod remains single-player only. It has no multiplayer ownership model, network service, API key, external language model, or internet dependency.

## Authority boundary

Minecraft single-player still has a logical server and a client. Rift Companions keeps permanent or gameplay-changing facts on the logical server.

| Layer | Owns | Must not own |
|---|---|---|
| Logical server | AI state, damage, ability validation, inventory transactions, lifecycle UUIDs, plans, memory, Safe Mode, navigation decisions, save recovery | HUD selection, client-only particles, camera behavior |
| Client | Rendering, animation playback, key mapping, Command Wheel, Team Journal, HUD, developer overlay, local feedback presentation | Damage, path result, item transfer, block action, plan completion, recall destination, save mutation |

A client command packet contains only a fixed command ID and bounded role or ability ID. The server validates the request again. A UI button cannot issue an arbitrary block edit, entity operation, chest read, or item grant.

## Persistent data contract

### Companion entity NBT

Live entity NBT includes only bounded personal state such as:

- owner UUID and fixed role;
- health, energy, Hive Strain, Focus, and combat toggle;
- current state and readable reason code;
- bounded six-slot personal inventory;
- cooldowns, shield/recovery/downed state, and last safe waypoint;
- valid local return/base task references needed for recovery.

It does not persist an arbitrary client command, a raw vanilla path object, chest contents, an unbounded conversation transcript, or a general remote-storage location.

### World `SavedData`

`TeamSavedData` is the authoritative per-owner world record. It contains roster lifecycle, one actual entity UUID per role, Team Blackboard, journal memories, anchors, plan state, action ledger, reservations, Safe Mode, bounded policy state, encounter context, role readiness/review state, and version markers.

A RESTING snapshot is intentionally narrow: personal resources, bounded inventory, cooldowns, health, and original dimension. It excludes a world position, target, path, rescue progress, container contents, and off-screen activity. It can restore only at a validated safe base in the original dimension.

## Version and migration policy

Each persisted domain has a dedicated marker in `SaveVersions`. The current source milestone uses team data version `21`, blackboard data version `17`, and config contract version `11`.

On load, migration follows conservative rules:

1. Preserve recognized existing fields.
2. Add a safe empty/default value only for a missing known field.
3. Never turn an unknown future save version into an assumed current version.
4. Mark a migration failure for readable Safe Mode review rather than silently continuing.
5. Write a local manual-backup advisory on migration failure; it does not claim to make a safe live world backup automatically.

## Pause, exit, and load behavior

The Command Wheel and Team Journal explicitly report `isPauseScreen() == false`; opening them does not claim to stop logical-server simulation. A normal integrated-server pause is treated as a temporary absence of ticks, not as a special action completion event.

The source contract is therefore:

- no action is committed merely because a pause, close, or disconnect happened;
- an incomplete transaction remains incomplete and is recovered as rollback/expiry, never guessed as success;
- a stale Scout, Return Home, or Base Activity task cancels to a safe follow state after load;
- a stale rescue reservation resets;
- invalid plan target/leader state becomes `FAILED_SAFE`;
- an unsafe companion location attempts only the existing validated safe relocation; otherwise it holds;
- the roster UUID gate rejects a second instance of the same role.

These flows must still be verified in an actual Forge world with pause/exit at combat, Safe Recall, dimension boundary, and active-plan edges.

## Explicit emergency controls

The player always retains direct safe control. The current source surface maps the requested operational controls as follows:

| Player intent | UI / command | Server result |
|---|---|---|
| Call team home | Command Wheel `Call Team Home` / `/companions return_home` | Existing HOME anchor validation only; no portal, cross-dimension, or automatic transport |
| Stop all actions | `Stop All` button / Command Wheel / `/companions stop_all_actions` | Persisted manual Safe Mode cancels plans, focus, optional powers, automatic combat, and optional initiative; Follow, Recall, Journal, and Resume remain available |
| Clear current plan | `Clear Plan` / `/companions cancel_plan` | Cancels the active/draft plan, returns formation to Follow, and records a bounded Guardian review |
| Reset one task | `/companions reset_task <role>` | Stops local navigation, clears local target/focus, then uses Follow, validated Safe Recall, or safe Hold fallback |
| Recall one or all | `/companions recall <role>` or `/companions recall` | Server validates loaded safe ground; no unsafe teleport is fabricated |
| Unstuck | `/companions unstuck <role>` | Uses the same safe recall path; it does not break a block or open a door |
| Repair saved state | `/companions repair_save` | Cancels open plan/focus/navigation and returns loaded companions to a bounded safe state |
| Pre-uninstall preparation | `/companions dismiss_all` | Dismisses existing non-downed roster entries only; downed entries are deliberately deferred for explicit resolution |

Building automation is disabled by design. There is no companion block breaking, building, TNT, lava, fire, Redstone, door use, farm behavior, or private-chest access to disable at runtime.

## Work scheduling and performance policy

The source uses bounded/event-driven cadence rather than a full world scan every tick.

| Cadence | Allowed work |
|---|---|
| Every tick | Essential entity movement, damage response, short-lived effect countdown, and bounded companion state progression |
| Every few ticks | Formation update and immediate event processing through the existing bounded queue |
| About 20–40 ticks | Local perception, encounter context, role readiness, and short-range optional scans subject to configuration and performance tier |
| About 100+ ticks | Environment reassessment, base life, temporal memory, optional intention/story checks, and micro-scenes |
| About 200+ ticks or event driven | Long-lived review, journal reflection, and optional ambient opportunities |

`LIGHT`, `STANDARD`, and `CINEMATIC` scale optional cadence through `PerformancePolicy`. They do not change the safe active-squad cap of two companions. LIGHT reduces optional context work, inventory assistance, and ambient scenes before it compromises Follow, Safe Recall, rescue safety, plan cancellation, or critical alerts.

The code never intentionally loads a chunk for a Scout, path target, loot target, off-screen companion, or compatibility lookup.

## Developer instrumentation

Developer timing is opt-in:

```text
common.developer_diagnostics.enabled = false by default
client.hud.developer_overlay_default = false by default
```

When the common flag is enabled, the source keeps a tiny in-memory rolling summary of Companion AI, navigation, Team Director, and dialogue work. It does not persist samples, upload data, contact a service, or claim to replace Spark.

The owner-only developer overlay is toggled with `F8`. It shows:

- current companion task/state;
- requested and selected bounded path target;
- path intent, outcome, retry count, and failure reason;
- current plan/event queue context;
- memory usage against its explicit bound;
- last dialogue trigger/outcome and normal-chat cooldown;
- sampled and rolling microsecond summaries for AI, navigation, Team Director, and dialogue work.

`/riftcompanions_dev perf` prints the same compact local timing summary. `/companions export_memory` includes readiness, Guardian review, timing, dialogue, and navigation diagnostic details in a local report.

A timing summary is not a TPS/MSPT benchmark. Runtime profiling must still use a real Forge test world and an appropriate profiler; a clean build is not performance evidence.

## Fixed baseline world and performance gate

Before publishing a performance claim, create one disposable runtime test world with:

- a recorded fixed seed;
- a marked surface route and a marked simple cave route;
- a repeatable twenty-hostile test enclosure used only when needed;
- one-companion and two-companion runs of ten minutes each;
- the same client settings, feature flags, and active duo recorded for every run.

Record baseline TPS/MSPT, allocated memory trend, path rebuild count, active event count, and developer overlay values. Compare every new feature against that baseline. Do not invent a universal millisecond threshold before the actual test hardware is known.

## Runtime acceptance matrix

The future runtime campaign must include at least:

- pause during combat;
- exit during Safe Recall;
- exit at a dimension boundary;
- save/load during active plan, rescue, and RESTING snapshot transitions;
- reset task during active navigation;
- protected bystander boundary during Gifted Push;
- LIGHT performance degradation behavior;
- repeated save/load without duplicate/missing entity, duplicate item, or duplicate plan action;
- a 20-minute and later 60-minute stable companion session;
- fixed Hopper acceptance scenarios and role readiness suggestions;
- visual/camera/audio checks and a profiler capture.

The developer chaos briefing lists these checks but does not simulate a passing result.

## Bug report template

Use this exact evidence format for runtime defects:

```text
World seed:
Minecraft version:
Forge version:
GeckoLib version:
Rift Companions source/JAR version:
Installed mods:
Active duo and feature flags:
Performance tier and relevant policies:
Steps to reproduce:
Expected result:
Actual result:
Save/exit/teleport edge involved:
Developer overlay or exported report:
latest.log / crash report:
```

## Related guidance and Vanilla policy

See `docs/WORLD_RULES_ONBOARDING_ACCESSIBILITY_CONTRACT.md` for the server-derived Vanilla rule projection, no-world-edit invariant, optional one-time tutorial, Guide tab, and critical-alert-only dialogue behavior.

## Deliberate non-claims

This source does not claim successful Forge save/load, migration, pause, TPS/MSPT, Spark, visual FPS, audio mix, or long-session gameplay evidence. It also does not add automatic boats, mounts, minecarts, portal transfer, End entry, chunk loading, multiplayer support, broad mod support, or an external AI controller.
