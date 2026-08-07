# Feature Flags and Safe Data-Pack Extension

## Ownership boundary

Java owns the safety gate, state machines, transactional side effects, save/recovery, navigation, and Team Director. Data packs own constrained content such as dialogue, optional intention definitions, item labels, tags, and support-profile data.

No JSON file can grant a companion permission to break blocks, open private storage, harvest farms, use redstone, activate a portal, cross a dimension, or bypass a server safety check.

## Feature flags

The common config exposes development isolation flags:

```text
enable_hopper_core
enable_memory_lite
enable_will_hive_link
enable_will_hive_surge
enable_max_scout
enable_eleven_powers
enable_team_plans
enable_inventory_assist
enable_story_graph
enable_duo_dynamics
enable_companion_intentions
enable_daily_ambient
enable_companion_social
```

Flags exist to isolate a regression and support controlled A/B playtests. They are not a reason to ship an unsafe feature. Turning a flag off never removes Follow, Safe Recall, or access to the Team Journal.

`enable_inventory_assist` defaults to `false`. Optional WORLD_FREE ground pickup remains disabled until both this feature flag and a non-OFF pickup policy are explicitly enabled. `team_supply_enabled` also defaults to `false`; it can access only a player-bound safe-base container, never arbitrary chests. There is no farm action or general container automation.

`enable_companion_social` isolates authored two-companion social lead/reply exchanges. It never grants a power, reads a container, inspects hidden terrain, changes a plan, takes player input, locks a camera, or creates runtime text. Disabling it leaves all companion gameplay, conversation, Follow, Recall, Journal, and safety systems available.

## Reloaded content

| Folder | Loader | Purpose | Failure behavior |
|---|---|---|---|
| `companions_dialogue/` | `DialogueReloadListener` | English dialogue by stable ID/role/trigger | Invalid line is skipped with a log warning |
| `compatibility_packs/` | `CompatibilityPackReloadListener` | Optional support-pack metadata | Invalid pack remains Unknown/Caution |
| `companion_intentions/` | `IntentionReloadListener` | Optional intention context, completion key, text references | Invalid file is skipped; no quest is invented |
| `item_classifications/` | `ItemClassificationReloadListener` | Transparent manual-inventory item labels | Invalid file is skipped; no automation permission changes |
| `threat_profiles/` | `ThreatProfileReloadListener` | Advisory archetype vocabulary and positive caution bias | Invalid file is skipped; profiles cannot grant power eligibility |
| `structure_overrides/` | `StructureProfileOverrideReloadListener` | Observed-block profile vocabulary and positive caution bias | Invalid file is skipped; scanner still inspects only loaded player-visible blocks |
| entity type tags | Vanilla tag loader | Hive story sensing, boss resistance, and protected-target exclusions | Missing tags never create a fictional ability target or bypass hostile eligibility |

All current data-pack JSON is English-only. Runtime content still uses stable IDs and never depends on a translated literal string for logic.

## Optional intention schema

`data/riftcompanions/companion_intentions/core_intentions.json` uses a bounded schema:

```json
{
  "id": "guardian_guard_post",
  "role": "guardian",
  "context": "...",
  "optional_action": "...",
  "completion_key": "ANCHOR_GUARD_POST",
  "reward_summary": "...",
  "dialogue_trigger": "intention_offer"
}
```

The data file may describe an intention, but Java validates the event that completes it. A content pack cannot complete an intention by chat text, grant a damage upgrade, or create an item reward.

## Item classification schema

`data/riftcompanions/item_classifications/*.json` entries contain only an item ID and a conservative `ItemCategory`. The result is used for transparent manual transfer feedback. Unknown modded items remain `UNKNOWN` unless a safe data override is supplied.

## Advisory threat and structure schemas

Threat profiles declare an `entity_type`, one bounded `ThreatArchetype`, and a non-negative `caution_bias` from 0 through 30. They can make a warning more specific or more cautious; they cannot reduce the conservative Unknown/Caution fallback for unclassified modded threats.

Structure overrides declare only an observed `block`, one allow-listed profile ID, and a non-negative `caution_bias`. The bounded perimeter scanner never searches chunks, discovers hidden structures, or treats a profile as a permission to enter, loot, trade, or activate a portal.

## Personal policy state

`PlayerPolicyState` is persisted per owner in the team blackboard. It may override the effective Seer, Gifted, or Scout power policy, while the world config remains the fallback. The Team Journal **Policies** tab and fixed role-only packet can cycle or reset a policy. A personal policy cannot bypass Safe Mode, power feature flags, cooldown, energy, strain, protected/boss tag rules, active-threat/range/line checks, control-heart limits, or any Safety Gate.

## Reload discipline

A runtime test must verify the following after the Forge workspace is available:

- invalid JSON logs a readable warning and leaves the save playable;
- removing a Hive tag during a channel causes release validation to fail safely;
- removing an intention definition leaves the stored intention readable rather than crashing;
- changing a classification never changes item count, ownership, or container access;
- a threat or structure advisory can only add caution and vocabulary, never discover hidden world data or grant an ability;
- a personal policy update is owner-only and still rejects forbidden role/policy combinations.
