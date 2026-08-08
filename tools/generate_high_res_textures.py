#!/usr/bin/env python3
"""Generate original 512px textures for the faceted Rift Companions character rig.

The PNG atlases are technical UV sheets for an original rounded/faceted model,
not visible Steve-style square skins. Eyes, lids, brows, mouth, hair locks, and
clothing layers are separate Geo cuboids mapped to material islands here.

No source image is read. No actor face, screen costume, logo, or third-party
Yes Steve Model asset is copied, traced, transferred, or embedded.
"""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import random
import zlib

from PIL import Image, ImageDraw, ImageFont

from generate_gecko_assets import ATLAS, TEXTURE_SIZE

ROOT = Path(__file__).resolve().parents[1]
PERSONAL_DIR = ROOT / "src/main/resources/assets/riftcompanions/textures/entity/personal"
PUBLIC_DIR = ROOT / "src/main/resources/assets/riftcompanions/textures/entity/public"
DOCS = ROOT / "docs"
PREVIEW = DOCS / "faceted_character_texture_preview.png"

for directory in (PERSONAL_DIR, PUBLIC_DIR, DOCS):
    directory.mkdir(parents=True, exist_ok=True)


@dataclass(frozen=True)
class Palette:
    file_name: str
    label: str
    role: str
    skin: tuple[int, int, int]
    skin_shadow: tuple[int, int, int]
    skin_light: tuple[int, int, int]
    hair: tuple[int, int, int]
    hair_light: tuple[int, int, int]
    iris: tuple[int, int, int]
    outer: tuple[int, int, int]
    outer_light: tuple[int, int, int]
    inner: tuple[int, int, int]
    denim: tuple[int, int, int]
    pants: tuple[int, int, int]
    leather: tuple[int, int, int]
    metal: tuple[int, int, int]
    accent: tuple[int, int, int]


# Original, non-portrait palettes. They intentionally describe only broad,
# grounded late-1980s character archetypes and never reproduce screen pixels.
PERSONAL_PALETTES = (
    # Hand-authored palettes informed by the supplied references: hair, eye and
    # everyday clothing direction only — no photo pixels or portrait transfer.
    Palette("will_seer.png", "WILL / SEER", "seer", (205, 171, 151), (118, 82, 70), (239, 205, 182),
            (48, 37, 31), (105, 78, 60), (91, 104, 83), (73, 83, 92), (132, 145, 152),
            (104, 61, 53), (55, 67, 83), (43, 52, 66), (61, 45, 36), (136, 137, 130), (150, 51, 47)),
    Palette("hopper_sheriff.png", "HOPPER / GUARDIAN", "guardian", (202, 151, 122), (112, 72, 55), (232, 183, 148),
            (82, 67, 53), (145, 126, 101), (83, 105, 122), (113, 100, 75), (163, 148, 110),
            (225, 218, 196), (61, 76, 87), (49, 59, 68), (83, 59, 42), (157, 153, 139), (118, 91, 53)),
    Palette("eleven_gifted.png", "ELEVEN / GIFTED", "gifted", (220, 174, 151), (135, 83, 70), (247, 206, 182),
            (57, 42, 36), (117, 82, 64), (100, 80, 50), (72, 88, 104), (131, 151, 166),
            (194, 172, 153), (65, 78, 92), (48, 58, 72), (72, 52, 42), (154, 151, 140), (135, 72, 73)),
    Palette("max_scout.png", "MAX / SCOUT", "scout", (232, 169, 134), (173, 91, 72), (250, 201, 168),
            (168, 67, 39), (232, 126, 72), (80, 112, 101), (72, 91, 108), (120, 145, 162),
            (209, 74, 57), (65, 83, 101), (45, 58, 75), (73, 51, 37), (150, 147, 135), (217, 164, 72)),
)

