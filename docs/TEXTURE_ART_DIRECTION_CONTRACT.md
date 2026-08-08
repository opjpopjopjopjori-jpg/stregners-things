# Faceted Character Visual Direction Contract

## Goal

Rift Companions uses original faceted characters with a tapered head silhouette, separate face components, layered hair, articulated body segments, and ordinary late-1980s clothing. The direction aims for a rich custom-model feeling inside the Forge/GeckoLib renderer without a third-party model dependency.

## Originality boundary

Reference research informs only broad hair mass, age band, grounded clothing layers, and muted late-1980s color direction. This project does not reproduce actor likeness, a screen costume, source pixel data, show logo, soundtrack, dialogue, or a Yes Steve Model asset.

## Technical contract

- Each personal/public texture atlas remains 512×512 RGBA.
- Every role Geo model declares `faceted_character_512_v2` and retains the animation-safe `shared_humanoid_v2` backbone.
- `eye_*`, `brow_*`, `mouth_*`, and role hair bones are visual-only and may be driven only by authored presentation clips.
- The renderer must not alter hitboxes, collision, player input, camera, power authority, or gameplay state.
- External downloads, online AI, asset APIs, and mandatory YSM installation are prohibited.

## Runtime review gates

Check the final models in Minecraft for frustum culling, close-range face readability, blink/gaze alignment, hair clipping, locomotion clipping, facial expression timing, and GPU performance before making any runtime claim.
