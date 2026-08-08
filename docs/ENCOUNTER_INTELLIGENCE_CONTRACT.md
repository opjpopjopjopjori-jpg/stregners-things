# Encounter Intelligence and Role Awareness Contract

## Purpose

Encounter intelligence turns visible biome, structure-plan, and threat evidence into a bounded context. It does not give companions global map knowledge, free mansion detection, hidden loot information, forced plans, or automatic dimension travel.

## Context pipeline

```text
Visible biome / visible threat / active player-approved structure plan
-> EncounterContextState
-> risk and recommended formation
-> optional role-specific dialogue mapping
-> bounded memory hook only at transition or risk escalation
```

The logical server owns this pipeline. The client receives a summary only.

## Role awareness

| Role | Primary encounter attention | Explicit limitation |
|---|---|---|
| Guardian | visible hostile pressure, team center, exit/retreat space | does not decide the player’s final entry/loot choice |
| Seer | visible Hive tag, observed clue confidence, anomaly pattern, active hostile pressure | no wall vision, x-ray, passive mob-farm control, or permanent monster ownership |
| Gifted | rescue-safe destination, local protection timing, energy/policy | no weather magic, permanent protection, or unsafe push |
| Scout | visible route/edge/high ground and formation spacing | no unloaded-chunk scout, ocean exploration, or fake path certainty |

## Built-in profiles

```text
Dense Forest
Mountain / Cliff
Desert
Ocean / Coast
Nether
End
Village
Dungeon
Mineshaft
Ruined Portal
Stronghold
Swarm
Ambusher
Ranged Threat
Explosive Threat
Controller / Mental Threat
Heavy
Boss
```

Each profile exposes observable signals, risk, suitable roles, forbidden assumptions, player choices, memory hooks, and dialogue category. Context dialogue comes from safe data mappings under:

```text
data/riftcompanions/encounter_dialogue/*.json
```

An invalid mapping file is ignored. It cannot create a new encounter, force dialogue, bypass a Safety Gate, or change entity ownership.

## Formation guidance

Encounter context may recommend, but never force, a formation:

```text
Mountain / Dungeon / Mineshaft / Stronghold -> CAVE
Nether / End -> RETREAT caution posture
Other calm profiles -> FOLLOW
Active combat still takes precedence -> COMBAT
```

The player can still issue Follow, Hold, Retreat, Cancel, and other bounded commands.

## No false knowledge rules

- Forest does not imply a mystery.
- Desert does not imply heat/thirst damage.
- Ocean does not imply boat intelligence.
- Ruined Portal does not imply known destination.
- Stronghold does not imply known portal location before visible discovery.
- Structure profiles inspect only player-visible loaded blocks.
- Threat profiles require visible target/line-of-sight evidence.

## Runtime matrix

- Forest daytime/no hostiles: context can update without forced retreat or spam.
- Forest night/crowd/no exit: risk escalates and safe retreat/defend options appear.
- Mountain: CAVE formation and navigation cliff rules; no unsafe compact push.
- Coast: companions do not begin long water travel.
- Nether/End: no automatic portal entry.
- Village: no chest/trade action; protection remains player-controlled.
- Dungeon/mineshaft: player controls blocks/loot; context does not invent unseen rooms.
- Every threat archetype: dialogue/formation difference is visible without granting automatic victory.
