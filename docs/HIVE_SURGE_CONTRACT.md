# Will Surge Contract

> See `docs/WILL_GIFTED_PRECISION_POWER_CONTRACT.md` for the shared control-heart model, mode-specific lift/shatter precision, and Gifted presentation boundaries.

## Scope

Will Surge is a rare explicit Seer action against visible **eligible hostile threats**. It expands temporary control-heart capacity during a genuine emergency; it does not bypass active-threat eligibility, protected areas, boss resistance, or safety policy.

Available explicit ability IDs:

```text
surge_suspend
surge_redirect
surge_shatter
surge_swarm_freeze
```

The player chooses one mode. The logical server validates hostile eligibility, line-of-sight, loaded range, control-heart load, policy, protected-area boundary, villagers, animals, energy, strain, cooldown, and boss resistance before focus begins.

## Surge gate

A Surge must satisfy at least one genuine emergency signal:

- player lava/fire/fall/critical-health risk;
- configured number of visible eligible hostile targets;
- a visible boss resistance signal.

It is rejected near protected or machine annotations, villagers, or animal boundaries. This remains conservative: Surge has no loot, block, transport, pet, or farm-automation path.

## Channel and transaction safety

```text
NOTICE -> FOCUS -> validated RELEASE -> RECOVERY
```

- Target loss, policy disable, Safe Mode, damage interruption, protection change, or immunity cancels before full cost.
- Energy and strain apply only on a valid release; both scale from the selected control-heart load.
- Surge receives only the configured temporary heart-capacity bonus and never ignores the hard target cap.
- A Surge has a long shared cooldown and forced Seer recovery.
- Save/load recovery never guesses a successful release.
- Player input/camera remains unrestricted throughout.

## Modes

| Mode | Real target state | Purpose | Limits |
|---|---|---|---|
| Suspend | Independent no-AI/no-gravity control state with bounded collision-safe lift | Open a short exit | No block pass, teleport, chunk load, or full boss control |
| Redirect | Independent safe move-away state | Break pressure toward team | Never players, pets, villagers, animals, protected types, fluids, or unloaded space |
| Shatter | Independent non-graphic weakness/slow state | Short high-cost opening | No gore, kill, loot, or farm effect |
| Swarm Freeze | Per-target independent Suspend state | Escape a small visible crowd | Nearest-first selection, heart budget, hard cap, no global freeze |
| Stagger | Short resistant target debuff | Honest limited response to overload/boss pressure | No AI suppression, lift, redirect, or kill |

The existing `HiveControlManager` persists original target AI/gravity state and restores it at expiry. A full-control release clears fall distance and adds only a short confusion/stagger effect; resisted `Stagger` never alters boss AI/gravity.

## Team and presentation layer

The source reuses server-owned Seer focus/release visual actions, owner HUD Will Link status with target/load capacity, original Hive cues, Low Effects particle reduction, Guardian guard response, and Scout route response. A full distinct Surge animation asset family and advanced audio mix remain runtime/content work, not a false completion claim.

## Required runtime tests

- Eligible vanilla zombie group in critical context: Surge channels, releases once, and obeys its heart budget/cap.
- Protected entity, villager, animal, or sensitive-area boundary: Surge rejects safely.
- Wither/Ender Dragon: short Stagger only; no lift, redirect, kill, or boss-phase bypass.
- High-health non-boss: control-heart overflow uses the same short Stagger path.
- Target death/protection change during focus: no cost duplication or stale target state.
- Player movement during focus: no forced camera/input behavior.
- Save/load during focus: recovery cancels or restores only valid state; no duplicate release.
