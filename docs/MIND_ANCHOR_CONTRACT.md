# Mind Anchor Contract

## Scope

Mind Anchor is Max's grounded support system for explicit supported mental/disorientation effects. It is not telekinesis, monster control, physical healing, invulnerability, universal potion removal, or a boss skip.

Advanced actions are disabled by default:

```text
mind_anchor_enabled = false
enable_max_mind_anchor = true
```

Supported effects are data-driven under:

```text
data/riftcompanions/mental_effects/*.json
```

An absent effect profile means Max does not claim to remove it.

## Ability IDs

```text
ground
anchor_point
break_free
escape_window
```

| Action | Actual effect | Required limits |
|---|---|---|
| Grounding | Removes one highest-priority supported mental effect | Focus, Scout policy, cooldown |
| Anchor Point | Saves a temporary safe local anchor position | Safe spot, Focus, duration/cooldown, no block placement |
| Break Free | Removes one supported effect only while a valid anchor is active | Focus, anchor, cooldown |
| Escape Window | Uses/creates local anchor, clears one supported effect, drafts bounded retreat | Focus, safe anchor, no teleport or damage immunity |

## Anchor behavior

An active anchor does not heal health or prevent monster damage. While the player remains in its configured local radius, it shortens only supported effect durations once per second. It does not affect poison, wither, hunger, arbitrary mod effects, or physical threats unless a data profile explicitly supports a mental effect.

## Safety rules

- No anchor block is placed.
- No monster is moved or controlled.
- No player/companion teleport occurs.
- No unsupported effect is removed.
- No item fuel is used; Focus recovers through existing calm/rest behavior.
- Safe Mode, Scout policy, Focus, cooldown, and enabled feature/config checks remain final.

## Required runtime tests

- Vanilla combat with no supported effect: all advanced actions reject safely.
- Supported confusion/darkness effect + sufficient Focus: Grounding clears one effect.
- Anchor Point without safe local location: no artificial anchor is created.
- Active anchor radius: supported duration decreases; no physical damage resistance appears.
- Unsupported poison/wither: Max does not claim to remove it.
- Low Focus: action fails with no partial state.
- Escape Window: uses safe retreat plan only; no teleport or boss bypass.
- Save/load while anchor active: expired/dimension-invalid anchor cannot create a persistent exploit.
