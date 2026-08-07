# Team Supply Status UI and Privacy Contract

## Status

This is a source implemented owner-only status projection. Team Supply remains disabled by default and is not runtime tested. The UI does not make Team Supply safer by itself; all bind and withdraw operations remain server-owned in `TeamSupplyService`.

## What the client may receive

`TeamStatusSnapshot.TeamSupplyView` contains only:

```text
Feature enabled flag
Bound flag
Safe-base availability flag
One bounded English availability summary
Per-role withdrawals used today
Per-role daily cap
Per-role allow-listed item category names
```

## What the client never receives

```text
Bound container coordinates
Bound dimension ID
Container block type
Container slot count
Item stack IDs
Item counts
NBT
Container contents
Private chest data
Structure loot data
Any automatic discovery result
```

The Team Journal Guide tab shows only the availability summary. It does not render a chest inventory, remote item list, map point, or direct withdraw button.

## Server boundary

The existing server path remains mandatory:

```text
Player explicitly binds a looked-at loaded container
-> safe HOME or REST anchor check
-> same-dimension/range/loaded validation
-> role is loaded and near the base
-> category whitelist + daily cap
-> transaction + anchor reservation
-> one item transfer
-> bounded memory record
```

No status packet can skip, infer, or pre-approve one of these checks.

## UI language

The status summary uses conservative English messages such as:

```text
Team Supply is disabled in world settings.
No Team Supply container is bound.
Team Supply is bound but requires a safe HOME or REST anchor.
Team Supply is bound; withdrawals remain role-whitelisted and capped.
```

The UI never promises that a specific item exists in the container.

## Network budget

The projection is bounded to one line plus up to four role cap rows. It uses no position or inventory serialization. This view introduced protocol `25`; the current custom protocol is `32` after later Will control, natural-world, and Social Director packet revisions.

## Required runtime tests

1. Disabled Team Supply: UI reveals no container data and says disabled.
2. Bound Team Supply outside a safe base: UI says unavailable and exposes no coordinate.
3. Bound same-dimension safe-base supply: UI shows only role cap/category metadata.
4. Fill a container with rare, unknown, protected, and allowed items: UI must not reveal contents.
5. Withdraw once: used/cap status updates without stack, slot, or coordinate leakage.
6. Change dimension, unload container chunk, or destroy/change container: UI stays bounded and the server rejects withdrawal safely.
7. Save/Load Team Supply state: status remains coherent and no item is duplicated.

## Non-claims

This is not a chest screen, remote inventory system, transport network, auto-loot dashboard, or team storage permission. It does not replace runtime transaction, privacy, or exploit testing.
