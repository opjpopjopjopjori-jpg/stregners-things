# Story Graph, Mystery Board, and Promise Contract

> See `docs/STORY_JOURNAL_PRESENTATION_CONTRACT.md` for the bounded owner-only Team Journal projection and packet safety rules.

## Optional story boundary

The story is alternate Minecraft continuity. It never starts a world timer, forces player movement, opens a portal, spawns a boss for drama, or blocks ordinary survival progression.

`StoryState` now owns three bounded layers:

```text
Story chapter status
Mystery Board clues
Optional companion promises
```

## Mystery Board

A clue is created only from player-visible evidence:

- player-authored `INVESTIGATE` annotation;
- visible Hive-linked observation;
- future explicit supported encounter adapters.

Each clue carries a stable ID, English summary, confidence, day, and deferred flag. It cannot reveal a structure, map coordinates, chunk contents, or future event.

Commands:

```mcfunction
/companions mystery status
/companions mystery defer <id>
```

Deferring a clue creates no penalty, timer, relationship loss, or lost progression.

## Optional promises

Only one open promise can exist at a time. A promise is offered only at a safe base and from real state, such as:

```text
Guardian: establish a Guard Post.
Seer: compare two observed clues.
Scout: mark a return route.
Gifted: test a protective exit plan.
```

A promise has a completion key tied to a server-observed event. It grants a memory and minor transparent relationship clarity only; it does not give loot, damage, permanent speed, forced travel, or core-content access.

Commands:

```mcfunction
/companions promise status
/companions promise accept
/companions promise defer
```

## Completion safety

Promise completion is checked only after the corresponding player action succeeds:

```text
ANCHOR_GUARD_POST
ANNOTATION_INVESTIGATE
ANNOTATION_SAFE_ROUTE
ABILITY_GIFTED_SHIELD
```

A player can ignore or defer any promise forever. Sandbox progression remains intact.

## Required runtime tests

- Mark an investigate location: one clue appears without hidden structure data.
- Repeat same mark/reload: clue does not duplicate.
- Offer, accept, defer, and complete each promise.
- Ignore a promise for many Minecraft days: no penalty.
- Complete a promise: one memory/relationship event only.
- Disable Story Graph: Follow, plans, inventory, journal, and base systems remain functional.
