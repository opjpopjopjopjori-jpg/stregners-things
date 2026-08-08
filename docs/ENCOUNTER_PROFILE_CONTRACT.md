# Encounter Profile Contract

## Purpose

Encounter Profiles prevent every dangerous place from producing the same generic reaction. They describe observable context and role priorities; they never generate hidden structures, force worldgen, or grant x-ray knowledge.

## Profile fields

Each profile has:

- ID and type: biome, structure, threat, or journey.
- Observable signals.
- Primary and secondary risk.
- Suitable roles.
- Forbidden assumptions.
- Suggested player choices.
- Entry and exit conditions.
- Memory hooks and dialogue categories.
- Adapter requirement flag.

## Built-in biome profiles

- `dense_forest`
- `mountain_cliff`
- `desert`
- `ocean_coast`
- `nether`
- `end`
- safe `unknown_biome:*` fallback

No profile invents vanilla heat, thirst, ocean boat intelligence, mansion discovery, or unseen structure knowledge.

## Built-in structure profiles

A bounded server-side perimeter scan classifies visible evidence only:

- `village` from a visible bell.
- `dungeon` from a visible spawner.
- `mineshaft` from visible cobweb evidence.
- `ruined_portal` from visible obsidian/crying obsidian.
- `stronghold` from a visible End Portal Frame.
- `unknown_structure` fallback.

Player approval remains required for Structure Entry plans. No profile opens containers, activates portals, or changes blocks.

## Threat archetypes

- `SWARM`
- `AMBUSHER`
- `RANGED_THREAT`
- `EXPLOSIVE_THREAT`
- `CONTROLLER_MENTAL`
- `HEAVY`
- `BOSS`
- `UNKNOWN`

Custom mobs use `EncounterAdapterRegistry`. Missing, failed, or incompatible packs return Unknown/Caution behavior rather than crashing or pretending to know mechanics.

`data/riftcompanions/threat_profiles/` may add an advisory archetype, positive caution bias, observed tell, and counterplay vocabulary for a known entity type. `data/riftcompanions/structure_overrides/` may classify only an already observed block with an allow-listed profile and positive caution bias. Neither format may lower modded Unknown/Caution, find unseen structures, load chunks, grant a power target, or bypass the player approval path.

## Context and dialogue layer

`EncounterContextService` stores only the current observed profile, bounded risk, focus, and recommended formation. It announces an encounter only after a profile transition or meaningful risk escalation and only through a valid role/profile dialogue mapping. See `docs/ENCOUNTER_INTELLIGENCE_CONTRACT.md`.

## Variety rule

A recently successful plan type has a short cooldown. The Team Director does not repeat the same plan without a new reason, while critical health events can still override normal variety rules.

## Required runtime tests

- Dense forest at daytime with no threats: no forced retreat.
- Dense forest at night with crowd/no exit: retreat or player-approved defense.
- Mountain cliff: formation avoids player overlap and unsafe slot placement.
- Ocean/coast: no autonomous long water travel.
- Structure scan: no chunk load or hidden-room knowledge.
- Custom adapter failure: Unknown/Caution fallback, no crash.
