# Natural Gait and Head-Look Contract

## Goal

A companion following a valid path should travel with its body facing its real travel vector. It must not repeatedly spin its entire body because of ambient look goals. Attention to a lateral or rear target is presentation-only: the faceted head/face layer may glance while the body continues along its path.

## Server gait director

`CompanionGaitPresentationService` runs only after normal Minecraft navigation movement has updated for the tick.

1. When horizontal velocity is meaningful, it stabilizes body yaw toward the current travel vector with a capped turn rate.
2. It removes the old `LookAtPlayerGoal` and `RandomLookAroundGoal` full-body jitter path.
3. It reads only an already-selected current combat target; it does not scan entities, acquire targets, read hidden terrain, alter navigation, or create player control.
4. It emits a look gesture only when that target is new or moves into a new lateral/rear sector. A target remaining in the same sector creates no repeated gesture.
5. It synchronizes one visual `CompanionLookIntent` to the client.

## Look intents

| Intent | Body behavior | Face-layer behavior |
|---|---|---|
| `FORWARD` | Continue travel heading | Neutral forward face loop |
| `GLANCE_LEFT` | Continue travel heading | Head and eyes rotate left only |
| `GLANCE_RIGHT` | Continue travel heading | Head and eyes rotate right only |
| `CHECK_BACK_LEFT` | Continue travel heading | One bounded rear-left check, then forward return |
| `CHECK_BACK_RIGHT` | Continue travel heading | One bounded rear-right check, then forward return |

A lateral glance lasts 20 ticks and then enters a 70-tick cooldown. A rear check lasts 30 ticks and then enters a 120-tick cooldown. Both gestures return the head and eyes to forward, so a target cannot pin the head sideways or cause repeated body spin. Normal idle and alert loops keep head yaw neutral; they never create a periodic left/right head scan.

## Three controller split

```text
companion_body       -> body locomotion/action bones only
companion_face       -> head, eyes, lids, brows, jaw, mouth
companion_secondary  -> role hair locks and clothing tails/collars
```

The body walk/run clips intentionally do not animate the `head` bone. Head orientation belongs only to the face layer, so a right glance or rear check does not fight the walking animation or rotate the full body.

## Safety and scope

- No camera/input lock.
- No target acquisition, x-ray, player tracking, chunk loading, or terrain scan.
- No AI/path/target mutation from an animation clip.
- No change to hitbox, collision, damage, power, inventory, or player movement.
- Gait visual state is ephemeral and is not persisted as player/world data.

## Runtime matrix

| Case | Expected result |
|---|---|
| Long straight Follow path | Body remains aligned to velocity; no repeated full-body spin. |
| Visible target on right/left while path continues | Body remains on path; head/eyes use only the matching glance loop. |
| Visible target behind while path continues | One head-only rear check plays, returns forward, and respects cooldown. |
| Target disappears | Look intent returns to forward without a stuck head rotation. |
| Combat / power / talk | Body action continues while face and secondary controllers keep bounded expression/hair motion. |
| Downed / logout / dismissal | Intent/session state clears safely. |

The matrix is a test requirement, not an unverified runtime result.
