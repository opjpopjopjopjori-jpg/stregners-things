# Playtest Campaign and Acceptance Matrix

## Status

This is a local source-supported campaign plan. The Forge 1.20.1 workspace builds cleanly, but this plan is not proof of runtime success until real playtests happen.

## Campaign stages

| Stage | Focus | Minimum success condition |
|---|---|---|
| Test A — Technical Sandbox | One companion: spawn, follow, hold, recall, save/load | No duplicate, permanent stuck state, or crash |
| Test B — Guardian Experience | Night, cave, creeper, retreat, base return | Player understands the safety reason and is not blocked |
| Test C — Duo Experience | All six pairs in suitable context | Tester can describe pair difference without damage ranking only |
| Test D — Memory Experience | Multiple sessions and revisited changed place | Team memory is evidence-based and does not invent world knowledge |
| Test E — Stress/Chaos | Crowd, path failure, save/load, cancel, low-FPS simulation | Safe fallback; no endless loop, duplicate, or control loss |
| Test F — Series Feel | Base -> discovery -> danger -> return | Tester remembers plan, choice, consequence, and return |

## Local acceptance entry

The owner can store a local 1–5 rating in the world data:

```mcfunction
/companions playtest rate clarity 5
/companions playtest rate usefulness 4
/companions playtest rate personality 4
/companions playtest rate pace 4
/companions playtest rate safety 5
/companions playtest rate memory 4
/companions playtest rate series_feel 4
/companions playtest status
```

Axes:

```text
CLARITY
USEFULNESS
PERSONALITY
PACE
SAFETY
MEMORY
SERIES_FEEL
```

The Go rule is explicit: if any rated axis averages below `3/5`, new content should remain on hold until that axis is corrected. The telemetry is local-only, has no network upload, and appears in the local diagnostics export.

## Required notes outside the score

For each campaign run, record manually:

- seed and game/mod versions;
- selected duo and enabled feature flags;
- scenario/encounter profile;
- any crash, duplicate, loss, player-control issue, false perception, or unsafe block interaction;
- TPS/MSPT/Spark capture after the future test workspace exists;
- whether a companion acted outside role/persona;
- whether silence, consequence, memory, and dialogue felt appropriate.
