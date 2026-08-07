# Contextual Interaction Director Contract

## Purpose

The Contextual Interaction Director gives companions authored local reactions to visible animals, a safely observed focused hostile, biome transitions, player-selected structure context, and low companion energy. It is a data-driven field-guide layer, not external AI and not a random chatter generator.

## Data source

```text
data/riftcompanions/contextual_interactions/vanilla_field_guide.json
```

The default authored multi-stage library contains 198 scene definitions across:

- 22 visible passive/utility animals;
- 23 focused hostile field-guide briefs;
- 23 biome notes;
- 11 player-selected/known structure notes;
- 1 fatigue/rest request cue.

Every Discovery/Revisit scene maps to an original English lead trigger, reply trigger, visual-only lead/reply action, bounded priority, and cooldown. Each scene has authored English lead/reply coverage for Guardian, Seer, Gifted, and Scout. This produces 1,584 contextual lines and brings the full default dialogue library to 2,134 lines without runtime text generation.

## Valid observation boundary

An interaction can be selected only from one of these facts:

| Source | Gate |
|---|---|
| Passive entity | Living, loaded, within local radius, visible to player, and in the authored registry |
| Focused hostile field guide | Existing visible perception cue, at safe distance, not currently targeting player/companion, and in registry |
| Biome | Current loaded player biome only |
| Structure | Existing player-selected visible perimeter assessment or current bounded encounter profile |
| Fatigue | Active nearby companion has low existing Energy while the local area is safe |

The director never reads a container, scans unloaded terrain, sees through walls, reads a mob inventory, manipulates a pet/villager/animal, controls a boat/horse, creates an entity, grants an item, alters loot, starts a forced plan, or changes player input/camera.

## Context animations

Each role has authored visual-only body clips:

```text
context_animal_greet
context_animal_observe
context_field_note
context_threat_brief
context_loot_note
context_biome_brief
context_structure_brief
context_rest_request
context_route_note
```

Face and secondary controllers keep expression/hair motion active underneath contextual body clips. Context action clips are suppressed under urgent combat, retreat, downed, recovery, or other high-priority states.

## Dialogue and plan rules

- One normal contextual line observes the existing global chat budget and per-trigger cooldown.
- Context is disabled in Safe Mode, active/pending plan state, nearby combat danger, low player health, lava/fire/fall risk, or ineligible companion state.
- Hostile information is educational and safety-focused; it does not auto-cast a power or order a kill.
- A structure note may describe a visible/player-selected site but never reveals hidden rooms, loot, coordinates, or future events.
- An optional field note can suggest safe investigation language, but it never creates a mandatory mission, reward, timer, progression lock, or autonomous path.
- Animal lines can appreciate or explain an animal, but companions never herd, tame, transport, mount, breed, or farm it.

## Performance and spam bounds

- Director cadence: 40 ticks.
- One global contextual delivery window per owner: 300 ticks.
- Per-group Discovery/Revisit cooldown: 3,600–9,600 ticks in the default pack.
- Session history: capped recent signatures plus 128 stage groups per owner; non-persistent and cleared at logout.
- Entity radius: 12 blocks, loaded local entities only.

## Runtime acceptance matrix

| Setup | Expected result |
|---|---|
| Visible cow in a safe field | One appropriate companion may greet/observe with a context animation and authored line; no animal movement/control occurs. |
| Distant visible skeleton not attacking | One field-guide note may explain range/cover/drop safety; no auto attack or loot path begins. |
| Zombie attacks player/companion | Threat Response Director owns urgent reaction; contextual trivia is suppressed. |
| Enter player-selected visible mineshaft | One structure note may discuss exit/route caution; no forced entry or hidden scan occurs. |
| Biome transition | One bounded biome note may occur after safety/cooldown gates. |
| Low Energy companion in a safe area | Companion can request a short safe rest; it does not force a rest, stop player input, or create a mission. |
| Repeated cow/biome visibility | Recent signature and per-definition cooldown prevent repeated chatter. |

This is source/build validated. Actual trigger timing, animation playback, perceived realism, frame cost, and player acceptance require integrated-client testing.
