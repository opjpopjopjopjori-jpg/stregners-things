# 0.4.1-dev Integrated World-Creation Crash Hotfix

## Reported symptom

Creating an integrated single-player world could crash during Minecraft's initial JSON resource reload with a null `Gson` exception from `GsonHelper`.

## Root cause

Nine Rift Companions classes extend `SimpleJsonResourceReloadListener`. Their source declared `INSTANCE` before `GSON`:

```java
public static final Listener INSTANCE = new Listener();
private static final Gson GSON = new Gson();
```

Java initializes static fields in declaration order. The singleton constructor therefore called `super(GSON, ...)` while `GSON` was still null. Minecraft retained that null value until its first integrated-server data reload.

## Correction

Every affected class now declares `GSON` before `INSTANCE`:

```java
private static final Gson GSON = new Gson();
public static final Listener INSTANCE = new Listener();
```

Affected reload domains: dialogue, compatibility packs, intentions, item classifications, threat profiles, structure overrides, mental effects, encounter dialogue, and behavior profiles.

## Regression guard

`tools/validate_source_tree.py` now fails if a `SimpleJsonResourceReloadListener` singleton precedes its `GSON` field or no longer passes that field to the superclass. The packaged class files are also checked with `javap` before handoff.

## Test boundary

This eliminates the identified null-Gson path. It does not claim successful gameplay, rendering, save/load, animation, audio, social behavior, performance, or broad-modpack compatibility. A clean Forge 47.4.22 / GeckoLib Forge 4.7.1.1 profile must still create a disposable world to establish the next runtime result.
