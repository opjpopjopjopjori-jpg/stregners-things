# Compatibility and Support Pack Guide

Compatibility packs are optional data-driven declarations loaded from:

```text
data/riftcompanions/compatibility_packs/*.json
```

Minimal definition:

```json
{
  "id": "examplemod_safe_pack",
  "target_mod": "examplemod",
  "level": "SUPPORTED_PACK"
}
```

Allowed levels:

```text
VERIFIED_VANILLA
FRIENDLY
SUPPORTED_PACK
EXPERIMENTAL
INCOMPATIBLE
```

Rules:

- A malformed pack is ignored and logged.
- A missing target mod leaves the pack inactive.
- A failed pack must never crash the game or grant unsupported knowledge.
- Unknown entities always fall back to `EncounterIdentity.UNKNOWN` and Caution behavior.
- A compatibility pack may not bypass Safety Gates, access private inventory, force chunk loading, or alter player data.
- Boss behavior requires a separate boss adapter and runtime tests.

## Other safe content folders

These folders are independently validated and do not grant gameplay permissions:

```text
data/riftcompanions/companions_dialogue/*.json
data/riftcompanions/companion_intentions/*.json
data/riftcompanions/item_classifications/*.json
data/riftcompanions/threat_profiles/*.json
data/riftcompanions/structure_overrides/*.json
```

An intention pack can describe optional context and a completion key, but Java validates completion. An item classification pack only changes transparent manual-transfer labels. Threat and structure packs can only add cautious advisory vocabulary from already observed entities or blocks. None can enable automatic pickup, chest access, block interaction, power bypass, hidden structure discovery, or cross-dimension behavior.