PUBLIC_PALETTES = (
    Palette("seer_public.png", "SEER", "seer", (193, 160, 149), (111, 77, 76), (224, 190, 177),
            (73, 62, 56), (126, 105, 90), (87, 104, 104), (65, 92, 115), (119, 151, 171),
            (137, 96, 83), (56, 71, 88), (51, 65, 82), (74, 58, 49), (137, 139, 138), (151, 132, 104)),
    Palette("guardian_public.png", "GUARDIAN", "guardian", (188, 139, 114), (102, 68, 58), (220, 170, 141),
            (71, 70, 66), (144, 146, 138), (88, 84, 70), (78, 71, 56), (127, 116, 90),
            (106, 99, 87), (70, 76, 74), (50, 59, 59), (78, 60, 45), (148, 151, 143), (139, 130, 105)),
    Palette("gifted_public.png", "GIFTED", "gifted", (204, 168, 156), (123, 82, 82), (235, 202, 187),
            (76, 64, 56), (131, 108, 91), (84, 99, 93), (69, 99, 130), (125, 153, 180),
            (204, 197, 183), (74, 101, 129), (49, 64, 82), (67, 56, 51), (144, 147, 146), (147, 108, 94)),
    Palette("scout_public.png", "SCOUT", "scout", (223, 158, 129), (160, 87, 74), (244, 190, 159),
            (151, 67, 42), (216, 123, 77), (81, 115, 100), (103, 71, 66), (157, 107, 91),
            (82, 112, 136), (88, 72, 69), (50, 66, 82), (79, 60, 46), (145, 146, 143), (181, 152, 90)),
)


def clamp(value: float) -> int:
    return max(0, min(255, int(round(value))))


def mix(a: tuple[int, int, int], b: tuple[int, int, int], amount: float) -> tuple[int, int, int]:
    return tuple(clamp(first * (1 - amount) + second * amount) for first, second in zip(a, b))


def dark(color: tuple[int, int, int], amount: float = 0.55) -> tuple[int, int, int]:
    return tuple(clamp(value * amount) for value in color)


def rgba(color: tuple[int, int, int], alpha: int = 255) -> tuple[int, int, int, int]:
    return (*color, alpha)


def rect(name: str) -> tuple[int, int, int, int]:
    return ATLAS[name]


def fill(image: Image.Image, name: str, base: tuple[int, int, int], high: tuple[int, int, int],
         low: tuple[int, int, int], rng: random.Random, grain: int = 3) -> None:
    x, y, width, height = rect(name)
    pixels = image.load()
    for py in range(y, y + height):
        vertical = (py - y) / max(1, height - 1)
        light_factor = 0.12 - vertical * 0.20
        for px in range(x, x + width):
            horizontal = (px - x) / max(1, width - 1)
            edge = min(px - x, x + width - 1 - px, py - y, y + height - 1 - py)
            noise = rng.randint(-grain, grain) / 100.0
            value = light_factor + (horizontal - 0.5) * 0.04 + noise - (0.10 if edge < 2 else 0.0)
            color = mix(base, high, value / 0.22) if value >= 0 else mix(base, low, -value / 0.33)
            pixels[px, py] = rgba(color)


def stitch(draw: ImageDraw.ImageDraw, start: tuple[int, int], end: tuple[int, int], color: tuple[int, int, int], step: int = 5) -> None:
    dx, dy = end[0] - start[0], end[1] - start[1]
    length = max(abs(dx), abs(dy))
    for offset in range(0, length + 1, step * 2):
        first = offset / max(1, length)
        second = min(1.0, (offset + step) / max(1, length))
        draw.line((round(start[0] + dx * first), round(start[1] + dy * first),
                   round(start[0] + dx * second), round(start[1] + dy * second)), fill=rgba(color), width=1)


