#!/usr/bin/env python3
"""Generate 64x64 character textures from 64x64 skins with 3D hair & eye sheets.

This migrates the character presentation from 512px to authentic 64x64 Minecraft
player skins, enriched with realistic 3D hair textures and realistic 3D eyes/eyebrows
in the unused 64x64 UV regions.
"""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import random

from PIL import Image, ImageDraw, ImageFont

from generate_gecko_assets import TEXTURE_SIZE

ROOT = Path(__file__).resolve().parents[1]
PERSONAL_DIR = ROOT / "src/main/resources/assets/riftcompanions/textures/entity/personal"
PUBLIC_DIR = ROOT / "src/main/resources/assets/riftcompanions/textures/entity/public"
DOCS = ROOT / "docs"
PREVIEW = DOCS / "faceted_character_texture_preview.png"

for directory in (PERSONAL_DIR, PUBLIC_DIR, DOCS):
    directory.mkdir(parents=True, exist_ok=True)


@dataclass(frozen=True)
class CharacterConfig:
    role: str
    label: str
    file_name: str
    source_64: str
    iris_color: tuple[int, int, int]
    hair_tint: tuple[int, int, int]


CHARACTERS = (
    CharacterConfig(
        role="seer",
        label="WILL / SEER",
        file_name="will_seer.png",
        source_64="2026_08_05_will-byers-st4-24252076.png",
        iris_color=(95, 75, 55),    # Warm brown iris
        hair_tint=(120, 92, 90),
    ),
    CharacterConfig(
        role="guardian",
        label="HOPPER / GUARDIAN",
        file_name="hopper_sheriff.png",
        source_64="2022_11_22_---keep-on-growing-up--kid--don---t-let-me-stop-you--------jim-hopper-21072102.png",
        iris_color=(100, 85, 65),   # Hazel brown iris
        hair_tint=(168, 131, 138),
    ),
    CharacterConfig(
        role="gifted",
        label="ELEVEN / GIFTED",
        file_name="eleven_gifted.png",
        source_64="2026_04_11_jane-hopper-byers-23987497.png",
        iris_color=(85, 70, 55),    # Deep brown iris
        hair_tint=(107, 86, 77),
    ),
    CharacterConfig(
        role="scout",
        label="MAX / SCOUT",
        file_name="max_scout.png",
        source_64="2026_07_15_madmax-24197127.png",
        iris_color=(75, 115, 135),  # Ocean blue/green iris
        hair_tint=(196, 119, 80),
    ),
)


def clamp(v: float) -> int:
    return max(0, min(255, int(round(v))))


def mix(a: tuple[int, int, int], b: tuple[int, int, int], amount: float) -> tuple[int, int, int]:
    return tuple(clamp(x * (1 - amount) + y * amount) for x, y in zip(a, b))


def sample_skin_color(base_im: Image.Image) -> tuple[int, int, int]:
    """Sample average facial skin tone from the 64x64 skin."""
    face = base_im.crop((10, 10, 14, 14)).convert("RGB")
    pixels = [face.getpixel((x, y)) for y in range(4) for x in range(4)]
    if not pixels:
        return (210, 180, 165)
    r = sum(p[0] for p in pixels) // len(pixels)
    g = sum(p[1] for p in pixels) // len(pixels)
    b = sum(p[2] for p in pixels) // len(pixels)
    return (r, g, b)


def sample_hair_color(base_im: Image.Image) -> tuple[int, int, int]:
    """Sample average hair tone from the 64x64 skin top head."""
    top = base_im.crop((8, 0, 16, 8)).convert("RGB")
    pixels = [top.getpixel((x, y)) for y in range(8) for x in range(8)]
    if not pixels:
        return (100, 80, 70)
    r = sum(p[0] for p in pixels) // len(pixels)
    g = sum(p[1] for p in pixels) // len(pixels)
    b = sum(p[2] for p in pixels) // len(pixels)
    return (r, g, b)


def paint_3d_hair_sheet(image: Image.Image, hair_color: tuple[int, int, int], rng: random.Random) -> None:
    """Paint dedicated realistic 3D hair textures in unused UV area (0, 32)-(16, 48)."""
    draw = ImageDraw.Draw(image)
    light = tuple(min(255, c + 35) for c in hair_color)
    dark = tuple(max(0, c - 35) for c in hair_color)
    for py in range(32, 48):
        for px in range(0, 16):
            noise = rng.randint(-15, 15) / 100.0
            t = clamp((0.5 + noise) * 255)
            c = mix(dark, light, t / 255.0)
            image.putpixel((px, py), (*c, 255))
    # Strand highlights
    for _ in range(5):
        sx = rng.randint(1, 14)
        draw.line((sx, 32, sx, 47), fill=(*light, 120), width=1)


