# 512×512 Faceted Character UV Layout

The eight companion atlases are technical **512×512 RGBA** sheets for four personal and four public faceted-character models. A PNG remains rectangular by definition, but the visible character is not a flat square skin: face planes, eyes, lids, brows, mouth, hair locks, body segments, and clothing layers are independent Geo cuboids.

## Material islands

| Group | Purpose |
|---|---|
| `head_*` | Abstract non-portrait skin planes for tapered faceted heads |
| `skin_tile`, `hair_tile` | Face, ears, nose, hair locks, brows, and lids |
| `eye_tile`, `accent_tile`, `thread_tile` | Separate whites, iris, pupil, catchlight, and mouth components |
| `jacket`, `shirt`, `field`, `cloth`, `weather` | Ordinary late-1980s clothing layers and role palettes |
| `pants`, `leather`, `boot`, `metal` | Articulated lower body, shoes, watch, belt, and hardware |

## Editing boundary

- Preserve `512×512`, RGBA, lossless PNG output.
- Keep all UV regions inside `tools/generate_gecko_assets.py` bounds.
- Do not paint a copied face or exact costume onto `head_*` panels.
- Keep the eye and hair material islands distinct because their Geo bones animate independently.
