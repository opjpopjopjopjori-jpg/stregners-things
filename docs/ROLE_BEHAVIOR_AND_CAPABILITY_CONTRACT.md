# Role Behavior, Personality, and Capability Contract

## Separation of concerns

The source separates four layers:

```text
Personality profile
Tactical role
Capability loadout
Relationship / memory state
```

A personality profile controls priorities, forbidden actions, formation bias, and dialogue style. It does not directly grant a power, bypass a safety rule, or change damage. A capability action still passes the shared server Safety Gate.

Profiles load from:

```text
data/riftcompanions/behavior_profiles/*.json
```

An invalid profile is ignored safely and cannot turn a companion into a different permission class.

## Will behavior

`WillAwarenessState` stores a bounded evidence key, evidence count, confidence level, stress window, and dialogue cooldown.

```text
LOW: one weak/new observed signal
MEDIUM: two related observations
HIGH: visible confirmed Hive tag
```

Time alone never raises confidence. A stressed Will reduces optional signal dialogue instead of becoming a combat liability or making false alerts.

Will does not long-range scout, enter melee from a Focus command, chase loot, see through walls, or claim that ordinary cave darkness is supernatural evidence.

## Max behavior

Before a short scout begins, the server checks:

```text
Player health
Any DOWNED teammate
Boss encounter context
Current danger score
Loaded terrain-only route candidates
Configured default/hard scout radius
```

A rejected scout returns one concise reason. A successful scout records a bounded route observation and returns to formation. It does not load chunks, chase loot, cross deep water/lava, or outvote a critical retreat.

Scout Signal is a separate explicit tactical action: it marks up to three visible active hostile targets for a short duration after focus/cooldown/policy checks. It does not fire a projectile, reveal through walls, damage entities, affect protected targets, or become a loot/farm tool.

## Gifted behavior

Gifted emergency rescue/protection remains capability-driven:

```text
Target validity
Safe destination
Energy
Cooldown
Player policy
Protected-area rules
Boss resistance
```

`GiftedBehaviorState` additionally exposes a bounded READY/CAUTION/LIMITED/EXHAUSTED/RECOVERING/DOWNED readiness band. It can make a policy-aware, cooldown-bound suggestion but never auto-casts a power. Push now rejects local protected-bystander boundaries before spending Energy, and Shield remains owner-only. See `docs/ROLE_READINESS_AND_AFTER_ACTION_CONTRACT.md` for the full contract.

## Guardian behavior

Guardian remains the practical safety lead for visible survival pressure, retreat space, guard line, and team center. It can recommend safety but cannot take final non-emergency player decisions, open containers, force entry, or make a destructive plan.

Guardian Brace is a separate explicit 80-tick, close-range mitigation stance. It needs an active nearby threat, Guardian range, energy, cooldown, and normal Safety Gates; it reduces player damage modestly, never grants invulnerability, and ends on Safe Mode, logout/reload, distance loss, dismissal, rest, or expiry.

A compact `GuardianReviewState` records one factual after-action summary for terminal/cancelled/failed-safe plans. It does not reopen a plan, force chat, punish a choice, or create a second command channel.

## Social and speaking behavior

When exactly two active companions are nearby and the local Social Director safety gate passes, an authored lead/reply exchange may reflect visible campfire, weather, horizon, safe-base, player-selected work, cave, village, travel, or calm context. The roles retain distinct speaking priorities:

```text
Guardian: concrete safety and next step
Seer: evidence, uncertainty, and observation
Gifted: care, consent, and pacing
Scout: route, terrain, and practical preparation
```

The exchange is not an open-ended chatbot. It never reads containers, sees hidden terrain, modifies a plan, forces a player choice, uses external AI, or continues after danger, Safe Mode, active plan, distance/state failure, unload, or logout.

## Required runtime tests

- One ordinary dark cave/no tag: Will stays normal follow with no anomaly warning.
- Repeated investigation/Hive evidence: Will moves LOW -> MEDIUM -> HIGH only from new evidence.
- Low-confidence warning ignored: Will does not spam or block player movement.
- Scout while player critical/downed/boss context: Max rejects with one reason.
- Scout in loaded safe 20–24 block terrain: reports and returns within timeout.
- Gifted low energy/unsafe rescue destination: clear refusal, no fake effect.
- Guardian Brace with and without a real nearby threat: bounded mitigation only; no cost on a rejected request.
- Scout Signal on visible and unseen/protected targets: visible hostile marks only; no projectile/damage/wall vision.
- Focus target on Seer: no forced melee rush.
