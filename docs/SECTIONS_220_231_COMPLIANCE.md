# Compliance Audit: Design Bible Sections 220–231

**Audit scope:** current source-only repository.  
**Status vocabulary:** `Implemented source` means a bounded source path and data contract exist. `Partial` means the core contract exists but the full requested breadth is deliberately absent. `Deferred` means intentionally outside current scope. `Runtime unverified` means a real Forge test is still required.

> This audit was refreshed after Team Supply, policy editor, first-home milestones, intentions/promises, role readiness, and Guardian review source work. It does not treat source presence as gameplay proof.

## 220. Dialogue Choice Philosophy

| Requirement | Status | Evidence / boundary |
|---|---|---|
| Ask for information | Implemented source | Conversation topics cover plan, health, memory, place, and role limits. |
| Accept / delay plan | Implemented source | Plan accept, decline, cancel, timeout, and `FAILED_SAFE` paths are server-owned. |
| Set personal policy | Implemented source | Journal, packet, and command policy editor for Seer/Gifted/Scout; Safety Gates remain final. |
| Offer support/rest | Implemented source | Safe anchor/rest selection and optional intentions are bounded. |
| End conversation | Implemented source | Local conversation close control. |
| No forced romance / moral test / disclosure | Implemented by design | No romance, hidden dialogue score, or private-data prompt source path exists. |
| Action-based relationship | Partial | Trust, consequences, arcs, and promises are bounded foundations, not full relationship simulation. |

## 221. Team Hub and Base Life

| Requirement | Status | Evidence / boundary |
|---|---|---|
| HOME/Entry/Rest/Guard/Journal/Supply/Lookout/Quiet/Memorial anchors | Implemented source | `BaseAnchorType` and server anchor services. |
| Role base routing | Implemented source baseline | Guardian guard/entry, Seer journal, Gifted quiet/rest, Scout lookout/entry routes are bounded/no-block behavior. |
| No chest/farm/sleep/Redstone/block changes | Implemented by design | Base source contains no container, harvesting, sleep, Redstone, or block-edit behavior. |
| BASE_RETURN / prep / memory / intention | Implemented source foundation | Quiet windows, milestone memory, first-safe-return/base-plan hooks, optional intentions, and review data; not a cinematic base simulation. |
| Team Supply permissions | Implemented source foundation, disabled by default | Explicit player-bound safe-base container, whitelist/cap/reservation/transaction; no discovery or private chest access. |
| Base-life tests | Runtime unverified | Fixed checklist exists; no Forge session has run. |

## 222. Home as Memory

| Requirement | Status | Evidence / boundary |
|---|---|---|
| First/current/previous home | Implemented source | HOME anchor and bounded milestone memory updates. |
| First safe return / first plan from home | Implemented source | Dedicated Blackboard flags and milestone service. |
| Rare callbacks without entry spam | Implemented source foundation | Temporal/quiet/cooldown rules; full landmark library is deferred. |

## 223. Temporal Memory and Calendar

| Requirement | Status | Evidence / boundary |
|---|---|---|
| Game-time-only temporal source | Implemented source | Minecraft day/game time/session recap, not real-world drama. |
| Post-crisis quiet and dawn opportunity | Implemented source foundation | Quiet window, temporal service, bounded daily ambient/dawn hooks. |
| Intention/promise defer behavior | Implemented source | Optional/deferable state with no timer, punishment, or progression lock. |
| Memory aging/probability | Partial | Bounded cooldown and temporal selection exist; no large probabilistic long-memory simulator. |
| Boss/Nether/promise anniversaries | Deferred | Requires tested boss/dimension content beyond current scope. |
| Temporal tests | Runtime unverified | Runtime checklist exists. |

## 224. Downed, Rescue, Separation, Recovery

| Requirement | Status | Evidence / boundary |
|---|---|---|
| Stable/Danger/Unreachable/Rescuing/Recovering | Implemented source | `DownedStatus`, bounded rescue flow, recovery. |
| Guardian guard / Seer observe / Gifted readiness / Scout route posture | Implemented source baseline | Rescue choreography is posture/cue only; no forced power. |
| Player is decisive rescuer | Implemented source | Player starts the explicit rescue transaction. |
| Reservation and deterministic priority | Implemented source | Bounded reservation and `RescuePriorityService`; final runtime reachability UX remains unverified. |
| No off-screen solo death/adventure | Implemented by design | No solo task/chunk-load/off-screen simulation source path. |
| Downed tests | Runtime unverified | Required matrix documented. |

