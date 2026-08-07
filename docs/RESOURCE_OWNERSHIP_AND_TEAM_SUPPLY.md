# Resource Ownership, Limited Inventory, Pickup, and Team Supply Contract

> See `docs/TEAM_SUPPLY_STATUS_UI_CONTRACT.md` for the bounded owner-only status projection and container privacy rules.

## Ownership classes

```text
PLAYER_PRIVATE
TEAM_APPROVED
WORLD_FREE
STRUCTURE_LOOT
PROTECTED
```

| Class | Companion behavior |
|---|---|
| `PLAYER_PRIVATE` | Never auto-taken. Includes owned ground items and every player inventory/container by default. |
| `TEAM_APPROVED` | Only explicit player-bound Team Supply container access, whitelist, cap, transaction, and journal record. |
| `WORLD_FREE` | Optional short-range ground-item assistance only, after policy, need, risk, ownership, and utility checks. |
| `STRUCTURE_LOOT` | Never opened or withdrawn automatically. Player opens/distributes it first. |
| `PROTECTED` | Never auto-taken: rare/protected category, player-owned item, unknown item, beds, portals, redstone/TNT/lava policy items, or player-defined protected area. |

No companion reads private chest contents, opens arbitrary containers, mines a resource block, or walks toward distant loot.

## Bounded personal inventory

The current companion inventory remains six bounded slots. It is not a mule inventory, remote storage system, or player chest replacement.

Current categories include:

```text
HEALING
BASIC_FOOD
PERSONAL_WEAPON
PERSONAL_AMMO
TEAM_EMERGENCY_SUPPLY
QUEST_OR_MEMORY_ITEM
BUILDING_MATERIAL_SAFE
RARE_OR_PROTECTED
UNKNOWN
JUNK_OR_LOW_PRIORITY
```

Unknown modded items remain `UNKNOWN` unless a safe support data pack classifies them. Rare/protected, weapon, building, unknown, and junk categories never qualify for automatic ground pickup.

## World-free pickup pipeline

`WorldFreePickupService` is disabled by default through both the inventory-assist feature flag and `AUTO_PICKUP_OFF` policy.

When explicitly enabled, each candidate follows:

```text
Perceive nearby item entity
-> classify
-> verify WORLD_FREE ownership
-> verify role need and utility score
-> verify no local hostile/sensitive-area risk
-> verify physical short range; no navigation toward item
-> transaction + item reservation
-> transfer exactly one item
-> record only meaningful approved pickup
```

A companion never chases an item beyond the configured physical radius, through lava, across a new chunk, or into a hostile crowd.

## Auto-pickup policies

```text
OFF
EMERGENCY_ONLY
ROLE_SUPPLIES
ASK_BEFORE_TAKING
TEAM_CHEST_ONLY
FULL_ASSISTED
```

- `OFF`: no pickup.
- `EMERGENCY_ONLY`: only nearby WORLD_FREE healing/basic food when player or companion is low health.
- `ROLE_SUPPLIES`: role whitelist and utility score.
- `ASK_BEFORE_TAKING`: request bark only; no automatic transfer.
- `TEAM_CHEST_ONLY`: no world pickup; explicit Team Supply only.
- `FULL_ASSISTED`: late/testing policy; still cannot take rare, unknown, private, or structure loot.

## Team Supply

Team Supply is disabled by default:

```text
team_supply_enabled = false
```

A player must bind one loaded container while standing at a safe HOME or REST anchor:

```mcfunction
/companions supply bind
```

Then a deliberate withdrawal can occur only when the player and companion are nearby, the anchor is in the same loaded dimension, the item is on the role whitelist, it is not rare/protected/unknown, the daily cap remains, and a transaction/anchor reservation succeeds:

```mcfunction
/companions supply withdraw guardian
/companions supply status scout
```

The system never scans or binds a container automatically. Withdrawal is one item at a time and creates a Team Journal memory record.

## Required runtime tests

- Nearby unowned healing item + low Guardian health + Emergency policy: exactly one safe pickup.
- Nearby diamond/TNT/lava bucket/unknown modded item: no automatic pickup.
- Owned player item on ground: no automatic pickup.
- Distant arrow behind lava: no navigation or pickup.
- Ordinary structure chest: no open/read/withdraw.
- Explicit bound Team Supply: whitelist/cap/transaction/journal success once.
- Disable pickup mid-session: no queued action remains.
- Save/load with item in companion inventory and Team Supply state: no duplication, loss, or clone.
