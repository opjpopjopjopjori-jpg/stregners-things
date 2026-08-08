# Texture-Compatible Animation Mapping

Every animation uses specific 512x512 RGBA islands:

- `eye_tile` + `accent_tile` + `thread_tile` → face expressions (neutral, combat, recovery, alert, talk)
- `hair_tile` → secondary hair motion (gentle, intense, combat, recovery)
- `jacket` / `shirt` / `field` / `cloth` / `weather` → clothing dynamics (defensive, retreating, active)
- `pants` / `leather` / `boot` / `metal` → lower body articulation (walk, run, combat stance)

Every enhanced animation controller references these islands explicitly.
