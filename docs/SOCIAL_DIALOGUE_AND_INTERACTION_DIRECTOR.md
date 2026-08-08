# Social Dialogue, Interaction, and Animation Director

## Purpose

This system turns companion dialogue and social animation into a bounded, server-owned interaction layer. It enriches personality and world response without creating a chatbot, external AI dependency, forced cutscene, camera lock, player-input change, hidden-world scan, inventory inspection, or automatic gameplay action.

```text
Visible local cue
-> server safety gate
-> authored lead dialogue
-> social visual action
-> bounded paired reply
-> cooldown and normal chat budget
```

## Social cue sources

The director considers only local, player-visible facts:

| Cue | Source boundary |
|---|---|
| Campfire | A nearby loaded campfire block, or player-selected campfire interaction |
| Weather | Current local rain or thunder only |
| Horizon | Current local dawn/dusk time window only |
| Base | Existing safe HOME/REST context only |
| Work | Player-selected crafting, furnace, anvil, loom, cartography, or other visible work block only |
| Cave | Current bounded dungeon, mineshaft, or stronghold encounter profile only |
| Village | Current bounded village encounter profile only |
| Travel | Current player movement after all local safety gates pass |
| Calm | No nearby hostile, no active danger/plan, and normal local movement state |

Containers, chests, inventories, hidden structures, remote chunks, block contents, player chat, and unseen entities are never read or used as dialogue input.

## Social safety gate

A lead/reply exchange can start only when:

```text
Companion social feature flag is enabled
Safe Mode is inactive
No active or pending team plan
No nearby loaded hostile within the local safety radius
No critical player hazard or low-health boundary
No DOWNED, FIGHTING, RETREATING, STUCK_RECOVERY, EXHAUSTED, RECOVERING, or RESTING speaker
Exactly two nearby active companions can participate
The pair is within bounded local social distance
The cooldown and normal dialogue budget permit the lead line
```

The reply is scheduled only if the lead line was actually delivered. A paired reply shares the lead line's normal chat window; it cannot open extra chatter capacity. Any danger, Safe Mode, plan, distance failure, logout, or unavailable speaker cancels the pending reply safely.

## Animation direction

The social director uses appended display-only `CompanionAction` values:

```text
SOCIAL_LISTEN
SOCIAL_POINT
SOCIAL_REASSURE
SOCIAL_GEAR_CHECK
SOCIAL_OBSERVE
SOCIAL_CAMPFIRE
SOCIAL_WEATHER
SOCIAL_HORIZON
SOCIAL_BASE
SOCIAL_WORK
SOCIAL_CAVE
SOCIAL_VILLAGE
SOCIAL_TRAVEL
SOCIAL_CALM
```

All four companion animation files now include a social/world clip family plus two role-specific clips:

| Role | Role-specific clips |
|---|---|
| Seer | `social_trace_signal`, `social_reflect` |
| Guardian | `social_perimeter_scan`, `social_radio_check` |
| Gifted | `social_grounding_pose`, `social_breathe` |
| Scout | `social_map_read`, `social_route_confirm` |

The base social family is interruptible. Any urgent combat, retreat, downed, or recovery visual suppresses it. Social clips do not apply damage, movement, effects, inventory changes, blocks, plans, memory writes, power casts, or AI state changes.

## Authored dialogue expansion

The English dialogue library includes paired lead/reply content for nine social cue families across every role:

```text
social_campfire_lead / social_campfire_reply
social_weather_lead / social_weather_reply
social_horizon_lead / social_horizon_reply
social_base_lead / social_base_reply
social_work_lead / social_work_reply
social_cave_lead / social_cave_reply
social_village_lead / social_village_reply
social_travel_lead / social_travel_reply
social_calm_lead / social_calm_reply
```

The lines are authored English project text with role-specific voice direction:

```text
Guardian: practical safety, stable route, concrete next step
Seer: evidence discipline, observation, uncertainty without panic
Gifted: grounded care, consent, emotional restraint, protection timing
Scout: terrain, visibility, route clarity, practical preparation
```

No actor voice imitation, copied dialogue, show quotation, external language model, API key, internet request, or runtime text generation is used.

## Player-led conversation expansion

The Conversation screen now includes bounded contextual topics:

```text
Read the world
Team check
Last encounter
Check in
```

Replies use only local weather/light, current plan, visible team state, bounded memory, energy/focus/strain, and recorded encounter facts. They do not claim hidden knowledge, read containers, reveal coordinates, or force a player choice.

## Runtime acceptance matrix

1. Lead and reply appear only when both speakers are loaded, safe, nearby, and dialogue budget allows.
2. A hostile, active plan, Safe Mode, downed state, retreat, low health, logout, or distance break cancels a pending reply.
3. Player-selected campfire/bed/work-block interaction is the only block-derived social cue; containers are ignored.
4. Every social clip is interruptible and does not modify gameplay state.
5. The paired reply does not create a third normal-chat budget window.
6. The Conversation screen remains usable with all expanded topics at normal and small GUI sizes.
7. Low Effects, muted companion cues, CRITICAL_ONLY, and high-contrast HUD leave a readable text/state fallback.
8. No camera lock, cutscene, input freeze, external AI, copied voice, or copied show dialogue occurs.

## Non-claims

Static source/resource validation confirms the contracts and clips exist. Dialogue pacing, GeckoLib blend quality, social timing, player acceptance, UI layout, performance, and real world compatibility remain unverified until actual client testing occurs.
