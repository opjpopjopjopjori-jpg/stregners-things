# 64×64 Player Skin UV Layout

The companion atlases are **64×64 RGBA** textures using the standard Minecraft player skin UV layout, plus dedicated 3D hair and 3D eye extension sheets in unused UV areas.

## UV Mapping

| Region | UV Range (64x64) | Purpose |
|---|---|---|
| Player Skin Base | `[0, 0]` to `[64, 32]` | Standard Minecraft player skin (Head, Body, Arms, Legs) |
| Player Skin Overlays | Standard Overlay UVs | Jacket, hat/hair layer, sleeves, and pants overlays |
| 3D Hair Sheet | `[0, 32]` to `[16, 48]` | Dedicated realistic 3D hair strands and highlights |
| 3D Eye & Brow Sheet | `[32, 32]` to `[40, 48]` | Dedicated realistic 3D eye whites, irises, pupils, glints, eyelids, and eyebrows |

## Editing boundary

- Preserve `64×64`, RGBA, lossless PNG output.
- Keep standard Minecraft player skin UV coordinates for base body and overlays.
- 3D hair volume and 3D eye/brow cuboids in Geo models sample from the dedicated extension sheets.