def paint_3d_eye_sheet(image: Image.Image, iris_color: tuple[int, int, int], skin_color: tuple[int, int, int], hair_color: tuple[int, int, int], rng: random.Random) -> None:
    """Paint dedicated realistic 3D eye/eyebrow textures in unused UV area (32, 32)-(40, 48)."""
    draw = ImageDraw.Draw(image)
    # 1) Eye whites (32, 32, 2, 1)
    draw.rectangle((32, 32, 33, 32), fill=(242, 244, 238, 255))
    # 2) Iris & Pupil (34, 32, 1, 1)
    draw.point((34, 32), fill=(*iris_color, 255))
    # 3) Specular Catchlight Glint (36, 32, 1, 1)
    draw.point((36, 32), fill=(255, 255, 255, 255))
    # 4) Eyelids (32, 34, 2, 1)
    lid_col = tuple(max(0, c - 15) for c in skin_color)
    draw.rectangle((32, 34, 33, 34), fill=(*lid_col, 255))
    # 5) Eyebrows (34, 34, 3, 1)
    brow_col = tuple(max(0, c - 20) for c in hair_color)
    draw.rectangle((34, 34, 36, 34), fill=(*brow_col, 255))
    # 6) Neutral mouth (36, 36, 2, 1)
    lip_col = mix(skin_color, (180, 110, 105), 0.45)
    draw.rectangle((36, 36, 37, 36), fill=(*lip_col, 255))


def enhance_skin_atlas(atlas: Image.Image, config: CharacterConfig, rng: random.Random) -> None:
    """Add realistic 3D hair/eye sheets to the authentic 64x64 skin."""
    skin_color = sample_skin_color(atlas)
    hair_color = sample_hair_color(atlas)
    paint_3d_hair_sheet(atlas, hair_color, rng)
    paint_3d_eye_sheet(atlas, config.iris_color, skin_color, hair_color, rng)


def build_character_texture(config: CharacterConfig) -> Image.Image:
    src_path = PERSONAL_DIR / config.source_64
    atlas = Image.open(src_path).convert("RGBA").copy()
    if atlas.size != (64, 64):
        atlas = atlas.resize((64, 64), Image.Resampling.NEAREST)
    rng = random.Random(config.role)
    enhance_skin_atlas(atlas, config, rng)
    return atlas


def font(size: int) -> ImageFont.ImageFont:
    try:
        return ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", size)
    except OSError:
        return ImageFont.load_default()


def preview(entries: list[tuple[CharacterConfig, Image.Image]]) -> None:
    margin, tile, title_h = 22, 256, 74
    canvas = Image.new("RGBA", (2 * (tile + margin) + margin, title_h + 2 * (tile + 56 + margin) + margin), (15, 21, 30, 255))
    draw = ImageDraw.Draw(canvas)
    draw.text((margin, 18), "Rift Companions — 64x64 Character Skins (3D Hair & Eyes)", fill=(231, 239, 247, 255), font=font(22))
    draw.text((margin, 49), "Authentic 64x64 Minecraft player skins with 3D hair and 3D eye extension sheets.", fill=(158, 183, 203, 255), font=font(12))
    for index, (config, image) in enumerate(entries):
        x = margin + (index % 2) * (tile + margin)
        y = title_h + margin + (index // 2) * (tile + 56 + margin)
        canvas.alpha_composite(image.resize((tile, tile), Image.Resampling.NEAREST), (x, y))
        draw.rectangle((x, y, x + tile - 1, y + tile - 1), outline=(109, 137, 161, 255), width=2)
        draw.text((x, y + tile + 9), config.label, fill=(231, 239, 247, 255), font=font(17))
        draw.text((x, y + tile + 32), "64x64 Minecraft player skin with 3D hair/eye extensions", fill=(151, 177, 197, 255), font=font(11))
    canvas.save(PREVIEW)


def write_docs() -> None:
    (DOCS / "TEXTURE_UV_LAYOUT.md").write_text(
        """# 64×64 Player Skin UV Layout

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
""",
        encoding="utf-8",
    )
    (DOCS / "TEXTURE_ART_DIRECTION_CONTRACT.md").write_text(
        """# Character Visual Direction Contract

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
""",
        encoding="utf-8",
    )


def main() -> None:
    review: list[tuple[CharacterConfig, Image.Image]] = []
    for config in CHARACTERS:
        atlas = build_character_texture(config)
        atlas.save(PERSONAL_DIR / config.file_name)
        review.append((config, atlas))
        public_name = f"{config.role}_public.png"
        atlas.save(PUBLIC_DIR / public_name)
    preview(review)
    write_docs()
    print("Deleted and replaced all character texture atlases with 64px player skins + 3D hair/eye sheets.")


if __name__ == "__main__":
    main()
