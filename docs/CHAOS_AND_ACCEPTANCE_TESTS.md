# Chaos, Acceptance, Visual, and Performance Test Gate

## Status

This is an executable **test plan**, not a report of completed runtime tests. The Forge Java 17 build workspace now compiles cleanly, but Spark profiling and every real Minecraft test-world result remain unverified.

`/riftcompanions_dev chaos <scenario>` prints the expected safety contract and current local plan state. It does not simulate a passing test and must not be used as evidence of runtime stability.

## Fixed chaos scenarios

| Scenario ID | Operator setup | Expected safe outcome |
|---|---|---|
| `logout_during_active_plan` | Save/exit while a plan is active | No duplicate plan action; invalid continuation becomes `FAILED_SAFE` |
| `pause_during_combat` | Pause and resume an integrated single-player world during visible combat | No extra action is committed while ticks are stopped; state remains coherent on resume |
| `exit_during_safe_recall` | Save/exit at the recall transaction edge | Rollback or one confirmed recall only; no clone and no unsafe destination |
| `safe_recall_destination_contention` | Recall two active companions in one command burst; repeat in a narrow one-square corridor and fresh two-role spawn burst | Each destination is distinct, loaded, collision-clear, and short-reserved; a later unsafe request fails/holds safely rather than stacking entities |
| `exit_during_dimension_boundary` | Save/exit while player is at a portal/dimension boundary | No automatic companion portal use or cross-dimension clone |
| `dimension_change_during_scout` | Move player dimension during Scout | Scout cancels to Follow/Resting; no portal or chunk-load trick |
| `player_death_during_rescue` | Die while a rescue is in progress | Rescue reservation clears; companions use existing safe fallback |
| `companion_stuck_in_leaves_water_or_door` | Trap a companion in each case | Bounded recovery; safe recall or hold, never block breaking |
| `visible_hostile_response_contention` | One to five visible zombies target player/companion while companions are following or have recent formation slots | Combat-capable companions clear stale formation, acquire local targets, fight/retreat without freezing, emit bounded dialogue, and only propose Defend for player approval |
| `inventory_full_during_transfer` | Fill inventory / companion slots during give/withdraw | No duplicate, no generated replacement item |
| `target_dies_during_seer_disrupt` | Kill target during validated ability | No repeated hit, no loot change, readable rejection |
| `powers_disabled_during_gifted_focus` | Toggle power flag during focus | Existing focus uses safety cancellation; future casts reject |
| `safe_waypoint_destroyed` | Remove/alter safe route terrain | Stale navigation cancels; no assumed block placement |
| `structure_chunk_unload_during_memory_callback` | Leave the structure area during callback | No hidden discovery, no forced chunk loading |
| `chat_flood_then_alert` | Queue low priority lines then trigger P0 | Critical alert remains readable and low lines remain bounded |
| `doctrine_change_during_active_plan` | Change doctrine around an active plan | Current plan stays coherent; future plan sees new doctrine |
| `datapack_reload_protects_will_target` | `/reload` adds the selected target type to `protected_from_companions` | Release revalidates hostile eligibility, cancels before full cost, and applies no control state |
| `resting_snapshot_safe_base_boundary` | Rest a duo companion, save/reload, move dimension, then attempt restore | Restore rejects outside original-dimension safe base; normal dismissal returns bounded items once |
| `repeated_companion_fault` | Controlled development-only fault injection for one companion | Readable fault reason and Safe Mode circuit breaker; no crash loop |
| `closed_door_navigation` | Follow/formation route ends behind a closed door | Route around, hold, or recovery; no autonomous door use |
| `cliff_drop_rejection` | Requested path contains a large ravine descent | Path node rejects before deliberate unsafe drop |
| `formation_slot_contention` | Two companions approach one compact safe area | Expiring reservations keep slots distinct; no pileup loop |
| `reset_task_during_navigation` | Reset one companion while a bounded path is active | Local path/focus clears; Follow, Safe Recall, or safe Hold is the only fallback |
| `gifted_push_protected_bystander` | Request Push with a villager, pet, or companion near the impact boundary | Push rejects before Energy/cooldown loss; shield stays owner-only |
| `performance_tier_degradation` | Compare STANDARD and LIGHT under the fixed baseline route | Optional work is reduced before Follow, Recall, rescue safety, or critical alerts |
| `vanilla_rule_policy_transition` | Change to Peaceful, Creative, or Spectator in a controlled world | Combat initiative pauses without block edits or loss of Follow, Recall, Journal, or safe controls |
| `tutorial_save_load` | Deliver/dismiss/resume guidance across repeated Save/Load | One-time progress persists; no message flood or gameplay block occurs |
| `critical_alert_profile` | Use `CRITICAL_ONLY` during alert + ambient queue pressure | P0 alert remains readable while non-critical dialogue is suppressed |
| `animation_marker_cancel` | Invalidate a power target/policy before planned release timing | No success clip, particle burst, or sound after server rejection/cancel |
| `animation_safe_load_fallback` | Save/exit during focus, strike, or recovery presentation | Reload derives safe visual state from server AI and no delayed effect applies |
| `animation_low_effects_cap` | Compare normal and Low Effects power cues | Particle count drops while pose/icon/HUD/chat fallback remains readable |
| `social_reply_interrupted` | Start a safe social lead, then create danger, Safe Mode, active plan, low health, or pair distance break before reply timing | Pending reply cancels with no delayed dialogue, power, movement, inventory, plan, or player-control change |
| `social_logout_pending_reply` | Logout after an authored social lead and before its reply | Session clears; login never fabricates a stale reply or social animation |

