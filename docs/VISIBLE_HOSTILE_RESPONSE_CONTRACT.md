# Visible Hostile Response and Anti-Freeze Contract

## Goal

When a visible, loaded hostile mob attacks the player or an active companion, companions must not remain frozen under a Follow or formation navigation order. The team reacts through bounded local combat targeting, realistic authored dialogue, player-respecting plan proposals, and an existing Retreat safety path.

## Detection boundary

`ThreatResponseDirector` runs on the existing bounded Team Director cadence only when normal Survival threat scanning is permitted.

A hostile is relevant only when it is:

1. Alive and already loaded within the local response radius.
2. Directly targeting the player or an active companion, or visible to the player.
3. A normal Minecraft/compatible hostile; the system adds no entity, spawn, worldgen, or wave.

The director does not inspect containers, scan unloaded terrain, use x-ray, acquire hidden targets, force chunk loads, or generate enemies.

## Immediate response

For one to three relevant threats:

- Combat-capable active companions clear stale formation slots.
- Guardian, Gifted, and Scout receive a nearby bounded threat target when their existing combat safety gate allows it.
- They enter `FIGHTING`, allowing `CompanionMeleeGoal` to own local pursuit/attack rather than Follow overwriting navigation each tick.
- Seer receives a visual notice and may contribute authored observation dialogue, but never auto-casts a power.
- One authored `hostile_contact` dialogue lead may play; a second active companion may give one paired reply in the same chat budget.

## Crowd and retreat behavior

For three or more relevant threats, the system may draft a **Defend** proposal. The player must still accept or decline it; companions never force a combat plan.

For critical player health, extreme local crowd pressure, or a critically injured active companion, the existing bounded Retreat path may start. It uses existing safe-waypoint/formation rules and clear `retreat_start` dialogue; it does not delete threats, teleport the player, or grant invulnerability.

## Anti-freeze rules

- `FIGHTING` excludes formation-slot assignment.
- `FIGHTING` ignores stale Follow navigation while a valid nearby target remains.
- A target outside the configured pursuit radius clears through the normal Follow fallback rather than leaving the companion frozen.
- Melee may orient the body toward the confirmed target only during actual combat. Ordinary path attention remains head-only through the natural gait system.
- The response clears when no valid local target remains.

## Dialogue boundary

New original English trigger families:

```text
hostile_contact
hostile_crowd
```

They contain role-specific tactical information: line, exit, spacing, pressure, and route. They do not claim hidden knowledge, issue a forced player command, create a new power, or overwhelm chat.

## Runtime matrix

| Setup | Expected result |
|---|---|
| One zombie attacks player | Active combat-capable companion targets/approaches/attacks or takes valid local combat posture; no frozen Follow path. |
| One zombie attacks a companion | Targeted companion and nearby teammate react; stale formation does not overwrite combat. |
| Two or three visible zombies | One bounded contact/crowd exchange; local combat targets stay within pursuit radius. |
| Five zombies or player at low health | Existing Retreat safety path can begin; no forced Defend approval. |
| Safe Mode / Peaceful / Creative / Spectator | Threat response respects existing policy and does not force combat. |
| Zombie dies or leaves local range | FIGHTING clears to Follow without stuck target/navigation state. |
| Seer active | Notice/observation only unless the player explicitly requests an existing Seer power. |

This contract is source/build validated. Actual combat movement, target selection, dialogue timing, plan UI, TPS, and user acceptance require real integrated-client testing.