def material_tiles(image: Image.Image, palette: Palette, rng: random.Random) -> None:
    draw = ImageDraw.Draw(image)
    mapping = (
        ("skin_tile", palette.skin, palette.skin_light, palette.skin_shadow, 2),
        ("hair_tile", palette.hair, palette.hair_light, dark(palette.hair, 0.42), 4),
        ("eye_tile", (227, 229, 222), (250, 252, 246), (163, 170, 166), 1),
        ("metal_tile", palette.metal, mix(palette.metal, (235, 238, 232), 0.35), dark(palette.metal, 0.44), 2),
        ("jacket_tile", palette.outer, palette.outer_light, dark(palette.outer, 0.44), 4),
        ("shirt_tile", palette.inner, mix(palette.inner, (236, 231, 218), 0.22), dark(palette.inner, 0.45), 3),
        ("pants_tile", palette.pants, mix(palette.pants, (180, 190, 198), 0.16), dark(palette.pants, 0.45), 3),
        ("leather_tile", palette.leather, mix(palette.leather, (207, 164, 117), 0.22), dark(palette.leather, 0.42), 3),
        ("sleeve_tile", palette.outer, palette.outer_light, dark(palette.outer, 0.44), 4),
        ("cuff_tile", dark(palette.outer, 0.72), palette.outer, dark(palette.outer, 0.34), 3),
        ("field_tile", palette.denim, mix(palette.denim, (170, 187, 198), 0.22), dark(palette.denim, 0.46), 3),
        ("accent_tile", palette.iris, mix(palette.iris, (220, 230, 220), 0.36), dark(palette.iris, 0.35), 2),
        ("thread_tile", dark(palette.hair, 0.35), palette.hair, dark(palette.hair, 0.20), 2),
        ("boot_tile", dark(palette.leather, 0.66), palette.leather, dark(palette.leather, 0.25), 3),
        ("pack_tile", palette.outer, palette.outer_light, dark(palette.outer, 0.45), 3),
        ("knee_tile", palette.pants, mix(palette.pants, (160, 170, 179), 0.16), dark(palette.pants, 0.44), 3),
        ("sole_tile", dark(palette.leather, 0.50), palette.leather, dark(palette.leather, 0.22), 2),
        ("cloth_tile", palette.inner, mix(palette.inner, (225, 206, 190), 0.18), dark(palette.inner, 0.45), 3),
        ("utility_tile", palette.outer, palette.outer_light, dark(palette.outer, 0.48), 3),
        ("signal_tile", palette.metal, mix(palette.metal, (220, 220, 210), 0.22), dark(palette.metal, 0.42), 2),
        ("weather_tile", palette.outer, mix(palette.outer, (190, 166, 135), 0.15), dark(palette.outer, 0.48), 4),
        ("spare_tile", palette.leather, mix(palette.leather, (205, 170, 126), 0.18), dark(palette.leather, 0.44), 3),
    )
    for name, base, high, low, grain in mapping:
        fill(image, name, base, high, low, rng, grain)
        x, y, width, height = rect(name)
        if name in {"hair_tile", "jacket_tile", "field_tile", "cloth_tile", "weather_tile", "sleeve_tile"}:
            for line_y in range(y + 5, y + height - 3, 8):
                draw.line((x + 3, line_y, x + width - 4, line_y), fill=rgba(mix(base, high, 0.20)), width=1)
        if name in {"leather_tile", "boot_tile", "metal_tile"}:
            stitch(draw, (x + 4, y + 4), (x + width - 5, y + 4), mix(base, high, 0.45))


def face_panels(image: Image.Image, palette: Palette, rng: random.Random) -> None:
    # No painted eyes or copied facial appearance: geometry supplies independent eyes/lids/brows.
    for name, base, high, low in (
        ("head_front", palette.skin, palette.skin_light, palette.skin_shadow),
        ("head_back", palette.skin_shadow, palette.skin, dark(palette.skin_shadow, 0.56)),
        ("head_left", palette.skin_shadow, palette.skin, dark(palette.skin_shadow, 0.56)),
        ("head_right", palette.skin_shadow, palette.skin, dark(palette.skin_shadow, 0.56)),
        ("head_top", palette.skin, palette.skin_light, palette.skin_shadow),
        ("head_bottom", palette.skin_shadow, palette.skin, dark(palette.skin_shadow, 0.60)),
    ):
        fill(image, name, base, high, low, rng, 2)
    draw = ImageDraw.Draw(image)
    x, y, w, h = rect("head_front")
    # Soft structural planes: bridge/cheek/mouth shadows stay abstract and non-portrait.
    draw.polygon(((x + 44, y + 34), (x + 52, y + 34), (x + 49, y + 67)), fill=rgba(mix(palette.skin_shadow, palette.skin, 0.42)))
    draw.line((x + 28, y + 69, x + 41, y + 72), fill=rgba(mix(palette.skin_shadow, palette.skin, 0.30)), width=1)
    draw.line((x + 56, y + 72, x + 69, y + 69), fill=rgba(mix(palette.skin_shadow, palette.skin, 0.30)), width=1)
    draw.line((x + 39, y + 82, x + 58, y + 82), fill=rgba(mix(palette.skin_shadow, palette.skin, 0.46)), width=1)


