# Behavior Acceptance Catalog

## Purpose

This catalog turns subjective companion claims into named, repeatable runtime scenarios. It is source implemented as `BehaviorTestCatalog` and can be read through:

```mcfunction
/riftcompanions_dev behavior <case_id>
```

The command prints a setup, expected behavior, and failure boundary. It does not spawn a fake scenario, modify the world, or mark a test as passed.

## Guardian controlled acceptance rule

For each written Guardian scenario, run ten controlled repetitions after the Forge runtime workspace exists.

```text
Required safe and explainable outcomes: 9 out of 10
```

This is not a claim that Guardian solves ninety percent of all random Minecraft situations. It is a repeatable acceptance target for fixed documented setups.

## Catalog IDs

| ID | Focus | Expected boundary |
|---|---|---|
| `guardian_low_health_known_route` | Survival priority | Retreat or Guard Player beats loot, scout, base, and ambient work. |
| `guardian_calm_distant_zombie` | Restraint | No panic plan for one distant ordinary zombie. |
| `guardian_unknown_structure_sunset` | Perimeter caution | Mark/return or caution; never forced entry or loot access. |
| `guardian_creeper_protected_area` | Protected-area safety | No reckless explosive close melee beside protected space. |
| `guardian_low_confidence_seer` | Evidence discipline | Caution/regroup allowed; boss certainty/panic forbidden. |
| `guardian_unsafe_hold` | Safe command fallback | Unsafe Hold rejects or uses safe behavior. |
| `guardian_blocked_path` | Navigation recovery | Bounded retry -> recovery -> Safe Recall/Hold; no block edit/loop. |
| `seer_dark_cave_no_tag` | Non-radar boundary | Darkness alone never becomes anomaly certainty. |
| `will_vanilla_hostile_control` | Broad hostile eligibility | Eligible vanilla hostile types do not need any custom entity tag; protected living types remain excluded. |
| `will_swarm_heart_budget` | Deterministic crowd cap | Nearest-first Swarm selection stays within server heart budget/hard cap and matches HUD load/count. |
| `will_boss_partial_stagger` | Boss/overload resistance | Bosses and over-budget threats get only short Stagger, never full control or phase/loot bypass. |
| `will_protected_target_rejected` | Protected-target boundary | Villagers, pets, animals, companions, and protected entity types never receive a control state. |
| `guardian_brace_bounded` | Guardian mitigation | Explicit, short, energy/cooldown-bounded player protection is not invulnerability or a farm tool. |
| `scout_signal_visible_only` | Scout tactical mark | Only visible active hostile targets get temporary non-damaging marks. |
| `social_visible_world_pair` | Social world interaction | Safe nearby pair exchange uses only visible local cues, authored dialogue, and interruptible social clips. |
| `social_reply_safety_cancel` | Social interruption safety | Pending reply cancels on danger, Safe Mode, plan, low health, state/distance break, logout, or unload. |
| `conversation_context_topics` | Contextual conversation | New world/team/encounter/check-in replies use bounded visible/team/memory facts only. |
| `scout_precheck_critical` | Scout refusal | Critical/downed/boss/high-danger context blocks scout. |
| `gifted_protected_bystander` | Power safety | Push rejects before cost near protected bystander. |
| `story_journal_privacy` | Narrative privacy | Bounded Journal summary never exposes coordinates/raw clue IDs/future content. |
| `team_supply_privacy` | Resource privacy | UI never exposes container coordinate, slot, stack, count, NBT, or contents. |

## Regression discipline

Every runtime bug must become one of the following before it is considered fixed:

1. A new behavior catalog case.
2. A new chaos scenario.
3. A new source validation invariant.
4. A documented intentional scope boundary.

A vague manual claim such as "the companion looked smarter" is not sufficient acceptance evidence.

## Required evidence per run

```text
Case ID:
World seed:
Minecraft / Forge / GeckoLib / mod version:
Active duo:
Feature flags and policies:
Setup steps:
Observed reason codes:
Expected result:
Actual result:
Pass / fail:
Developer overlay or diagnostic report:
latest.log / crash report if applicable:
```

## Non-claims

The catalog is not an automated Minecraft test runner. It becomes runtime evidence only after the owner allows a real Forge test world.
