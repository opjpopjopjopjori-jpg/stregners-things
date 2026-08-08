# Story Journal, Mystery Board, and Promise Presentation Contract

## Status

This document describes source implemented owner-only Journal presentation. It does not claim that the UI has been viewed in a Forge runtime. The story remains optional, English-only, offline/single-player only, and unable to block normal Minecraft survival progression.

## Presentation boundary

`TeamStatusSnapshot.StoryJournalView` projects only bounded player-owned story facts:

```text
Visible chapter titles/statuses, with locked chapters projected as `Undiscovered Chapter`
Up to four most recent visible-evidence clue summaries
One currently open optional promise
Total clue/promise counts
One recent memory summary already present in the Journal
```

The packet deliberately excludes:

```text
Raw clue IDs
Block coordinates
Dimension coordinates
Structure bounding boxes
Chest contents
Unloaded chunk data
Future event data
Hidden mob internals
Any automatic route or quest target
```

The client can render the facts but cannot accept a promise, defer a clue, complete a story node, unlock a role, or change a chapter. All such state changes remain server commands/services.

## Journal layout

The existing `Journal` tab now presents:

1. Optional active/available/deferred/completed chapter summary.
2. One current promise with role, state, and bounded English summary.
3. Mystery Board records ordered by recent observed evidence.
4. Deferred status for a clue, without punishment styling.
5. One recent team memory as a compact historical link.
6. English command reminders for story, mystery, and promise status.

It intentionally does not create a forced quest screen, minimap marker, countdown, progress bar, camera move, path arrow, or hard navigation target.

## Chapter status vocabulary

| Status | Player-facing meaning |
|---|---|
| `LOCKED` | Optional context is not yet available; normal survival remains unaffected. |
| `AVAILABLE` | Optional context may be started later. |
| `ACTIVE` | Optional context is currently being tracked as a Journal fact. |
| `DEFERRED` | The player chose to leave it for later. |
| `COMPLETED` | A bounded player-observed milestone was completed. |

The Journal never presents `LOCKED` chapter details as undiscovered world knowledge.

## Mystery clue rules

A clue shown in the Journal came only from a permitted source already enforced by `StoryService`:

```text
Player-authored INVESTIGATE annotation
Visible Hive-linked observation
Future explicit supported encounter adapter
```

The summary remains evidence language, not certainty. A deferred clue remains readable but cannot become a penalty, timer, relationship loss, or lost loot condition.

## Promise rules

Only one promise may be open at a time. The Journal shows its current `OFFERED`, `ACCEPTED`, or `DEFERRED` status but does not provide a reward claim button.

Commands remain explicit:

```mcfunction
/companions story status
/companions mystery status
/companions mystery defer <id>
/companions promise status
/companions promise accept
/companions promise defer
```

A promise completion is still checked only by a matching server-observed completion key. The UI never converts text, animation, a click, or a client packet into a completion.

## Packet budget

The source uses bounded fields:

```text
Story chapters: at most StoryChapter.values().length
Mystery clues: at most 4 projected entries
Clue summary: at most 160 characters
Promise summary: at most 160 characters
No coordinate or raw clue-ID string
```

The custom network protocol is `24` because this bounded projection changes the owner status packet layout.

## Acceptance matrix

The future Forge runtime test must confirm:

1. Story tab shows no hidden structure coordinate or raw clue identifier.
2. More than four clues keeps the packet/UI bounded and shows recent evidence only.
3. Deferred clue remains visible and unpunished after Save/Load.
4. Accepted/deferred/completed promise display matches server state after Save/Load.
5. Player can use all normal survival systems while every chapter is locked, deferred, or disabled.
6. Story Graph disabled leaves the Journal stable and shows no fabricated completion.
7. Small GUI scale does not overlap command controls or hide plan safety information.
8. Packet decoding rejects no malformed/unbounded list length in a real runtime test.

## Non-claims

This is not a full Story Graph editor, romance system, quest tracker, cinematic replay, map waypoint system, or long-form narrative simulation. It is a bounded presentation layer over facts the server already owns.
