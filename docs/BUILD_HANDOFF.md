# Build and Runtime Handoff

## Current build workspace

A ForgeGradle workspace is now present in this repository after explicit owner approval.

```text
Minecraft: 1.20.1
Forge: 47.4.22
Java target: 17
Gradle wrapper: 8.8
GeckoLib Forge dependency: 4.7.1.1
```

The clean build command has succeeded. See `docs/BUILD_VALIDATION_REPORT.md` for the exact boundary of that claim.

## 0.9.0-dev multi-stage contextual scene scope

This revision retains the null-`Gson` crash fix, faceted-character rebuild, Safe Recall anti-stacking placement, layered animation, and visible-hostile response. It upgrades contextual interaction into 198 multi-stage Discovery/Revisit scenes with 1,584 contextual lead/reply lines for visible animals, safely observed distant mobs, player-used work blocks, weather/time, biomes, player-selected structures, and fatigue. It is visual/dialogue information only: no animal control, loot grant, container access, x-ray, forced plan, or runtime AI text generation. Build and static checks do not replace a clean in-game interaction, combat, gait, animation, recall, or world-creation regression test.

## Build command

Use Java 17 and a single worker on low-memory environments:

```bash
./gradlew clean build --no-daemon --max-workers=1 -Dorg.gradle.internal.instrumentation.agent=false
```

The output JAR is under:

```text
build/libs/riftcompanions-0.9.0-dev.jar
```

A persistent copy is also delivered outside the generated `build/` directory when a build succeeds.

## Runtime prerequisites

A normal Forge client test profile needs:

1. Minecraft Java Edition 1.20.1.
2. Forge 47.4.22.
3. GeckoLib Forge 4.7.1.1.
4. The generated Rift Companions JAR.
5. An isolated disposable test world.

Do not test first against a valued survival world.

## First runtime gates

- Confirm Forge discovers `riftcompanions` in the Mods list.
- Spawn one companion, then test Follow/Hold/Recall for 20 minutes.
- Test 20 Save/Load cycles with active companions, plans, inventory, anchors, doctrine, and relation data.
- Test recall near lava, water, cliffs, walls, caves, and powder snow.
- Test all formations without player push or interaction-block obstruction.
- Run every scenario in `docs/NAVIGATION_AND_STUCK_RECOVERY.md`, including closed doors, stairs, ladders, leaves, ravines, fluids, narrow caves, slot contention, chunk unload, and Stuck Recovery.
- Run the animation, behavior, Story Journal privacy, Team Supply privacy, chaos, and acceptance catalogs.
- Profile path creation/replan frequency with Spark while two companions follow a moving player through a fixed cave route.
- Test normal Minecraft hostile interactions: Will control boundaries, Guardian Brace, Scout Signal, Gifted powers, threat observation, and formations against visible loaded hostile mobs only.
- Confirm the mod registers no custom hostile entity, enemy texture, hostile worldgen, natural spawn, operation-wave spawn, loot-table path, or forced encounter system.
- Test Guardian Brace and Scout Signal with normal, Low Effects, Safe Mode, save/load, policy, protected-target, and cooldown cases.
- Test the faceted head/eyes/lids/brows/jaw, hair secondary motion, role-specific idle/walk/run/combat/power clips, paired dialogue timing, social reply cancellation, expanded conversation topics, campfire/work/weather/horizon/cave/village/travel cues, and Social Journal view in a disposable world.
- Test Recall All, fresh two-role spawn, Stuck Recovery recall, save/load relocation, and a one-block corridor. Verify distinct destination AABBs in open terrain and a safe reject/hold rather than a stack in tight terrain.
- Test one to five visible zombies attacking player/companion while active companions are following, holding recent formation slots, or using player focus. Verify FIGHTING clears stale formation, melee pursuit/attack starts, dialogue remains bounded, Defend remains player-approved, and critical pressure uses Retreat rather than a frozen team.
- Test a visible cow, a safely distant visible skeleton, biome transitions, a player-selected mineshaft/structure assessment, and low companion Energy. Verify one authored contextual action/line, cooldown behavior, no animal control, no auto loot/farm path, no container access, no forced objective, and suppression during danger.
- Profile companion/team/social ticks with Spark before enabling more content.

## Do not skip

- Back up worlds before an update.
- Test one feature at a time against a fixed world seed.
- Do not add automatic portal traversal, chest access, or destructive actions before their separate acceptance tests.
- A successful Gradle build is not a successful Minecraft runtime test.
