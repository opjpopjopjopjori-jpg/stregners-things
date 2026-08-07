# Will Control and Boss Interaction Contract

> The internal `Hive*` class names preserve prior resource and save-safe terminology. Player-facing behavior is now **Will Control**: visible eligible hostile mobs use a deterministic control-heart budget. See `docs/WILL_GIFTED_PRECISION_POWER_CONTRACT.md` for the full model and precision-animation rules.

## Real target state

Every fully controlled target receives persistent server state with a mode, expiry, original AI/gravity flags, and redirect source. Only flags changed by the control mode are restored, preventing a reload or boss resistance path from leaving an entity in a fake frozen state.

A valid target is a visible, loaded hostile `Mob` implementing Minecraft's `Enemy` interface and actively threatening the owner or an owned companion; a nearby recognized boss pressure also qualifies for its limited resistance path. Players, companions, villagers, animals, tameable entities, and types in `riftcompanions:protected_from_companions` remain ineligible. This active-threat condition prevents passive mob-farm control.

| Mode | Actual gameplay behavior | Safety limit |
|---|---|---|
| Suspend | No AI, zero velocity, no gravity, bounded collision-safe lift, safe release with zero fall distance | No teleport, block pass, or chunk load |
| Redirect | No AI and bounded collision-checked movement away from the source | Never targets protected entities or enters unloaded/fluid/collision space |
| Shatter | Short stunned/weak state with non-graphic VFX | Does not kill, modify loot, or duplicate loot |
| Swarm Freeze | Independent Suspend state per selected target | Heart budget and hard target cap; no global dimension freeze |
| Stagger | Short resistance/overload debuff state | Does not change boss AI/gravity, lift, redirect, or kill |

## Control-heart model

The server evaluates target maximum health as control hearts, then adds bounded attack and armor pressure. It selects nearest valid targets first, applies the configured heart budget, and sends display-only target/load information to the owner HUD.

- One target too powerful for the remaining budget receives a short `Stagger` opening rather than full control.
- A crowd is bounded by both available control hearts and the configured Swarm target cap.
- Higher combined load raises Energy/Strain cost and shortens the release window.
- No client packet can nominate target UUIDs, set heart values, or alter the selection.

## Channel contract

Will enters focus first. If a selected target is lost, invalid, out of sight, out of range, protected, immune, or if Will is interrupted before release, the channel cancels without the full energy/strain cost. Release validates targets again before applying any effect.

## Boss resistance

- `IMMUNE`: no release; the HUD reports that the target resists the link.
- `PARTIAL`: a short `Stagger` only; no AI suppression, lift, redirect, kill, or boss-phase bypass.
- `VULNERABLE_WINDOW`: normal mode can apply if the target fits the control-heart budget.

The built-in resistance tag includes the vanilla Wither and Ender Dragon. Datapacks/adapters can add a safe partial or immune compatibility rule for another boss. No boss becomes a pet, kills itself, flies without physics, or bypasses a quest/phase requirement.

## VFX and audio

- Target state uses bounded particles, Will Link HUD status, timer text, and companion presentation poses; the mod adds no custom hostile model or enemy animation family.
- Low Effects reduces particles but leaves HUD/state information intact.
- Original synthesized OGG cues are used for notice, focus, release, resist, and recovery.
- No camera movement, input lock, time stop, terrain destruction, show audio, actor voice, or copied music is used.

## Required runtime tests

- Standard vanilla hostile types can focus/release only when eligible, visible, loaded, and in range.
- Players, pets, villagers, animals, companions, and protected tags reject safely.
- A crowded group obeys nearest-first heart selection and the hard cap.
- High-health and boss targets produce short Stagger behavior instead of unsafe full control.
- Target dies/lost before release: no full cost and no success VFX.
- Boss partial resistance: no lift/redirect/full AI suppression.
- Swarm cap in Low Effects mode.
- Player moves behind a wall during focus.
- Save/load while a target is controlled.
- Will damage interruption.
- All accessibility reductions still communicate mode and duration.
