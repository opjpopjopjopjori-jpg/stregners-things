# Vanilla Rule Policy, Onboarding, and Accessibility Contract

## Status

This document describes source implemented behavior. The Forge workspace now compiles successfully, but this document does not claim a Minecraft runtime test has been completed. The project remains offline/single-player only and English-only.

## Vanilla rule policy

`VanillaRuleSnapshot` reads only actual Vanilla facts that are relevant to companion safety:

```text
Difficulty
Mob Griefing
Keep Inventory
Daylight Cycle
Hardcore
Creative state
Spectator state
```

It does not invent heat, thirst, mud, ropes, or hidden world information.

| Vanilla condition | Source policy |
|---|---|
| Survival | Bounded survival initiative may run under existing Safety Gates. |
| Peaceful | Automatic survival pressure and Defend planning are paused; Follow, Recall, Journal, base, and exploration guidance remain available. |
| Creative | Survival pressure and automatic combat planning are paused; companions remain available for follow/chat/base behavior. |
| Spectator | Loaded companions hold safely; an open plan is cancelled before any background execution can continue. |
| `mobGriefing = false` | No special block workaround occurs. Companion block edits are already disabled by design. |
| `keepInventory = true` | No player item handling is added. Story Downed behavior remains separate from player death inventory rules. |
| `doDaylightCycle = false` | Day-based ambient opportunities stay conservative; no forced time behavior is created. |
| Hardcore | Story Downed remains the default source behavior. No companion permadeath is silently enabled. |

The Team Journal Guide tab projects the current policy. It is a display-only server snapshot; it never changes a world rule.

## World edit invariant

`VanillaRulePolicyService.allowsCompanionWorldEdit` always returns `false` in this source milestone. This is intentional, not a missing permission check.

Companions do not:

- place, break, harvest, ignite, flood, or explode blocks;
- use TNT, lava, fire, Redstone, doors, or machines;
- create an emergency barrier;
- open private containers or inspect chest contents.

A future feature cannot bypass this invariant by observing `mobGriefing = true`.

## Optional onboarding

`OnboardingState` stores only a bounded delivered-hint set, a dismissal flag, and the most recent hint. It contains no chat transcript, personal data, task queue, reward, or progress gate.

Available one-time hints are:

```text
FIRST_COMPANION
FIRST_PLAN_PROPOSAL
FIRST_STUCK_RECOVERY
FIRST_SAFE_MODE
WORLD_RULE_POLICY
```

Hints are delivered only after real local events. They are concise English system messages and do not pause the world, lock input, open a forced screen, change a plan, spend a resource, or move an entity.

Configuration:

```text
common.gameplay_policy.tutorial_enabled = true
```

Player commands:

```mcfunction
/companions tutorial status
/companions tutorial dismiss
/companions tutorial resume
```

Dismissing guidance is always safe. It does not block Follow, Recall, Journal access, plans, powers, or Vanilla progression. Resuming guidance does not replay every past message; it shows only the next recommended reminder.

## Team Journal Guide tab

The English-only `Guide` tab provides a compact in-game explanation of:

- first companion controls and player agency;
- the next optional guidance reminder;
- current game mode, difficulty, and selected Vanilla rules;
- the permanent no-world-edit boundary;
- Stop All, Clear Plan, Reset Task, and Recall behavior;
- `CRITICAL_ONLY` dialogue accessibility mode.

It is deliberately text-first and does not require particles, audio, external fonts, camera movement, or a controller-specific UI.

## Accessibility

Existing and expanded accessibility behavior:

| Setting or control | Effect |
|---|---|
| `ChatProfile.CRITICAL_ONLY` | Shows P0 critical alerts only. HUD, Journal, commands, and Safe Mode feedback remain visible. |
| `ChatProfile.MINIMAL` | Shows P0/P1 urgent calls only. |
| `ChatProfile.STANDARD` | Default bounded dialogue policy. |
| `ChatProfile.CINEMATIC` | Allows rare ambient lines under normal cooldown/budget rules. |
| Low Effects | Reduces optional particles while preserving state/HUD information. |
| High Contrast Markers | Uses stronger HUD state colors and labels. |
| F8 Developer Overlay | Optional developer-only diagnostics; it does not affect gameplay. |
| Guide tab | Text explanation of current policy and safe controls. |

No accessibility profile changes AI authority, damage, inventory, plan validity, or Safety Gates.

## Runtime acceptance matrix

The future Forge runtime campaign must verify:

1. Peaceful rejects Defend planning without losing Follow, Recall, Journal, or base behavior.
2. Creative suppresses survival-pressure chatter and automatic combat planning.
3. Spectator safely holds loaded companions and resumes normal follow after exit.
4. `mobGriefing = false` never causes a companion world edit or a false emergency-building promise.
5. Hardcore does not silently turn Story Downed into companion permadeath.
6. Tutorial first-companion, plan, stuck, and Safe Mode hints appear once only.
7. Tutorial dismissal and resume persist across Save/Load without forcing messages or blocking gameplay.
8. `CRITICAL_ONLY` suppresses non-critical dialogue but leaves P0 alerts, HUD, Journal, and command feedback clear.
9. Guide tab fits at common GUI scales and does not cover command controls or create a pause/input lock.

## Feature kill criteria

- If a tutorial message becomes distracting in playtests, reduce delivery conditions or keep it Guide-tab-only; never add more forced screens.
- If a Vanilla rule interaction cannot be verified, show a conservative informational state and avoid claiming support.
- If a future world-edit feature conflicts with the no-edit invariant, it remains disabled rather than gaining a hidden exception.
- If a controller-specific guide cannot be tested, do not advertise it as supported.

## Non-claims

This source does not claim tested gamerule compatibility, full controller UI, a full quest tutorial, natural-language parser, automatic building, multiplayer behavior, or actual in-game accessibility ratings. Runtime validation remains required after the owner permits the Java 17 + Forge 47.x workspace.
