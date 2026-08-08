#!/usr/bin/env python3
"""Render a source-review board for the original faceted companion rig.

This illustration is derived from the project UV materials and geometry plan. It
is not a live GeckoLib/Minecraft render and must not be treated as runtime proof.
"""
from __future__ import annotations

from io import BytesIO
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageStat

from generate_gecko_assets import ATLAS

ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/riftcompanions/textures/entity/personal"
OUTPUT = ROOT / "docs/faceted_character_model_preview.png"
ROLES = (
    ("seer", "will_seer.png", "WILL / SEER", "Quiet layered silhouette"),
    ("guardian", "hopper_sheriff.png", "HOPPER / GUARDIAN", "Broad grounded workwear"),
    ("gifted", "eleven_gifted.png", "ELEVEN / GIFTED", "Controlled denim layers"),
    ("scout", "max_scout.png", "MAX / SCOUT", "Fast casual silhouette"),
)


def font(size: int) -> ImageFont.ImageFont:
    try:
        return ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", size)
    except OSError:
        return ImageFont.load_default()


def crop(texture: Image.Image, name: str) -> Image.Image:
    x, y, width, height = ATLAS[name]
    return texture.crop((x, y, x + width, y + height))


def color(texture: Image.Image, name: str) -> tuple[int, int, int, int]:
    mean = ImageStat.Stat(crop(texture, name).convert("RGB")).mean
    return tuple(int(value) for value in mean) + (255,)


def shade(colour: tuple[int, int, int, int], factor: float) -> tuple[int, int, int, int]:
    return tuple(max(0, min(255, int(value * factor))) for value in colour[:3]) + (colour[3],)


def clipped_face(texture: Image.Image, size: int) -> Image.Image:
    base = crop(texture, "head_front").resize((size, size), Image.Resampling.NEAREST)
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    # Eight-sided tapered face: the rendered silhouette no longer uses a square head.
    inset = int(size * 0.15)
    draw.polygon(((inset, 0), (size - inset, 0), (size, inset), (size, size - inset),
                  (size - inset, size), (inset, size), (0, size - inset), (0, inset)), fill=255)
    output = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    output.paste(base, (0, 0), mask)
    return output


def polygon(draw: ImageDraw.ImageDraw, points: list[tuple[int, int]], fill: tuple[int, int, int, int], outline: tuple[int, int, int, int] | None = None, width: int = 1) -> None:
    draw.polygon(points, fill=fill)
    if outline:
        draw.line(points + [points[0]], fill=outline, width=width, joint="curve")


