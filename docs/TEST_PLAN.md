# Test Plan

> This is a pending runtime plan. Static source validation does not substitute for a Forge 1.20.1 playtest.

## Lifecycle, transactions, and persistence

- Spawn every companion once; calling the same role again must recall or reject, never duplicate.
- Save/Load while following, holding, guarding, retreating, scouting, base activity, downed, rescuing, and recovering.
- Verify personal inventory, anchors, annotations, doctrines, relations, plans, focus target, Safe Mode, duo request, RESTING snapshots, personal policies, intention, and arc state restore safely.
- Interrupt manual give and withdraw with full/blocked inventory conditions. Confirm no item duplication, replacement, or loss.
- Switch duo at HOME/REST, save at each transaction edge, then reload. Confirm a RESTING snapshot either restores once at the same safe base or yields a readable Safe Mode/review path; it must never become cross-dimension storage.
- Verify lifecycle, plan, power, rescue, duo, and intention action IDs do not create duplicate completed work after reload.
- Pause during combat, exit during Safe Recall, exit at a portal/dimension boundary, and reload during an active plan. Confirm no action is committed merely by pause/exit and no duplicate entity/item/plan is created.
- Verify unconfirmed action ledger entries roll back and stale reservations release before recap.

## Migration and Safe Mode

- Keep world copies for each alpha save schema before changing a data version.
- Upgrade each copy, inspect player-readable recovery memory, and confirm no silent reset of valid roster, inventory, plan, or anchors.
- Test an unknown future data version and a deliberately invalid field. Expected: readable Safe Mode, a local manual-backup advisory, no crash, Follow/Recall/Journal still usable.
- Trigger repeated companion-owned fault injection only in a controlled development world. Expected: local log detail, readable circuit-breaker reason, and Safe Mode after the bounded threshold.
- Enable Safe Mode manually while a plan, automatic combat, and Hive focus are present. Expected: plan fails safely, focus cancels without deferred cost, powers reject, Follow/Recall remain usable.
- Clear Safe Mode only after an explicit player action; confirm normal safety gates still apply.

## Navigation and formation

- Execute the full matrix in `docs/NAVIGATION_AND_STUCK_RECOVERY.md`: surface, cave, stair, cliff, lava, water, closed-door, ladder/vine, leaves/cobweb, bed, chest, crafting table, redstone, and protected-zone scenarios.
- Two active companions must reserve distinct formation slots, avoid player camera center/interaction blocks, and never pile onto a single candidate square.
- Bounded alternate replans must escalate to Stuck Recovery without a permanent loop, block breaking, door use, chunk loading, or unsafe drop.
- Destroy/obstruct a stored safe waypoint and confirm a task cancels rather than inventing terrain or block actions.

## Vanilla rules, onboarding, and accessibility

- Peaceful: Defend plan proposal rejects with a readable world-rule code while Follow, Recall, Journal, Guide, and base behavior remain available.
- Creative: automatic survival-pressure dialogue and combat planning remain paused; manual safe commands still work.
- Spectator: companions hold safely and resume normal Follow only after the mode ends.
- Toggle Mob Griefing, Keep Inventory, Daylight Cycle, and Hardcore in isolated test copies. Confirm the Guide/diagnostic policy reflects real values and companions never gain block-edit permission.
- Trigger first companion, first plan, Stuck Recovery, Safe Mode, and relevant world-rule hints. Each must appear at most once, persist across Save/Load, and never lock input or progression.
- Test `/companions tutorial dismiss` and `/companions tutorial resume`; no historical hint flood may occur.
- Set `ChatProfile.CRITICAL_ONLY`: P0 alerts remain visible while normal/ambient dialogue is suppressed and HUD/Journal/commands stay readable.

## Plans, duo, intentions, and dialogue

