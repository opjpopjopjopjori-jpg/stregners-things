# Duo Dynamics, Companion Arcs, and Optional Intentions

## Active duo contract

The tactical team supports up to two active companions. Selection is available in the Team Journal’s **Squad** tab or through:

```mcfunction
/companions squad select guardian seer
```

The server accepts a selection only at a quiet HOME or REST anchor, with no nearby hostile crowd, no active/pending plan, and no Downed companion. It validates role IDs server-side. A client click cannot switch a squad by itself.

When a switch needs to free a slot, the outgoing loaded companion enters a server-owned `RESTING` snapshot through a transaction path. The live entity discards, so it cannot keep a chunk loaded or act off-screen. The bounded snapshot preserves personal inventory, energy, strain/focus, health, combat preference, and cooldown state; it excludes navigation, targets, world position, rescue state, and every player container.

A RESTING snapshot can restore only at a validated safe HOME/REST base in its original dimension. It is not remote storage or a cross-dimension inventory exploit. A normal explicit dismissal consumes that snapshot and returns its bounded items exactly once to the owner. If restore validation fails, the switch stops in a player-controlled safe fallback rather than manufacturing an entity or item.

## Pair profiles

| Pair | Identity | Best contexts | Explicit blind spot |
|---|---|---|---|
| Guardian + Seer | Safety and Mystery | Caves, unknown structures, careful investigation | Less direct damage and fewer route alternatives |
| Guardian + Gifted | Protection and Rescue | Defence, emergencies, controlled retreats | Less anomaly evidence and fewer route alternatives |
| Guardian + Scout | Guardian and Scout | Travel, mountains, forests, practical survival | No Hive sensing or power rescue |
| Seer + Gifted | Anomaly and Power | Supported Hive and mystery content | No Guardian frontline |
| Seer + Scout | Observation and Escape | Unknown biomes, clues, route testing | Less direct protection and no power rescue |
| Gifted + Scout | Mobility and Recovery | Routes, flexible exploration, emergency exits | No Guardian hold line or deep anomaly expertise |

A synergy is a cooldown-bound readable note, never a damage combo. Current examples connect a Guardian Shield window, a Seer disruption route window, a Scout retreat marker, or an evidence-supported protected entry. They do not grant automatic casting, boss control, loot multiplication, or invulnerability.

## Optional arcs

Four bounded arc trackers live in `ArcState`:

```text
Guardian — The Safe Way Back
Seer — The Pattern That Changes
Gifted — Choice, Not Weapon
Scout — A Route Worth Taking
```

They record observed milestones such as a successful retreat, first safe return HOME, first plan drafted from a safe base, an actual annotation, a rescue, or a safe route. Important journal milestones flow through an idempotent transaction key so a reload cannot write the same first-time memory twice.

They do not use kill counts, real-time deadlines, forced quests, permanent damage bonuses, or an unavoidable failure path. Each source milestone adds a reflection memory at most; it does not gate Minecraft progression.

## Intentions, not repetitive quests

An intention can appear only when the team is at a safe base, no plan is active, no other intention is open, and the feature flag is enabled. It has context, a voluntary action, a deferral path, a server-observed completion key, a memory reward, and no material loot reward.

Current data-driven examples:

```text
Guardian: set a Guard Post before a risky trip.
Seer: compare player-visible investigation markers.
Gifted: test Shield clarity at a player-chosen narrow entry.
Scout: test a higher route and mark the return.
```

Use the commands only when desired:

```mcfunction
/companions intention status
/companions intention accept
/companions intention defer
```

Ignoring or deferring an intention has no timer, no penalty, and no progression lock.
