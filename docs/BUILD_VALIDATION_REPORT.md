# Build Validation Report

## Scope

This report records the clean ForgeGradle validation boundary for this source tree. It is intentionally narrower than a Minecraft runtime test report.

## Workspace

```text
Minecraft: 1.20.1
Forge: 47.4.22
Java: Eclipse Temurin 17.0.20
Gradle wrapper: 8.8
GeckoLib compile dependency: geckolib-forge-1.20.1 4.7.1.1
Project version: 0.9.0-dev
```

## Current source revision additions

```text
Will control-heart budget and bounded hostile eligibility
Will active-threat / anti-farm gate
Short Stagger path for overload and partial boss resistance
Wither and Ender Dragon partial-resistance tag entries
Display-only Will target count and heart load/capacity HUD packet fields
Guardian Brace and Scout Signal bounded tactical actions
Entire prior visual direction retired and all eight 512×512 RGBA character atlas files regenerated
Four original faceted-character Geo models preserving the shared_humanoid_v2 animation backbone
68–72 bones and 57–61 bounded cuboid layers per role for tapered face planes, independent eyes/lids/brows/jaw/mouth, layered hair, articulated body, and grounded clothing
faceted_character_512_v2 visual contract with UV, parent-graph, canonical-pivot, face-bone, hair-motion, and animation-bone validation
Three independent GeckoLib controllers: body action, face expression, and secondary hair/clothing motion
Natural gait director: travel-vector body heading plus event-driven finite eye-lead/head-turn/hold/head-return/eye-recenter gestures; idle/alert head yaw stays neutral
Authored local blink, eye drift, brow expression, jaw-talk, secondary hair/clothing sway, and differentiated role idle/walk/run/combat/power/recovery presentation
Safe Recall / first-spawn anti-stacking: living-entity destination clearance, short owner-only placement reservations, stale formation-slot reset, lifecycle release, and logout cleanup
Visible hostile anti-freeze response: bounded local target assignment, FIGHTING state/formation interruption, authored contact/crowd dialogue, player-approved Defend proposal, and existing critical Retreat gates
Contextual Scene Director: 198 Discovery/Revisit data scenes, 1,584 contextual English lead/reply lines, nine contextual action clips per role, local visible entity/player-block/weather/time/biome/player-selected structure/fatigue gates, config version 16, and protocol 33
Bounded authored Social Director, paired-reply budget, Social Journal view, and expanded player-led conversation topics
Natural-world-only policy: no custom hostile entity, enemy texture, hostile worldgen, or operation spawn system
Configuration version: 15
Custom protocol: 32
Retained crash hotfix: Gson initializes before every SimpleJsonResourceReloadListener singleton
```

## Executed command

```bash
./gradlew clean build --stacktrace --no-daemon --max-workers=1 -Dorg.gradle.internal.instrumentation.agent=false
```

## Current clean build result

```text
BUILD SUCCESSFUL in 30s
8 actionable tasks: 8 executed
```

The verified clean run completed:

```text
clean
compileJava
processResources
classes
jar
downloadMcpConfig
extractSrg
createMcpToSrg
reobfJar
assemble
check
build
```

This verifies Java compilation, Forge/GeckoLib dependency resolution, resource processing, JAR packaging, and Forge reobfuscation. It does not prove runtime behavior.

## Artifact attestation

The final delivered development JAR is copied outside the generated `build/` directory. Its exact byte size and SHA-256 are written to an adjacent external `.sha256` sidecar at packaging time.

The checksum is kept in an external sidecar so it can be verified before installing the JAR and so the delivered artifact remains easy to identify independently of its source archive.

The JAR must remain above the requested 3 MB threshold through substantive original texture, animation, dialogue, code, and registered original audio assets. No padding file is permitted solely to inflate size.

## Integrated-world crash-hotfix validation retained in 0.9.0-dev

The reported integrated-world crash was traced to Java static field order in the mod's nine JSON reload listeners. Each listener now initializes `GSON` before `INSTANCE`, and the source validator checks that ordering. The built class bytecode must also show the `Gson` `putstatic` before listener singleton construction.

This proves that the prior null-Gson construction path is removed from the packaged bytecode. The faceted 512px Geo/atlas/face/hair contract is also source-validated. Neither check replaces a real Minecraft client regression test for world creation, texture rendering, culling, animation clipping, or GPU cost.

## Warnings

The clean compile has non-blocking deprecation warnings, primarily related to old `ResourceLocation` constructors and Forge context APIs. They do not prevent compilation or reobfuscation. They remain cleanup work, not runtime pass evidence.

## Explicit non-claims

This report does **not** claim any of the following:

```text
runClient launch success
Minecraft main-menu success
world creation success
entity spawn success
GeckoLib render success
animation blend success
Save/Load success
migration success
network packet runtime success
Will control-heart selection runtime success
Guardian Brace / Scout Signal runtime success
social director / paired reply / conversation runtime success
texture and social animation playback runtime success
faceted head/face texture rendering, culling, clipping, blink/gaze, or animation blend success
faceted character FPS/VRAM cost success
Natural straight-path gait and head-only glance runtime success
Visible hostile combat response, dialogue, plan proposal, and anti-freeze runtime success
Contextual animal/mob/biome/structure/fatigue trigger, animation, dialogue, and cooldown runtime success
Safe Recall anti-stacking runtime success in open/tight terrain
natural-world hostile interaction runtime success
boss compatibility success
power balance success
TPS/MSPT/Spark profile success
visual/audio QA success
playtest acceptance success
```

## Required next runtime sequence

1. Install Forge 1.20.1 / 47.4.22 and GeckoLib 4.7.1.1 in an isolated test profile.
2. Add the generated `riftcompanions-0.9.0-dev.jar`.
3. Launch a disposable new world and verify mod discovery before gameplay claims.
4. Run the Will behavior cases from `docs/BEHAVIOR_ACCEPTANCE_CATALOG.md` plus the channel, lift, boss, texture, social dialogue, expanded conversation, animation, behavior, story privacy, and Team Supply privacy catalogs.
5. Capture `latest.log`, crash reports, diagnostics export, Social tab state, and performance evidence for each defect.