def garment_panels(image: Image.Image, palette: Palette, rng: random.Random) -> None:
    draw = ImageDraw.Draw(image)
    for name, base, high, low in (
        ("torso_front", palette.outer, palette.outer_light, dark(palette.outer, 0.46)),
        ("torso_back", palette.outer, palette.outer_light, dark(palette.outer, 0.46)),
        ("torso_left", palette.outer, palette.outer_light, dark(palette.outer, 0.46)),
        ("torso_right", palette.outer, palette.outer_light, dark(palette.outer, 0.46)),
        ("torso_top", palette.outer_light, palette.outer, dark(palette.outer, 0.45)),
        ("torso_bottom", dark(palette.outer, 0.70), palette.outer, dark(palette.outer, 0.34)),
        ("arm_left_front", palette.outer, palette.outer_light, dark(palette.outer, 0.46)),
        ("arm_left_back", palette.outer, palette.outer_light, dark(palette.outer, 0.46)),
        ("arm_right_front", palette.outer, palette.outer_light, dark(palette.outer, 0.46)),
        ("arm_right_back", palette.outer, palette.outer_light, dark(palette.outer, 0.46)),
        ("leg_left_front", palette.pants, mix(palette.pants, (170, 180, 190), 0.16), dark(palette.pants, 0.46)),
        ("leg_left_back", palette.pants, mix(palette.pants, (170, 180, 190), 0.16), dark(palette.pants, 0.46)),
        ("leg_right_front", palette.pants, mix(palette.pants, (170, 180, 190), 0.16), dark(palette.pants, 0.46)),
        ("leg_right_back", palette.pants, mix(palette.pants, (170, 180, 190), 0.16), dark(palette.pants, 0.46)),
        ("hand_left", palette.skin_shadow, palette.skin, dark(palette.skin_shadow, 0.50)),
        ("hand_right", palette.skin_shadow, palette.skin, dark(palette.skin_shadow, 0.50)),
    ):
        fill(image, name, base, high, low, rng, 3)

    x, y, w, h = rect("torso_front")
    if palette.role == "seer":
        # Checked overshirt and tee, expressed as fabric construction rather than props.
        draw.rectangle((x + 33, y + 22, x + 62, y + 114), fill=rgba(palette.inner))
        for offset in range(30, 112, 13):
            draw.line((x + 6, y + offset, x + 32, y + offset + 3), fill=rgba(mix(palette.inner, palette.outer_light, 0.35)), width=2)
            draw.line((x + 64, y + offset + 3, x + w - 7, y + offset), fill=rgba(mix(palette.inner, palette.outer_light, 0.35)), width=2)
    elif palette.role == "guardian":
        draw.polygon(((x + 4, y + 8), (x + 33, y + 22), (x + 44, y + 68), (x + 10, y + 57)), fill=rgba(dark(palette.outer, 0.48)))
        draw.polygon(((x + w - 4, y + 8), (x + w - 33, y + 22), (x + w - 44, y + 68), (x + w - 10, y + 57)), fill=rgba(dark(palette.outer, 0.48)))
        draw.rectangle((x + 37, y + 23, x + 59, y + 113), fill=rgba(palette.inner))
        for button_y in (45, 67, 89):
            draw.ellipse((x + 45, y + button_y, x + 51, y + button_y + 6), fill=rgba(dark(palette.leather, 0.55)))
    elif palette.role == "gifted":
        draw.rectangle((x + 33, y + 23, x + 62, y + 113), fill=rgba(palette.inner))
        for stripe_y in range(y + 32, y + 108, 13):
            draw.line((x + 35, stripe_y, x + 60, stripe_y), fill=rgba(mix(palette.inner, palette.accent, 0.30)), width=2)
        stitch(draw, (x + 10, y + 18), (x + 24, y + 111), mix(palette.outer, palette.outer_light, 0.30))
        stitch(draw, (x + w - 10, y + 18), (x + w - 24, y + 111), mix(palette.outer, palette.outer_light, 0.30))
    else:
        draw.rectangle((x + 34, y + 23, x + 62, y + 113), fill=rgba(palette.inner))
        draw.line((x + w // 2, y + 14, x + w // 2, y + 128), fill=rgba(dark(palette.outer, 0.46)), width=2)
        for pocket_x in (13, 61):
            draw.rounded_rectangle((x + pocket_x, y + 73, x + pocket_x + 20, y + 96), radius=2, fill=rgba(mix(palette.outer, palette.outer_light, 0.12)), outline=rgba(dark(palette.outer, 0.48)))

    for arm in ("arm_left_front", "arm_left_back", "arm_right_front", "arm_right_back"):
        ax, ay, aw, ah = rect(arm)
        draw.line((ax + aw // 2, ay + 5, ax + aw // 2, ay + ah - 6), fill=rgba(dark(palette.outer, 0.42)), width=1)
    for leg in ("leg_left_front", "leg_left_back", "leg_right_front", "leg_right_back"):
        lx, ly, lw, lh = rect(leg)
        draw.line((lx + lw // 2, ly + 4, lx + lw // 2, ly + lh - 6), fill=rgba(dark(palette.pants, 0.42)), width=1)


def character_finish(image: Image.Image, palette: Palette) -> None:
    """Add readable fabric and hair direction to the role's existing UV islands."""
    draw = ImageDraw.Draw(image)
    # Fine, non-repeating hair fibres keep the layered hair cuboids from reading as flat blocks.
    x, y, w, h = rect("hair_tile")
    strand = mix(palette.hair, palette.hair_light, 0.58)
    shade = dark(palette.hair, 0.52)
    for offset in range(3, w - 2, 4):
        draw.line((x + offset, y + 3, x + max(2, offset - 5), y + h - 3), fill=rgba(strand), width=1)
    draw.line((x + 2, y + h - 4, x + w - 3, y + h - 4), fill=rgba(shade), width=1)

    # Eyes and iris materials are tiled independently by the animated face bones.
    ex, ey, ew, eh = rect("eye_tile")
    draw.rounded_rectangle((ex + 4, ey + 7, ex + ew - 5, ey + eh - 7), radius=5, fill=rgba((235, 238, 228)), outline=rgba((112, 99, 89)), width=1)
    ax, ay, aw, ah = rect("accent_tile")
    draw.ellipse((ax + 9, ay + 7, ax + aw - 10, ay + ah - 8), fill=rgba(palette.iris), outline=rgba(dark(palette.iris, .48)), width=2)
    tx, ty, tw, th = rect("thread_tile")
    draw.rectangle((tx, ty, tx + tw - 1, ty + th - 1), fill=rgba(dark(palette.hair, .34)))
    draw.ellipse((tx + 15, ty + 12, tx + 33, ty + 33), fill=rgba((25, 22, 21)))
    draw.ellipse((tx + 19, ty + 15, tx + 24, ty + 20), fill=rgba((232, 235, 226)))

    # Role clothes: deliberate seam, pocket, plaid and lining variations.
    if palette.role == "seer":
        for name in ("cloth_tile", "torso_front"):
            px, py, pw, ph = rect(name)
            for line in range(px + 8, px + pw, 13): draw.line((line, py + 3, line, py + ph - 4), fill=rgba(mix(palette.outer, palette.accent, .48)), width=1)
            for line in range(py + 8, py + ph, 13): draw.line((px + 3, line, px + pw - 4, line), fill=rgba(mix(palette.outer, palette.accent, .38)), width=1)
    elif palette.role == "guardian":
        px, py, pw, ph = rect("weather_tile")
        draw.line((px + pw // 2, py + 4, px + pw // 2, py + ph - 5), fill=rgba(dark(palette.outer, .48)), width=2)
        for yy in (py + 16, py + 31):
            draw.line((px + 4, yy, px + pw - 5, yy), fill=rgba(mix(palette.outer, palette.outer_light, .35)), width=1)
        px, py, pw, ph = rect("shirt_tile")
        for yy in range(py + 9, py + ph - 4, 11): draw.line((px + 4, yy, px + pw - 5, yy), fill=rgba(mix(palette.inner, palette.skin_light, .25)), width=1)
    elif palette.role == "gifted":
        px, py, pw, ph = rect("field_tile")
        for yy in range(py + 8, py + ph - 5, 9): draw.line((px + 3, yy, px + pw - 4, yy), fill=rgba(mix(palette.denim, palette.outer_light, .42)), width=1)
        draw.line((px + pw // 2, py + 3, px + pw // 2, py + ph - 4), fill=rgba(dark(palette.denim, .50)), width=1)
    else:
        px, py, pw, ph = rect("jacket_tile")
        draw.line((px + pw // 2, py + 3, px + pw // 2, py + ph - 4), fill=rgba(dark(palette.outer, .46)), width=2)
        draw.rectangle((px + 6, py + 24, px + pw - 7, py + 49), outline=rgba(mix(palette.outer, palette.outer_light, .48)), width=1)


def build_atlas(palette: Palette) -> Image.Image:
    rng = random.Random(zlib.crc32((palette.file_name + palette.label).encode("utf-8")))
    image = Image.new("RGBA", (TEXTURE_SIZE, TEXTURE_SIZE), (0, 0, 0, 0))
    material_tiles(image, palette, rng)
    face_panels(image, palette, rng)
    garment_panels(image, palette, rng)
    character_finish(image, palette)
    return image


def font(size: int) -> ImageFont.ImageFont:
    try:
        return ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", size)
    except OSError:
        return ImageFont.load_default()


def preview(entries: list[tuple[Palette, Image.Image]]) -> None:
    margin, tile, title_h = 22, 348, 74
    canvas = Image.new("RGBA", (2 * (tile + margin) + margin, title_h + 2 * (tile + 56 + margin) + margin), (15, 21, 30, 255))
    draw = ImageDraw.Draw(canvas)
    draw.text((margin, 18), "Rift Companions — Original Faceted Character Texture Atlases", fill=(231, 239, 247, 255), font=font(22))
    draw.text((margin, 49), "512px hand-authored UV sheets with readable hair fibres, eye materials, seams and clothing construction.", fill=(158, 183, 203, 255), font=font(12))
    for index, (palette, image) in enumerate(entries):
        x = margin + (index % 2) * (tile + margin)
        y = title_h + margin + (index // 2) * (tile + 56 + margin)
        canvas.alpha_composite(image.resize((tile, tile), Image.Resampling.NEAREST), (x, y))
        draw.rectangle((x, y, x + tile - 1, y + tile - 1), outline=(109, 137, 161, 255), width=2)
        draw.text((x, y + tile + 9), palette.label, fill=(231, 239, 247, 255), font=font(17))
        draw.text((x, y + tile + 32), "Original faceted character material set", fill=(151, 177, 197, 255), font=font(11))
    canvas.save(PREVIEW)


def write_docs() -> None:
    (DOCS / "TEXTURE_UV_LAYOUT.md").write_text(
        """# 512×512 Faceted Character UV Layout

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
""",
        encoding="utf-8",
    )
    (DOCS / "TEXTURE_ART_DIRECTION_CONTRACT.md").write_text(
        """# Faceted Character Visual Direction Contract

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
""",
        encoding="utf-8",
    )


def main() -> None:
    # Remove every old generated character texture before atomically replacing it.
    personal_dir = ROOT / "src/main/resources/assets/riftcompanions/textures/entity/personal"
    public_dir = ROOT / "src/main/resources/assets/riftcompanions/textures/entity/public"
    for palette in (*PERSONAL_PALETTES, *PUBLIC_PALETTES):
        target = (personal_dir if palette in PERSONAL_PALETTES else public_dir) / palette.file_name
        target.unlink(missing_ok=True)

    review: list[tuple[Palette, Image.Image]] = []
    for palette in PERSONAL_PALETTES:
        atlas = build_atlas(palette)
        atlas.save(personal_dir / palette.file_name)
        review.append((palette, atlas))
    for palette in PUBLIC_PALETTES:
        build_atlas(palette).save(public_dir / palette.file_name)
    preview(review)
    write_docs()
    print("Deleted and replaced all eight character texture atlases with original faceted-character 512px textures.")


if __name__ == "__main__":
    main()
