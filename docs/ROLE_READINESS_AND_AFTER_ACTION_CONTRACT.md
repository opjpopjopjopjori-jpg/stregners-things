# Gifted Readiness, Guardian Review, and Protected-Bystander Contract

## Scope

This source milestone deepens two role-specific systems without turning either role into an autonomous controller:

- Gifted readiness and policy-aware suggestions;
- Guardian factual after-action review cards.

Both systems are server-owned, bounded, English-only, and source implemented. Their runtime behavior remains unverified until a Forge 1.20.1 test workspace exists.

## Gifted readiness

`GiftedBehaviorState` persists a compact readiness band, readable reason code, and last suggestion cadence. It is not a psychological meter, relationship score, or permission grant.

| Band | Meaning | Automatic cast? |
|---|---|---|
| `READY` | Energy and core ability cooldowns support a bounded suggestion | Never |
| `CAUTION` | At least one core ability is cooling down | Never |
| `LIMITED` | Energy is below the normal comfort band | Never |
| `EXHAUSTED` | Energy is too low for a major power | Never |
| `RECOVERING` | Existing recovery/exhaustion state is active | Never |
| `DOWNED` | Gifted is down and cannot suggest or act | Never |

The state is saved in Team Blackboard data version 16 and defaults safely on migration.

### Suggestion policy

`GiftedBehaviorService` runs at a bounded performance-tier-scaled cadence. It can send a short original English suggestion only when all of the following are true:

- the Gifted role is loaded and the role feature is enabled;
- the current readiness band allows a meaningful suggestion;
- the configured/personal power policy permits that category of suggestion;
- a local bounded event creates a real opening, such as a downed teammate or a nearby hostile crowd;
- the role-specific suggestion cooldown has elapsed.

The service does **not** call `AbilityService.cast`. A suggestion never spends Energy, starts a cooldown, applies a shield, moves an entity, changes a block, or overrides the player.

Policy behavior is intentionally conservative:

| Policy | Suggestion behavior |
|---|---|
| `OFF` | No power suggestion |
| `RESCUE_ONLY` | Rescue-context suggestion only |
| `ASK_FIRST` | May offer a bounded opening; the player still issues the command |
| `EMERGENCY_ONLY` | Suggests only for a local immediate emergency |
| `ALLOWED` | May suggest a bounded opening; still no auto-cast |

## Push safety boundary

Gifted Push is a velocity-changing action and now validates before consuming Energy:

1. valid player intent and world policy;
2. active Gifted role, readiness, cooldown, and Energy;
3. visible local monster targets that are not protected;
4. active combat context;
5. sensitive annotation boundary;
6. protected-bystander boundary;
7. only then Energy, cooldown, animation, velocity, particles, and dialogue.

Push rejects with `PUSH_BLOCKED_NEAR_PROTECTED_BYSTANDER` when the local impact boundary contains a protected non-target, including a companion, villager, animal/pet, protected entity tag, or another player. The active owner and casting Gifted are excluded from that boundary so the owner can still request a defensive escape opening; no other bystander is made collateral.

The source uses only loaded local entities. It does not search distant chunks or infer a hidden civilian.

## Shield safety boundary

Shield is owner-only. It now validates that the active Gifted is close enough to the owner before Energy is consumed. The shield is never spread to villagers, pets, other companions, or nearby entities. If a protected boundary is nearby, the server returns an owner-only diagnostic code; it does not expand the effect.

Shield remains protection rather than invulnerability. It does not create an unattended farm, block edit, forced camera behavior, or permanent defense state.

## Guardian after-action review

`GuardianReviewState` stores one compact factual review:

- plan type;
- terminal status;
- short English summary;
- readable reason code;
- game time recorded.

The review is recorded when a plan is accepted into a terminal result, cancelled/declined, fails, becomes `FAILED_SAFE`, or is invalidated during load recovery. It is visible through the Team Journal snapshot and local diagnostic export.

A review has no reward, penalty, trust manipulation, hidden moral test, forced dialogue, or plan restart. It explains what happened once and returns control to the player.

| Result | Review tone |
|---|---|
| `SUCCEEDED` | The bounded objective was reached; remember route/conditions |
| `CANCELLED` | The player decision was respected; return to Follow |
| `ABORTED` | The plan ended before completion; return to simple formation |
| `FAILED_SAFE` | The target/route/condition became invalid; safe cancellation was correct |
| `FAILED` | The route/conditions did not hold; reassess before repeating |

## Guardian practical boundary

Guardian remains the survival safety lead for visible pressure:

- player health and nearby threats;
- team separation;
- known exit/last safe waypoint;
- protected-area explosive threat;
- rescue guard posture;
- player-approved Retreat, Defend, and Structure Entry plans.

Guardian does not acquire hidden world knowledge, chase loot, force a player to retreat in a non-emergency, start a second active plan, open containers, use TNT, build, break blocks, use a portal, or become a permanent controller of other roles.

## Required runtime scenarios

The future Forge campaign must run these deterministic cases and record pass/fail evidence:

1. Player at critical health, four nearby hostiles, known safe waypoint: Guardian chooses Retreat or Guard; no loot/ambient priority.
2. Full health, one distant zombie, daytime: Guardian stays Follow/Idle; no Retreat message.
3. Unknown visible structure at sunset with no known exit and low supplies: Guardian offers mark/return or perimeter caution; no forced entry.
4. Creeper near a protected base boundary: Guardian avoids reckless close melee near the protected area.
5. Low-confidence Seer warning only: Guardian adopts caution/regroup posture, not boss panic.
6. Hold request on unsafe lava/fall ground: safe hold fallback or readable rejection; no unsafe fixed position.
7. Twenty-second blocked path: bounded recovery, then safe recall/hold; no repeated unlimited path churn.
8. Gifted Push near villager, pet, or companion: server rejects before Energy/cooldown loss.
9. Gifted Shield with protected entities nearby: shield remains owner-only.
10. Gifted `ASK_FIRST`, `EMERGENCY_ONLY`, and `RESCUE_ONLY`: suggestions follow policy, but no power is automatically cast.
11. Plan success, cancel, timeout, and load-invalid target: one accurate Guardian review card for each result.

## Non-claims

This contract does not claim an actor voice, copied show dialogue, full emotional simulation, a permanent fear/trauma meter, balance proof, or real Forge playtest success. The dialogue lines added for readiness are original English project text and remain subject to future runtime pacing review.