## Duo campaign

Run one controlled session for each exact pair:

```text
Guardian + Seer: unknown structure
Guardian + Gifted: hostile crowd
Guardian + Scout: mountain travel
Seer + Gifted: active hostile-control threat
Seer + Scout: unknown biome
Gifted + Scout: retreat route
```

Confirm the pair’s wording, formation, bounded synergy note, and blind-spot warning match the active roles. Confirm no pair gains automatic damage, invulnerability, remote scouting, or boss bypass.

## Visual and audio checks

| Test | Setup | Expected result |
|---|---|---|
| `VISUAL_TEST_01` | Guardian Guard in narrow cave | Does not permanently block crosshair/camera |
| `VISUAL_TEST_02` | Seer anomaly cue with Low Effects | HUD/chat fallback remains understandable without particle dependence |
| `VISUAL_TEST_03` | Gifted Shield with 20 mobs | Particle cap/Low Effects preserve readability without collapse |
| `VISUAL_TEST_04` | Scout route point during combat | Route cue is clear without hiding the hostile target |
| `VISUAL_TEST_05` | Faceted face review at close, normal, side, rear, crouch, swim, and low-light camera ranges | Tapered head, separate eyes/lids/brows/jaw, and grounded clothing remain readable without missing UVs or severe clipping |
| `VISUAL_TEST_06` | Four role Idle, Walk, Run, Combat, power, talk, emotion, and social clips | Blink, local eye drift, jaw talk, secondary hair sway, and role-specific motion remain stable; no gameplay state changes from a visual bone |
| `VISUAL_TEST_07` | Recall All / fresh two-role spawn in open terrain and one-block corridor | No companion occupies the same AABB or centered block destination; tight space rejects/holds safely instead of stacking |
| `AUDIO_TEST_01` | Alert + ambient + power cue | Alert is distinguishable; cues do not become constant noise |

## Fixed performance baseline

Before a performance claim, use one disposable world with a recorded seed, a marked surface route, a simple cave route, and an optional twenty-hostile enclosure. Run ten-minute one-companion and two-companion sessions with the same settings, then compare TPS/MSPT, memory trend, path rebuild count, event count, and the opt-in developer overlay against the initial baseline. Do not invent a universal millisecond threshold before the actual test hardware is known.

## Acceptance evidence export

For each playtester, record a local report with:

- date, seed, Minecraft/Forge/GeckoLib versions;
- selected duo and feature flags;
- scenario IDs run;
- crashes, duplicate items/entities, lost items, or player-control issue;
- perceived clarity for alert, plan, route, rescue, and Safe Mode (1–5);
- whether a companion behaved out of persona;
- TPS/MSPT/Spark capture once available.

Do not declare a stable release until the fixed scenarios, visual/audio checks, migration world copies, and performance profile have passed in a real Forge runtime.
