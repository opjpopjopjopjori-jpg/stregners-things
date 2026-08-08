# Dialogue JSON Schema

Default file: `data/riftcompanions/companions_dialogue/core_en_us.json`

```json
{
  "schema": 1,
  "locale": "en_us",
  "entries": [
    {
      "id": "guardian.retreat_start.001",
      "role": "guardian",
      "trigger": "retreat_start",
      "priority": 1,
      "cooldown_ticks": 900,
      "text": "We have a reason to leave. Regroup and use the route back."
    }
  ]
}
```

Validation rules:

- `role` must be `seer`, `guardian`, `gifted`, or `scout`.
- `priority` must be 0–4, where 0 is critical.
- `cooldown_ticks` must be non-negative.
- `text` is limited to 240 characters.
- The packaged default locale is English-only.
- Invalid entries are skipped with a log warning rather than corrupting a world.

Gameplay code uses trigger IDs, not literal dialogue text. External content packs may add their own data, but the shipped mod pack contains English-only content.

The Social Director uses paired authored triggers such as `social_campfire_lead` and `social_campfire_reply`. A reply is scheduled only after its lead line was delivered, shares the existing normal chat budget, and cancels on danger, Safe Mode, plan, distance, state, unload, or logout failure. The JSON still grants no gameplay authority.