def character_card(role: str, texture: Image.Image, label: str, caption: str) -> Image.Image:
    width, height = 350, 540
    card = Image.new("RGBA", (width, height), (18, 25, 35, 255))
    draw = ImageDraw.Draw(card)
    skin = color(texture, "skin_tile")
    hair = color(texture, "hair_tile")
    eye = color(texture, "eye_tile")
    iris = color(texture, "accent_tile")
    outer = color(texture, "jacket_tile")
    inner = color(texture, "shirt_tile")
    pants = color(texture, "pants_tile")
    leather = color(texture, "leather_tile")
    metal = color(texture, "metal_tile")
    outline = (9, 13, 19, 255)
    cx = width // 2

    # Ground shadow.
    draw.ellipse((cx - 75, 445, cx + 75, 465), fill=(4, 7, 10, 145))
    # Rear hair layers appear before the faceted face.
    if role in {"gifted", "scout"}:
        polygon(draw, [(cx - 54, 116), (cx + 54, 116), (cx + 48, 234), (cx + 29, 256), (cx - 31, 256), (cx - 50, 234)], shade(hair, 0.74), outline, 2)
    elif role == "seer":
        polygon(draw, [(cx - 48, 120), (cx + 48, 120), (cx + 41, 221), (cx - 42, 221)], shade(hair, 0.76), outline, 2)
    else:
        polygon(draw, [(cx - 48, 129), (cx + 48, 129), (cx + 41, 202), (cx - 41, 202)], shade(hair, 0.72), outline, 2)

    # Tapered lower body and segmented legs.
    polygon(draw, [(cx - 47, 313), (cx - 6, 313), (cx - 11, 430), (cx - 52, 430)], pants, outline, 2)
    polygon(draw, [(cx + 6, 313), (cx + 47, 313), (cx + 52, 430), (cx + 11, 430)], pants, outline, 2)
    polygon(draw, [(cx - 55, 425), (cx - 10, 425), (cx - 4, 448), (cx - 60, 448)], leather, outline, 2)
    polygon(draw, [(cx + 10, 425), (cx + 55, 425), (cx + 60, 448), (cx + 4, 448)], leather, outline, 2)

    # Articulated arms with small elbow changes rather than solid full-length blocks.
    left_arm = [(cx - 87, 232), (cx - 55, 222), (cx - 48, 293), (cx - 78, 302)]
    right_arm = [(cx + 55, 222), (cx + 87, 232), (cx + 78, 302), (cx + 48, 293)]
    polygon(draw, left_arm, outer, outline, 2)
    polygon(draw, right_arm, outer, outline, 2)
    polygon(draw, [(cx - 78, 302), (cx - 48, 293), (cx - 46, 340), (cx - 71, 346)], shade(outer, 0.93), outline, 2)
    polygon(draw, [(cx + 48, 293), (cx + 78, 302), (cx + 71, 346), (cx + 46, 340)], shade(outer, 0.93), outline, 2)
    draw.rectangle((cx - 72, 339, cx - 48, 351), fill=skin, outline=outline, width=1)
    draw.rectangle((cx + 48, 339, cx + 72, 351), fill=skin, outline=outline, width=1)

    # Tapered torso with an independent front layer and collar planes.
    polygon(draw, [(cx - 61, 208), (cx + 61, 208), (cx + 50, 325), (cx - 50, 325)], outer, outline, 2)
    polygon(draw, [(cx - 26, 225), (cx + 26, 225), (cx + 21, 315), (cx - 21, 315)], inner, outline, 1)
    polygon(draw, [(cx - 55, 212), (cx - 20, 227), (cx - 29, 249), (cx - 50, 239)], shade(outer, 0.72), outline, 1)
    polygon(draw, [(cx + 55, 212), (cx + 20, 227), (cx + 29, 249), (cx + 50, 239)], shade(outer, 0.72), outline, 1)
    draw.line((cx, 221, cx, 317), fill=metal, width=2)

    # Round/faceted head is assembled from face planes; no square head outline is used.
    face = clipped_face(texture, 106)
    card.alpha_composite(face, (cx - 53, 91))
    # Hair masses differ by role but use only original stylized geometry.
    if role == "guardian":
        polygon(draw, [(cx - 47, 111), (cx - 26, 88), (cx + 29, 88), (cx + 49, 111), (cx + 43, 131), (cx - 43, 131)], hair, outline, 2)
        polygon(draw, [(cx - 37, 168), (cx + 37, 168), (cx + 30, 192), (cx - 30, 192)], shade(hair, 0.78), outline, 1)
    elif role == "seer":
        for x, y, w, h in ((-47, 99, 24, 62), (-22, 93, 25, 52), (3, 95, 25, 56), (28, 101, 22, 58)):
            polygon(draw, [(cx + x, y), (cx + x + w, y), (cx + x + w - 5, y + h), (cx + x + 4, y + h)], hair, outline, 1)
    elif role == "gifted":
        for x, y, w, h in ((-48, 99, 24, 80), (-23, 94, 27, 58), (4, 94, 28, 58), (28, 99, 23, 80)):
            polygon(draw, [(cx + x, y), (cx + x + w, y), (cx + x + w - 5, y + h), (cx + x + 4, y + h)], hair, outline, 1)
    else:
        for x, y, w, h in ((-49, 98, 23, 83), (-24, 94, 25, 56), (2, 94, 25, 56), (27, 98, 23, 83)):
            polygon(draw, [(cx + x, y), (cx + x + w, y), (cx + x + w - 5, y + h), (cx + x + 4, y + h)], hair, outline, 1)

    # Separate eyes, iris/pupil, lids, brows, nose and mouth mirror actual rig parts.
    for eye_x in (cx - 25, cx + 14):
        draw.rounded_rectangle((eye_x, 138, eye_x + 17, 148), radius=3, fill=eye, outline=outline, width=1)
        draw.ellipse((eye_x + 6, 140, eye_x + 12, 146), fill=iris, outline=outline)
        draw.ellipse((eye_x + 8, 141, eye_x + 11, 145), fill=(13, 17, 21, 255))
        draw.line((eye_x, 137, eye_x + 17, 136), fill=hair, width=2)
        draw.line((eye_x + 1, 149, eye_x + 16, 150), fill=shade(skin, 0.84), width=1)
    polygon(draw, [(cx - 3, 147), (cx + 3, 147), (cx + 5, 162), (cx - 4, 162)], shade(skin, 0.82), outline, 1)
    draw.line((cx - 13, 176, cx + 13, 176), fill=shade(hair, 0.45), width=2)

    # Role-level grounded details.
    if role == "seer":
        for y in (244, 262, 280):
            draw.line((cx - 58, y, cx - 33, y + 3), fill=shade(inner, 1.28), width=2)
            draw.line((cx + 33, y + 3, cx + 58, y), fill=shade(inner, 1.28), width=2)
    elif role == "guardian":
        draw.line((cx - 55, 310, cx + 55, 310), fill=leather, width=4)
        draw.rectangle((cx - 8, 307, cx + 8, 315), fill=metal, outline=outline, width=1)
    elif role == "gifted":
        for y in (248, 266, 284):
            draw.line((cx - 18, y, cx + 18, y), fill=shade(outer, 1.22), width=1)
    else:
        draw.rectangle((cx - 54, 273, cx - 33, 295), fill=shade(outer, 0.88), outline=outline, width=1)
        draw.rectangle((cx + 33, 273, cx + 54, 295), fill=shade(outer, 0.88), outline=outline, width=1)

    draw.rectangle((8, 8, width - 9, height - 9), outline=shade(outer, 1.38), width=1)
    draw.text((18, 478), label, fill=(235, 241, 247, 255), font=font(17))
    draw.text((18, 504), caption, fill=shade(outer, 1.50), font=font(11))
    return card


def main() -> None:
    card_w, card_h = 350, 540
    canvas = Image.new("RGBA", (4 * card_w + 54, card_h + 104), (14, 20, 29, 255))
    draw = ImageDraw.Draw(canvas)
    draw.text((24, 19), "Rift Companions — Faceted Character Rig Review", fill=(233, 240, 247, 255), font=font(28))
    draw.text((24, 58), "Tapered head • separate eye/lid/brow/jaw bones • layered hair • articulated body. Source review only, not a live Minecraft render.",
              fill=(160, 185, 205, 255), font=font(12))
    for index, (role, texture_name, label, caption) in enumerate(ROLES):
        texture = Image.open(TEXTURES / texture_name).convert("RGBA")
        card = character_card(role, texture, label, caption)
        canvas.alpha_composite(card, (15 + index * card_w, 95))
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(OUTPUT)
    print(f"Wrote faceted character source preview: {OUTPUT}")


if __name__ == "__main__":
    main()
