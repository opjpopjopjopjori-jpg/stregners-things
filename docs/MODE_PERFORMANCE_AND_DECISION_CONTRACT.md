# Gameplay Mode, Performance Tier, and Team Decision Contract

## Mode policy

`GameModePolicyService` defines explicit behavior:

| Mode | Companion behavior |
|---|---|
| Survival / Adventure | Full bounded survival, plans, threats, and role behavior |
| Creative | Follow/chat/base behavior; no automatic survival pressure plans |
| Peaceful | Exploration/base/context behavior; no automatic combat planning |
| Spectator | Companions hold safely and do not attempt to follow or execute plans |
| Hardcore | Story Downed remains default; no companion permadeath is silently enabled |

Commands are never interpreted as player wrongdoing or “cheating.”

`VanillaRulePolicyService` additionally projects Mob Griefing, Keep Inventory, Daylight Cycle, and Hardcore facts without granting a world-edit exception. Defend planning is paused in Creative, Spectator, and Peaceful; Follow, Recall, Journal, and safe exploration guidance remain available.

See `docs/WORLD_RULES_ONBOARDING_ACCESSIBILITY_CONTRACT.md` for the full rule, optional tutorial, and accessibility contract.

## Performance tiers

```text
LIGHT
STANDARD
CINEMATIC
```

- `LIGHT`: slower optional context/perception cadence, no optional inventory assist, no ambient scenes.
- `STANDARD`: default cadence and up to the current configured two-companion source limit; that limit remains runtime unverified.
- `CINEMATIC`: presentation cadence can increase, but it does not raise the active squad limit without runtime proof.

No external performance mod is required for core correctness. When the opt-in common developer diagnostic flag is enabled, a bounded in-memory monitor exposes sampled AI, navigation, Team Director, and dialogue timing through the owner-only F8 developer overlay and local report. It is not a TPS/MSPT or Spark substitute.

See `docs/OPERATIONAL_SAFETY_AND_OBSERVABILITY.md` for cadence, pause/exit, baseline-world, and developer-observability contracts.

## Decision ownership

`TeamDecisionService` maps an event to one bounded decision domain:

```text
SURVIVAL_CRISIS      -> Guardian safety lead
STRUCTURE_OR_MYSTERY -> Seer information lead / Guardian safety lead
ROUTE_OR_SCOUT       -> Scout information lead / Guardian safety lead
POWER_DECISION       -> Gifted capability lead / Guardian safety lead
CALM_OR_BASE         -> no forced major plan
```

The system records an evidence key and has a discussion cooldown. It does not create competing active plans, force the player to accept a plan, or let a role override an emergency Safety Gate.

## Required runtime tests

- Creative: companions follow without survival-plan spam.
- Spectator: companions pause safely and resume follow after exit.
- Peaceful: no combat plan draft while base/memory systems stay available.
- LIGHT: optional scans/scenes reduce without breaking HUD, rescue, follow, or recall.
- Simultaneous P0/P1/P3 events: one survival decision lead, no conflicting team dialogue.
- Route/mystery/power event: correct information lead speaks once and player remains final decision maker.
