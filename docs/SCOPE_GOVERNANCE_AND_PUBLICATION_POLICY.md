# Scope Governance, Feature Kill Criteria, and Publication Policy

## Purpose

This policy prevents source volume from being mistaken for a stable release and prevents future work from quietly expanding companion permissions. It applies to the offline/single-player source project only.

## Three different completion levels

| Level | Meaning | Current truth |
|---|---|---|
| Source implemented | Java/resources/docs/static validators exist | Many systems meet this level. |
| Runtime verified | Java 17 + Forge + GeckoLib test world proves behavior | Build now succeeds; real Minecraft runtime evidence is still pending. |
| Release ready | Runtime evidence, performance profile, asset/licensing review, version migration evidence, and acceptance results exist | Not claimed. |

No source feature may be advertised as runtime stable before the second level exists.

## Feature kill criteria

| Feature area | Keep only if | Reduce or disable if |
|---|---|---|
| Navigation / formations | Bounded recovery reaches Follow, Safe Recall, or Hold without repeating path churn | Repeated path failures persist after the written repair budget; reduce formation complexity or Scout scope rather than adding unbounded searches. |
| Gifted powers | Useful opening without removing player challenge | Repeated balance failures occur; retain Shield/Rescue and remove or narrow higher-impact behavior. |
| Story / dialogue | Adds clarity, personality, or optional evidence without chat pressure | Playtesters cannot identify a useful function; reduce frequency or remove lines rather than adding more volume. |
| Base life | Non-destructive atmosphere improves readability | It conflicts with navigation, player space, or block safety; keep anchors only. |
| Team Supply | Explicit transaction remains private, bounded, and non-exploitative | Any duplicate, coordinate leak, arbitrary chest access, or cross-dimension behavior appears; disable it. |
| Compatibility pack | Real adapter adds cautious vocabulary without permission expansion | It leaks hidden data, assumes every item/mob/structure behaves like Vanilla, or lacks a complete test matrix. |
| Natural-language parser | Reliable fixed command intent and cancellation behavior | Ambiguous inputs bypass safety or create surprise action; retain Command Wheel/explicit commands only. |
| Animation/VFX | Improves state readability without camera/FPS/crosshair harm | Visual clipping, sustained FPS regression, cue spam, or unclear cancellation occurs; reduce clip/VFX complexity. |

## Non-negotiable scope boundaries

The following remain outside the core source scope unless a future owner explicitly changes the design and provides runtime acceptance criteria:

```text
Multiplayer and multiple owners
External AI / internet / API keys
Free-form ChatGPT-like conversation
Automatic block breaking/building
TNT, lava, fire, Redstone, farm automation
Private chest access or remote storage
Automatic transport / portal / dimension transfer
Unbounded chunk scouting
Camera lock / input lock / forced cutscene
Actor voice imitation, show music, copied scenes, or extracted show assets
```

## Presentation and publication policy

The project has two content modes:

```text
PERSONAL
PUBLIC
```

Logic identifiers remain generic:

```text
seer
guardian
gifted
scout
```

Visible personal presentation is isolated through `ContentProfileRegistry`, texture folders, and dialogue/content profiles. Public presentation uses original generic role names and original public textures.

Before any public distribution:

1. Replace or review every personal presentation asset.
2. Confirm no actor likeness, voice imitation, soundtrack, quoted dialogue, show screenshot, extracted model, or copied scene remains.
3. Record ownership/license source for every model, texture, sound, font, and library.
4. Select a real owner-controlled project license; this repository does not invent legal ownership on behalf of the owner.
5. Verify the public content mode in an actual runtime world.
6. Publish only features that have passed the relevant runtime gate.

## Version discipline

- Every persisted data addition requires a `SaveVersions` review and safe migration default.
- Every custom packet layout or synchronized enum semantic change requires a protocol revision.
- Every regression found in runtime testing becomes a behavior catalog case, chaos scenario, static invariant, or explicit scope reduction.
- Every stable milestone should be tagged in the owner’s future Git repository only after the documented runtime gate for that milestone passes.

## Non-claims

This is governance and publication preparation, not a license grant, legal advice, runtime compatibility guarantee, or public-release approval.
