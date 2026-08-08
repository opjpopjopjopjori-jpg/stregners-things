# Character Visual Direction Contract

## Goal

Rift Companions uses authentic 64×64 Minecraft player skins from the characters' real skins, enhanced with realistic 3D modeled hair volume and realistic 3D eyes and eyebrows on the face.

## Visual & Rig Contract

- Each personal/public texture atlas remains 64×64 RGBA.
- Every role Geo model declares `faceted_character_64_v2` and retains the animation-safe `shared_humanoid_v2` backbone.
- Realistic 3D hair volume (`seer_hair_crown`, `scout_hair_back`, etc.) is modeled in 3D around the head and textured from the dedicated 3D hair extension sheet.
- Realistic 3D eyes and eyebrows (`eye_left_white`, `eye_left_pupil`, `brow_left`, etc.) are modeled on the face and textured from the dedicated 3D eye extension sheet.
- The renderer must not alter hitboxes, collision, player input, camera, power authority, or gameplay state.

## Runtime review gates

Check the final models in Minecraft for close-range face readability, blink/gaze alignment, realistic 3D hair appearance, and GPU performance.