- Run the fixed Guardian/Hopper decision scenarios from `docs/ROLE_READINESS_AND_AFTER_ACTION_CONTRACT.md` and `docs/BEHAVIOR_ACCEPTANCE_CATALOG.md` repeatedly. The initial acceptance target is at least 9 safe, explainable outcomes in 10 controlled runs for each written scenario; it is not a claim about every random Minecraft situation.
- P0 health critical must interrupt low-value work with a bounded retreat.
- Defend and Structure Entry must support accept, decline, cancel, success/failure, timeout, and `FAILED_SAFE` recovery.
- Run all six duo scenarios in `docs/CHAOS_AND_ACCEPTANCE_TESTS.md`; validate safe HOME/REST selection, no active-plan/downed bypass, and no automatic damage combo.
- Verify outgoing duo inventory returns exactly once to the player before dismissal.
- Offer, accept, defer, and server-complete each optional intention. Ignore one for multiple in-game days; confirm no penalty/timer/progression lock.
- Cycle/reset each personal power policy from the Policies tab and command surface. Confirm policy changes remain owner-only and never bypass Safe Mode, tags, target checks, cooldown, energy, or strain.
- Test Gifted READY/CAUTION/LIMITED/EXHAUSTED/RECOVERING/DOWNED state transitions. Confirm a readiness suggestion never auto-casts a power.
- Test Push beside villagers, pets, protected entities, and another companion. Expected: protected-boundary rejection before Energy/cooldown loss. Test Shield with the same boundary: expected owner-only effect.
- Run Guardian plan success/cancel/timeout/FAILED_SAFE paths and verify one factual after-action review, with no forced extra plan or dialogue flood.
- Verify no repeated normal dialogue flood, role persona drift, or non-English runtime dialogue.

## Resource ownership and Team Supply

- Nearby unowned healing item + low companion health + `EMERGENCY_ONLY`: exactly one short-range `WORLD_FREE` transfer, reservation, and transaction record.
- Diamond, TNT, lava bucket, owned player item, unknown modded item, and structure chest: no automatic pickup/open/read/withdraw.
- Distant arrow behind lava: no navigation request or pickup attempt.
- Bind Team Supply only from a safe HOME/REST anchor. Confirm same-dimension/range/container/whitelist/daily-cap transaction behavior.
- Disable inventory assist or Team Supply mid-session: no queued pickup/withdraw action remains.
- Save/load personal inventory, resting snapshot, and Team Supply state without duplication/loss.

## Downed and powers

- Test stable, danger, unreachable, rescuing, and recovering states.
- Test two downed companions and deterministic rescue priority/reservation behavior.
- Test role-readable rescue posture without automatic power cast or player-control loss.
- Test all power policies, personal policy overrides, Will Disrupt hostile eligibility/control-heart resistance, Will Surge emergency/protected-boundary/boss/save-load cases, Guardian Brace threat/range/mitigation/Safe Mode behavior, Scout Signal visible-target/focus/Low Effects behavior, Max Grounding, advanced Mind Anchor supported-effect/anchor radius/escape-window cases, and a datapack protected-tag change during a Will focus.
- Reload valid and malformed advisory threat/structure profiles. Confirm they can add cautious reason text only; they must not reveal hidden structures, remove modded Unknown/Caution, or grant an ability.

## Animation, Hive entities, visual, and audio

- Run the complete runtime matrix in `docs/ANIMATION_DIRECTOR_SYSTEM.md`: shared rig, clip existence, transition blend, marker cancellation, Low Effects, VFX cap, no camera/input lock, Save/Load safe state, sound fallback, and performance evidence.
- Test the late-arc private textures in all companion poses under day, night, rain, cave, combat, downed, and social clips; inspect UV seams, readability, and accessory motion.
- Test every Social Director cue: player-selected campfire/bed/work block, weather, horizon, base, cave, village, travel, and calm. Verify lead/reply timing, normal chat budget sharing, social feature disable, Social tab status, and all cancellation boundaries.
- Test expanded Conversation topics: Read the world, Team check, Last encounter, and Check in. Confirm responses use only visible/team/bounded-memory facts.
- Confirm a rejected power or target-invalid cancellation never plays a release-success clip, particle burst, or success sound.
- Confirm server-confirmed melee/hit-react presentation does not make a GeckoLib marker authoritative for damage.
- Confirm the registry contains only the four companion entity types and that no custom hostile entity, hostile texture, enemy model, loot table, worldgen, or forced encounter appears in a new normal-world save.
- Verify Will, Guardian, Gifted, and Scout presentation against vanilla hostile encounters without replacing Minecraft mobs or adding a monster source.
- Execute `VISUAL_TEST_01` through `VISUAL_TEST_04` and `AUDIO_TEST_01` in `docs/CHAOS_AND_ACCEPTANCE_TESTS.md`.
- Record FPS, TPS/MSPT, and Spark evidence with Low Effects on/off after the Forge workspace exists.

## Chaos scenarios

Run every scenario from `docs/CHAOS_AND_ACCEPTANCE_TESTS.md`. Expected result is always no crash, no duplication, no infinite loop, no silent data corruption, and no loss of player control. A safe cancellation is a passing fallback; a forced continuation is not required.

## Language and packaging

- Confirm no Arabic text appears in `src/main/java`, `src/main/resources`, or project documentation.
- Confirm only `en_us.json` is packaged.
- Confirm no Gradle/build/JAR artifacts are included in the source archive.
