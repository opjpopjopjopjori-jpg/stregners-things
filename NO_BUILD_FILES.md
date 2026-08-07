# Build Workspace Status

> Historical note: the project originally followed an owner-requested no-builder policy. The owner explicitly approved creation of a real Forge build workspace on 2026-08-06. This file remains at its original path so older documentation links stay valid.

## Present build scaffold

The repository now intentionally contains:

- `build.gradle`
- `settings.gradle`
- `gradle.properties`
- `gradlew` / `gradlew.bat`
- `gradle/wrapper/*`
- ForgeGradle 6 configuration for Minecraft 1.20.1 / Forge 47.4.22
- Java 17 toolchain target
- GeckoLib Forge dependency declaration for 1.20.1 / 4.7.1.1

The build cache and downloaded JDK/dependencies are held outside the source tree cache policy. Generated `build/`, `.gradle/`, `run/`, `run-data/`, and IDE output are not source deliverables and must not be committed to a clean source archive.

## Verified build scope

A clean command completed successfully in the current workspace:

```bash
./gradlew clean build --no-daemon --max-workers=1
```

This proves Java compilation, resource processing, jar packaging, and Forge reobfuscation for the current source version. It does **not** prove that Minecraft launched, a world loaded, GeckoLib rendered, Save/Load behaved correctly, or any runtime playtest passed.

## Runtime dependency note

The compiled development JAR does not bundle GeckoLib. A normal Forge 1.20.1 client installation must also include a compatible GeckoLib Forge JAR before attempting runtime playtests.

## Next technical gate

1. Create an isolated development test world.
2. Run the client through `./gradlew runClient` on a machine with sufficient graphics/runtime resources.
3. Follow `docs/BUILD_VALIDATION_REPORT.md`, `docs/TEST_PLAN.md`, and the chaos/acceptance matrices.
4. Do not call the JAR stable until the runtime gates pass.