## 225. Travel and Transport

| Requirement | Status | Evidence / boundary |
|---|---|---|
| Walking travel | Implemented source | Bounded navigation, formations, stuck recovery, safe recall fallback. |
| Boats / horses / minecarts | Deferred | No seat, ownership, rail, water, or dismount system. |
| Portal/dimension protocol | Deferred | Automatic transfer is deliberately disabled; companions do not auto-enter portals. |
| Travel dialogue budget | Implemented source baseline | Dialogue priority/cooldown and quiet windows; no long-distance chatter simulator. |

## 226. Vanilla Event Integration

| Requirement | Status | Evidence / boundary |
|---|---|---|
| Player death fallback | Implemented source baseline | Plans cancel and companions move to bounded safe fallback. |
| Vanilla mode / gamerule policy | Implemented source foundation | Survival/Creative/Peaceful/Spectator plus Mob Griefing, Keep Inventory, Daylight Cycle, and Hardcore projection; no world-edit exception is granted. |
| Sleeping / weather / machines | Partial | Non-interaction/safety boundaries and weather context exist; no companion sleep mechanic or full weather content library. |
| Village/pet protection | Implemented source baseline | Protected tags/target policy plus Gifted Push bystander checks; raid integration remains deferred. |
| Advancements / raids / Dragon / Wither | Deferred | Requires independent runtime safety and boss/raid adapters. |

## 227–228. English Dialogue Content

| Requirement | Status | Evidence / boundary |
|---|---|---|
| English default dialogue | Implemented | `core_en_us.json`, 534 original English entries with bounded social lead/reply families, one packaged `en_us` locale. |
| Base/resource/structure/combat/recovery/role readiness families | Implemented source baseline | Trigger families, priorities, cooldowns, and original text. |
| Exact copied show lines | Not claimed | Project text is original English. |
| Long-form mature narrative | Partial | Bounded arcs/mystery/promise/role dialogue foundations, not a finished multi-season story. |

## 229. Natural Hostile-World Interaction

| Requirement | Status | Evidence / boundary |
|---|---|---|
| Custom hostile entities / worldgen | Deliberately absent | The mod registers companions only; no custom enemy roster, hostile texture, natural spawn, or operation-wave spawn exists. |
| Will hostile-control boundary | Implemented source | Visible loaded hostile `Mob` / `Enemy` targets use a heart budget; players, companions, villagers, animals, tameable entities, and protected types remain excluded. |
| Vanilla and compatible mod-hostile interaction | Implemented source foundation | Threat observation, formations, Scout Signal, Guardian Brace, Gifted safety gates, and Will controls use visible loaded world threats only. |
| Boss/farm/loot balance | Partial | Contracts and adapters exist; real balance benchmark is pending. |

## 230. Dependency and Benchmark Policy

| Requirement | Status | Evidence / boundary |
|---|---|---|
| Forge 1.20.1 metadata / GeckoLib declaration | Implemented source | `mods.toml`, no binary bundled. |
| Developer timing visibility | Implemented source | Opt-in local timing summary and F8 overlay; not a profiler substitute. |
| Spark benchmark / SmartBrainLib decision | Deferred | Requires actual Java 17/Forge runtime and measured baseline. |
| Dependency audit | Partial | Documentation/source boundary exists; no real dependency benchmark data exists. |

## 231. Toolchain

| Requirement | Status | Evidence / boundary |
|---|---|---|
| Java 17 / Forge / IDE / Blockbench / profiler handoff | Documented | `docs/BUILD_HANDOFF.md` and test contracts. |
| Actual build workspace | Build verified | ForgeGradle workspace is present; a clean Java 17 / Forge 47.4.22 / GeckoLib 4.7.1.1 build succeeded. |
| Offline static validation | Implemented | `validate_source_tree.py` and `verify_user_requirements.py`. |

## Exact conclusion

The source now covers substantially more than a basic follower: all four role entities, safety lifecycle, plans, bounded role behavior, resource boundaries, mystery/promise foundations, and observability contracts are present. The approved Java 17 + Forge 47.4.22 + GeckoLib 4.7.1.1 workspace now compiles cleanly, but it is still **not runtime proven**. The next technical gate is real save/load, pause/exit, migration, navigation, role, visual/audio, and performance evidence.
