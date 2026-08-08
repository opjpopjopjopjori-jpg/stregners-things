# 512×512 HD Player Skin UV Layout

The companion atlases are **512×512 RGBA** textures using an 8x upscaled standard Minecraft player skin UV layout, plus dedicated 3D hair and 3D eye extension sheets.

## UV Mapping

| Region | UV Range (512x512) | Purpose |
|---|---|---|
| Player Skin Base | `[0, 0]` to `[512, 256]` | Standard 8x upscaled Minecraft player skin (Head, Body, Arms, Legs) |
| Player Skin Overlays | Standard Overlay UVs | Jacket, hat/hair layer, sleeves, and pants overlays |
| 3D Hair Sheet | `[0, 256]` to `[128, 384]` | Dedicated high-definition realistic 3D hair strands and highlights |
| 3D Eye & Brow Sheet | `[256, 256]` to `[320, 384]` | Dedicated realistic 3D eye whites, irises, pupils, glints, eyelids, and eyebrows |

## Editing boundary

- Preserve `512×512`, RGBA, lossless PNG output.
- Keep standard Minecraft player skin UV coordinates for base body and overlays.
- 3D hair volume and 3D eye/brow cuboids in Geo models sample from the dedicated extension sheets.
