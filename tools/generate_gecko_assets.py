#!/usr/bin/env python3
"""Generate 3D GeckoLib geometry for Rift Companions using 64px player skins.

This generator creates high-resolution 3D character models mapped to 64x64
Minecraft player skins, enriched with realistic 3D hair volume/strands and
realistic 3D eyes/eyebrows in the unused 64x64 UV areas.

The canonical animation backbone remains stable so gameplay-facing animation
selection continues to work. Role-prefixed visual bones are presentation-only.
"""
from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
GEO = ROOT / "src/main/resources/assets/riftcompanions/geo"
GEO.mkdir(parents=True, exist_ok=True)
TEXTURE_SIZE = 64

# ATLAS defines standard 64x64 regions for standard Minecraft player skins
# plus dedicated 3D extension UV sheets for realistic 3D hair and 3D eyes.
ATLAS: dict[str, tuple[int, int, int, int]] = {
    "head_front": (8, 8, 8, 8),
    "head_back": (24, 8, 8, 8),
    "head_left": (16, 8, 8, 8),
    "head_right": (0, 8, 8, 8),
    "head_top": (8, 0, 8, 8),
    "head_bottom": (16, 0, 8, 8),
    "skin_tile": (8, 8, 8, 8),
    "hair_tile": (40, 0, 8, 8),
    "eye_tile": (32, 32, 4, 2),
    "metal_tile": (4, 20, 2, 2),
    "torso_front": (20, 20, 8, 12),
    "torso_back": (32, 20, 8, 12),
    "torso_left": (28, 20, 4, 12),
    "torso_right": (16, 20, 4, 12),
    "torso_top": (20, 16, 8, 4),
    "torso_bottom": (28, 16, 8, 4),
    "jacket_tile": (20, 36, 8, 12),
    "shirt_tile": (20, 20, 8, 12),
    "pants_tile": (4, 20, 4, 12),
    "leather_tile": (4, 20, 4, 4),
    "arm_left_front": (36, 52, 4, 12),
    "arm_left_back": (44, 52, 4, 12),
    "arm_right_front": (44, 20, 4, 12),
    "arm_right_back": (52, 20, 4, 12),
    "sleeve_tile": (44, 36, 4, 12),
    "cuff_tile": (44, 40, 4, 4),
    "field_tile": (20, 36, 8, 12),
    "accent_tile": (36, 32, 2, 2),
    "thread_tile": (38, 32, 2, 2),
    "boot_tile": (4, 29, 4, 3),
    "pack_tile": (20, 36, 4, 6),
    "hand_left": (36, 58, 4, 4),
    "hand_right": (44, 26, 4, 4),
    "leg_left_front": (20, 52, 4, 12),
    "leg_left_back": (28, 52, 4, 12),
    "leg_right_front": (4, 20, 4, 12),
    "leg_right_back": (12, 20, 4, 12),
    "knee_tile": (4, 25, 4, 4),
    "sole_tile": (4, 29, 4, 2),
    "cloth_tile": (20, 20, 8, 12),
    "utility_tile": (20, 36, 8, 12),
    "signal_tile": (4, 20, 2, 2),
    "weather_tile": (20, 36, 8, 12),
    "spare_tile": (4, 20, 4, 4),
}


@dataclass(frozen=True)
class Build:
    role: str
    torso_width: float
    torso_depth: float
    shoulder_width: float
    arm_width: float
    leg_width: float
    head_width: float
    outer: str
    inner: str
    trouser: str


BUILDS = {
    "seer": Build("seer", 8.0, 4.0, 8.0, 4.0, 4.0, 8.0, "jacket_tile", "shirt_tile", "pants_tile"),
    "guardian": Build("guardian", 8.0, 4.0, 8.0, 4.0, 4.0, 8.0, "jacket_tile", "shirt_tile", "pants_tile"),
    "gifted": Build("gifted", 8.0, 4.0, 8.0, 4.0, 4.0, 8.0, "jacket_tile", "shirt_tile", "pants_tile"),
    "scout": Build("scout", 8.0, 4.0, 8.0, 4.0, 4.0, 8.0, "jacket_tile", "shirt_tile", "pants_tile"),
}


def box_faces(base_u: int, base_v: int, w: int, h: int, d: int) -> dict[str, Any]:
    """Return explicit UV faces for a standard box of dimensions w*h*d at base_u, base_v."""
    return {
        "up": {"uv": [base_u + d, base_v], "uv_size": [w, d]},
        "down": {"uv": [base_u + d + w, base_v], "uv_size": [w, d]},
        "east": {"uv": [base_u, base_v + d], "uv_size": [d, h]},
        "north": {"uv": [base_u + d, base_v + d], "uv_size": [w, h]},
        "west": {"uv": [base_u + d + w, base_v + d], "uv_size": [d, h]},
        "south": {"uv": [base_u + d + w + d, base_v + d], "uv_size": [w, h]},
    }


def cube(origin: list[float], size: list[float], surface: dict[str, Any] | list[int], *, inflate: float = 0.0) -> dict[str, Any]:
    payload: dict[str, Any] = {"origin": origin, "size": size, "uv": surface}
    if inflate:
        payload["inflate"] = inflate
    return payload


def bone(name: str, parent: str | None, pivot: list[float], cubes: list[dict[str, Any]] | None = None) -> dict[str, Any]:
    payload: dict[str, Any] = {"name": name, "pivot": pivot}
    if parent:
        payload["parent"] = parent
    if cubes:
        payload["cubes"] = cubes
    return payload


# Dedicated 3D Hair and 3D Eye/Eyebrow UV coordinates in 64x64 unused sheet
HAIR_3D_SURFACE = box_faces(0, 32, 4, 4, 4)
EYE_WHITE_SURFACE = box_faces(32, 32, 2, 1, 1)
IRIS_SURFACE = box_faces(34, 32, 1, 1, 1)
GLINT_SURFACE = box_faces(36, 32, 1, 1, 1)
LID_SURFACE = box_faces(32, 34, 2, 1, 1)
BROW_SURFACE = box_faces(34, 34, 3, 1, 1)
MOUTH_SURFACE = box_faces(36, 36, 2, 1, 1)


def sleeve_layers(spec: Build) -> list[dict[str, Any]]:
    half_arm = spec.arm_width / 2
    arm_x = 5.0
    arm_depth = max(4.0, spec.arm_width)
    return [
        bone(f"{spec.role}_left_upper_sleeve", "left_arm_upper", [arm_x, 19.2, 0], [
            cube([arm_x - half_arm, 16.0, -arm_depth / 2], [4, 6, 4], box_faces(48, 48, 4, 12, 4), inflate=0.25),
        ]),
        bone(f"{spec.role}_right_upper_sleeve", "right_arm_upper", [-arm_x, 19.2, 0], [
            cube([-arm_x - half_arm, 16.0, -arm_depth / 2], [4, 6, 4], box_faces(40, 32, 4, 12, 4), inflate=0.25),
        ]),
        bone(f"{spec.role}_left_lower_sleeve", "left_arm_lower", [arm_x, 13.8, 0], [
            cube([arm_x - half_arm, 12.0, -arm_depth / 2], [4, 4, 4], box_faces(48, 48, 4, 12, 4), inflate=0.25),
        ]),
        bone(f"{spec.role}_right_lower_sleeve", "right_arm_lower", [-arm_x, 13.8, 0], [
            cube([-arm_x - half_arm, 12.0, -arm_depth / 2], [4, 4, 4], box_faces(40, 32, 4, 12, 4), inflate=0.25),
        ]),
        bone(f"{spec.role}_left_cuff", "left_arm_lower", [arm_x, 11.8, 0], [
            cube([arm_x - half_arm, 10.0, -arm_depth / 2], [4, 2, 4], box_faces(48, 48, 4, 12, 4), inflate=0.28),
        ]),
        bone(f"{spec.role}_right_cuff", "right_arm_lower", [-arm_x, 11.8, 0], [
            cube([-arm_x - half_arm, 10.0, -arm_depth / 2], [4, 2, 4], box_faces(40, 32, 4, 12, 4), inflate=0.28),
        ]),
    ]


def anatomy(spec: Build) -> list[dict[str, Any]]:
    """Standard 3D humanoid body mapped to standard 64x64 Minecraft player skin UVs with face details."""
    half_head = spec.head_width / 2
    return [
        bone("root", None, [0, 0, 0]),
        bone("body", "root", [0, 12, 0]),
        bone("chest", "body", [0, 20, 0], [
            cube([-4, 12, -2], [8, 12, 4], box_faces(16, 16, 8, 12, 4)),
            cube([-4, 12, -2], [8, 12, 4], box_faces(16, 32, 8, 12, 4), inflate=0.25),
        ]),
        bone("torso_ribcage", "chest", [0, 19.0, 0], [
            cube([-4, 15, -2], [8, 6, 4], box_faces(16, 16, 8, 12, 4), inflate=0.01),
        ]),
        bone("torso_waist", "chest", [0, 14.0, 0], [
            cube([-3.8, 12, -1.8], [7.6, 3, 3.6], box_faces(16, 16, 8, 12, 4), inflate=0.01),
        ]),
        bone("neck", "chest", [0, 24, 0]),
        bone("head", "chest", [0, 24, 0]),
        bone("face_core", "head", [0, 28.0, 0], [
            cube([-half_head, 24, -half_head], [spec.head_width, spec.head_width, spec.head_width], box_faces(0, 0, 8, 8, 8)),
            cube([-half_head, 24, -half_head], [spec.head_width, spec.head_width, spec.head_width], box_faces(32, 0, 8, 8, 8), inflate=0.5),
        ]),
        bone("face_forehead", "head", [0, 31.0, 0], [
            cube([-3.6, 30.2, -4.08], [7.2, 1.4, 0.4], box_faces(8, 8, 8, 8, 8), inflate=0.01),
        ]),
        bone("face_left_cheek", "head", [-half_head, 28.0, 0], [
            cube([-4.1, 25.5, -3.8], [0.6, 3.5, 6.8], box_faces(8, 8, 8, 8, 8), inflate=0.01),
        ]),
        bone("face_right_cheek", "head", [half_head, 28.0, 0], [
            cube([3.5, 25.5, -3.8], [0.6, 3.5, 6.8], box_faces(8, 8, 8, 8, 8), inflate=0.01),
        ]),
        bone("face_left_ear", "head", [-4.4, 27.6, 0], [
            cube([-4.6, 26.2, -0.6], [0.6, 2.4, 1.4], box_faces(8, 8, 8, 8, 8), inflate=0.02),
        ]),
        bone("face_right_ear", "head", [4.4, 27.6, 0], [
            cube([4.0, 26.2, -0.6], [0.6, 2.4, 1.4], box_faces(8, 8, 8, 8, 8), inflate=0.02),
        ]),
        bone("jaw", "head", [0, 24, -4]),
        bone("face_chin", "jaw", [0, 25.3, 0], [
            cube([-3.2, 24.1, -4.1], [6.4, 1.6, 1.6], box_faces(8, 8, 8, 8, 8), inflate=0.01),
        ]),
        bone("nose_bridge", "head", [0, 27.9, -half_head], [
            cube([-0.5, 26.8, -4.3], [1.0, 2.0, 0.6], box_faces(8, 8, 8, 8, 8), inflate=0.01),
        ]),
        bone("nose_tip", "head", [0, 27.0, -half_head], [
            cube([-0.8, 26.4, -4.5], [1.6, 0.8, 0.8], box_faces(8, 8, 8, 8, 8), inflate=0.01),
        ]),
        bone("mouth_neutral", "jaw", [0, 25.5, -4.1], [
            cube([-1.5, 25.0, -4.12], [3.0, 0.6, 0.12], MOUTH_SURFACE, inflate=0.01),
        ]),
        # 3D Eyes and Eyebrows modeled realistically on front face
        bone("eye_left_white", "head", [-2.2, 28.0, -4.1], [
            cube([-3.1, 27.4, -4.12], [1.8, 1.2, 0.22], EYE_WHITE_SURFACE, inflate=0.01),
        ]),
        bone("eye_right_white", "head", [2.2, 28.0, -4.1], [
            cube([1.3, 27.4, -4.12], [1.8, 1.2, 0.22], EYE_WHITE_SURFACE, inflate=0.01),
        ]),
        bone("eye_left_pupil", "head", [-2.2, 28.0, -4.2], [
            cube([-2.7, 27.6, -4.22], [0.8, 0.8, 0.15], IRIS_SURFACE, inflate=0.01),
        ]),
        bone("eye_right_pupil", "head", [2.2, 28.0, -4.2], [
            cube([1.9, 27.6, -4.22], [0.8, 0.8, 0.15], IRIS_SURFACE, inflate=0.01),
        ]),
        bone("eye_left_glint", "eye_left_pupil", [-2.2, 28.0, -4.25], [
            cube([-2.4, 28.0, -4.28], [0.3, 0.3, 0.10], GLINT_SURFACE, inflate=0.01),
        ]),
        bone("eye_right_glint", "eye_right_pupil", [2.2, 28.0, -4.25], [
            cube([2.2, 28.0, -4.28], [0.3, 0.3, 0.10], GLINT_SURFACE, inflate=0.01),
        ]),
        bone("eye_left_upper_lid", "head", [-2.2, 28.6, -4.15], [
            cube([-3.2, 28.4, -4.18], [2.0, 0.4, 0.20], LID_SURFACE, inflate=0.01),
        ]),
        bone("eye_right_upper_lid", "head", [2.2, 28.6, -4.15], [
            cube([1.2, 28.4, -4.18], [2.0, 0.4, 0.20], LID_SURFACE, inflate=0.01),
        ]),
        bone("eye_left_lower_lid", "head", [-2.2, 27.4, -4.15], [
            cube([-3.2, 27.2, -4.18], [2.0, 0.3, 0.20], LID_SURFACE, inflate=0.01),
        ]),
        bone("eye_right_lower_lid", "head", [2.2, 27.4, -4.15], [
            cube([1.2, 27.2, -4.18], [2.0, 0.3, 0.20], LID_SURFACE, inflate=0.01),
        ]),
        bone("brow_left", "head", [-2.2, 29.3, -4.2], [
            cube([-3.4, 29.0, -4.25], [2.4, 0.7, 0.30], BROW_SURFACE, inflate=0.01),
        ]),
        bone("brow_right", "head", [2.2, 29.3, -4.2], [
            cube([1.0, 29.0, -4.25], [2.4, 0.7, 0.30], BROW_SURFACE, inflate=0.01),
        ]),
        # Left arm (segmented upper/lower/hand mapped to standard 64x64 left arm coordinates)
        bone("left_arm", "chest", [5.0, 22, 0]),
        bone("left_arm_upper", "left_arm", [5.0, 22, 0], [
            cube([4.0, 16.0, -2.0], [4, 6, 4], box_faces(32, 48, 4, 12, 4)),
        ]),
        bone("left_arm_lower", "left_arm_upper", [5.0, 16.0, 0], [
            cube([4.0, 12.0, -2.0], [4, 4, 4], box_faces(32, 48, 4, 12, 4)),
        ]),
        bone("left_hand", "left_arm_lower", [5.0, 12.0, 0], [
            cube([4.0, 10.0, -2.0], [4, 2, 4], box_faces(32, 48, 4, 12, 4)),
        ]),
        # Right arm (segmented upper/lower/hand mapped to standard 64x64 right arm coordinates)
        bone("right_arm", "chest", [-5.0, 22, 0]),
        bone("right_arm_upper", "right_arm", [-5.0, 22, 0], [
            cube([-8.0, 16.0, -2.0], [4, 6, 4], box_faces(40, 16, 4, 12, 4)),
        ]),
        bone("right_arm_lower", "right_arm_upper", [-5.0, 16.0, 0], [
            cube([-8.0, 12.0, -2.0], [4, 4, 4], box_faces(40, 16, 4, 12, 4)),
        ]),
        bone("right_hand", "right_arm_lower", [-5.0, 12.0, 0], [
            cube([-8.0, 10.0, -2.0], [4, 2, 4], box_faces(40, 16, 4, 12, 4)),
        ]),
        # Left leg (segmented upper/lower/foot mapped to standard 64x64 left leg coordinates)
        bone("left_leg", "root", [2.0, 12, 0]),
        bone("left_leg_upper", "left_leg", [2.0, 12, 0], [
            cube([0.0, 6.0, -2.0], [4, 6, 4], box_faces(16, 48, 4, 12, 4)),
            cube([0.0, 6.0, -2.0], [4, 6, 4], box_faces(0, 48, 4, 12, 4), inflate=0.25),
        ]),
        bone("left_leg_lower", "left_leg_upper", [2.0, 6.0, 0], [
            cube([0.0, 2.0, -2.0], [4, 4, 4], box_faces(16, 48, 4, 12, 4)),
            cube([0.0, 2.0, -2.0], [4, 4, 4], box_faces(0, 48, 4, 12, 4), inflate=0.25),
        ]),
        bone("left_foot", "left_leg_lower", [2.0, 2.0, 0], [
            cube([0.0, 0.0, -2.0], [4, 2, 4], box_faces(16, 48, 4, 12, 4)),
        ]),
        # Right leg (segmented upper/lower/foot mapped to standard 64x64 right leg coordinates)
        bone("right_leg", "root", [-2.0, 12, 0]),
        bone("right_leg_upper", "right_leg", [-2.0, 12, 0], [
            cube([-4.0, 6.0, -2.0], [4, 6, 4], box_faces(0, 16, 4, 12, 4)),
            cube([-4.0, 6.0, -2.0], [4, 6, 4], box_faces(0, 32, 4, 12, 4), inflate=0.25),
        ]),
        bone("right_leg_lower", "right_leg_upper", [-2.0, 6.0, 0], [
            cube([-4.0, 2.0, -2.0], [4, 4, 4], box_faces(0, 16, 4, 12, 4)),
            cube([-4.0, 2.0, -2.0], [4, 4, 4], box_faces(0, 32, 4, 12, 4), inflate=0.25),
        ]),
        bone("right_foot", "right_leg_lower", [-2.0, 2.0, 0], [
            cube([-4.0, 0.0, -2.0], [4, 2, 4], box_faces(0, 16, 4, 12, 4)),
        ]),
        bone("accessory_1", "head", [0, 24, 0]),
        bone("accessory_2", "chest", [0, 20, 0]),
    ]


def seer_visuals(spec: Build) -> list[dict[str, Any]]:
    return [
        # Realistic 3D layered 80s bowl cut hair with volume on top, bangs, and side locks
        bone("seer_hair_crown", "accessory_1", [0, 31.5, 0], [cube([-4.3, 31.2, -4.3], [8.6, 1.2, 8.6], HAIR_3D_SURFACE, inflate=0.05)]),
        bone("seer_hair_back", "accessory_1", [0, 27.0, 4.0], [cube([-4.2, 23.0, 3.8], [8.4, 7.5, 1.4], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("seer_hair_left_01", "accessory_1", [-4.2, 28.5, -3.0], [cube([-4.6, 26.0, -3.8], [1.2, 4.5, 7.4], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("seer_hair_left_02", "seer_hair_left_01", [-4.4, 25.5, -3.0], [cube([-4.7, 23.2, -3.7], [1.1, 2.8, 6.0], HAIR_3D_SURFACE, inflate=0.02)]),
        bone("seer_hair_center_01", "accessory_1", [-2.0, 29.0, -4.2], [cube([-4.2, 28.0, -4.5], [4.4, 3.2, 1.2], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("seer_hair_center_02", "accessory_1", [2.0, 29.0, -4.2], [cube([-0.2, 28.0, -4.5], [4.4, 3.2, 1.2], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("seer_hair_right_01", "accessory_1", [4.2, 28.5, -3.0], [cube([3.4, 26.0, -3.8], [1.2, 4.5, 7.4], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("seer_hair_right_02", "seer_hair_right_01", [4.4, 25.5, -3.0], [cube([3.6, 23.2, -3.7], [1.1, 2.8, 6.0], HAIR_3D_SURFACE, inflate=0.02)]),
        # Clothing motion layers
        bone("seer_flannel_left", "accessory_2", [2.1, 18.0, -2.0], [cube([0.1, 13.5, -2.1], [3.0, 8.5, 0.4], box_faces(16, 32, 3, 8, 1), inflate=0.01)]),
        bone("seer_flannel_right", "accessory_2", [-2.1, 18.0, -2.0], [cube([-3.1, 13.5, -2.1], [3.0, 8.5, 0.4], box_faces(16, 32, 3, 8, 1), inflate=0.01)]),
        bone("seer_tshirt_front", "accessory_2", [0, 18.0, -2.2], [cube([-1.5, 14.0, -2.25], [3.0, 7.5, 0.3], box_faces(16, 16, 3, 7, 1), inflate=0.01)]),
        bone("seer_shirt_tail", "accessory_2", [0, 13.2, 2.0], [cube([-4.0, 11.0, 1.8], [8.0, 2.5, 0.4], box_faces(16, 32, 8, 4, 1), inflate=0.01)]),
        bone("seer_collar_left", "accessory_2", [2.5, 21.5, -2.1], [cube([0.5, 20.0, -2.3], [3.0, 2.5, 0.4], box_faces(16, 32, 4, 3, 1), inflate=0.01)]),
        bone("seer_collar_right", "accessory_2", [-2.5, 21.5, -2.1], [cube([-3.5, 20.0, -2.3], [3.0, 2.5, 0.4], box_faces(16, 32, 4, 3, 1), inflate=0.01)]),
        *sleeve_layers(spec),
    ]


def guardian_visuals(spec: Build) -> list[dict[str, Any]]:
    return [
        # Realistic 3D sheriff officer hair, sideburns, and beard/stubble layer
        bone("guardian_hair_short", "accessory_1", [0, 31.5, 0], [cube([-4.2, 31.2, -4.2], [8.4, 1.4, 8.4], HAIR_3D_SURFACE, inflate=0.04)]),
        bone("guardian_hair_back", "accessory_1", [0, 27.5, 4.0], [cube([-4.2, 25.0, 3.8], [8.4, 5.5, 1.2], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("guardian_sideburn_left", "accessory_1", [-4.2, 27.5, -1.0], [cube([-4.5, 25.0, -2.0], [0.8, 4.0, 4.0], HAIR_3D_SURFACE, inflate=0.02)]),
        bone("guardian_sideburn_right", "accessory_1", [4.2, 27.5, -1.0], [cube([3.7, 25.0, -2.0], [0.8, 4.0, 4.0], HAIR_3D_SURFACE, inflate=0.02)]),
        bone("guardian_beard", "jaw", [0, 25.5, -4.0], [cube([-4.1, 24.0, -4.3], [8.2, 3.0, 1.0], HAIR_3D_SURFACE, inflate=0.02)]),
        # Clothing motion layers
        bone("guardian_workcoat_front", "accessory_2", [0, 18.0, -2.2], [cube([-3.9, 13.0, -2.25], [7.8, 9.5, 0.4], box_faces(16, 32, 8, 9, 1), inflate=0.01)]),
        bone("guardian_workcoat_back", "accessory_2", [0, 18.0, 2.1], [cube([-3.9, 13.0, 1.85], [7.8, 9.5, 0.5], box_faces(16, 32, 8, 9, 1), inflate=0.01)]),
        bone("guardian_shirt_front", "accessory_2", [0, 18.0, -2.3], [cube([-1.4, 14.0, -2.5], [2.8, 8.0, 0.3], box_faces(16, 16, 3, 8, 1), inflate=0.01)]),
        bone("guardian_belt", "accessory_2", [0, 13.5, 0], [cube([-3.8, 12.7, -2.15], [7.6, 1.2, 4.3], box_faces(16, 32, 8, 1, 4), inflate=0.01)]),
        bone("guardian_buckle", "guardian_belt", [0, 13.4, -2.3], [cube([-0.8, 12.9, -2.5], [1.6, 0.9, 0.3], box_faces(16, 32, 2, 1, 1), inflate=0.01)]),
        bone("guardian_watch", "left_arm_lower", [3.9, 12.0, -1.6], [cube([3.5, 11.4, -2.1], [1.3, 1.1, 0.4], box_faces(48, 48, 1, 1, 1), inflate=0.01)]),
        bone("guardian_coat_tail", "accessory_2", [0, 13.0, 2.1], [cube([-4.1, 10.5, 1.9], [8.2, 2.8, 0.5], box_faces(16, 32, 8, 4, 1), inflate=0.01)]),
        bone("guardian_collar_left", "accessory_2", [2.6, 21.5, -2.2], [cube([0.4, 19.8, -2.5], [3.2, 2.8, 0.5], box_faces(16, 32, 4, 3, 1), inflate=0.01)]),
        bone("guardian_collar_right", "accessory_2", [-2.6, 21.5, -2.2], [cube([-3.6, 19.8, -2.5], [3.2, 2.8, 0.5], box_faces(16, 32, 4, 3, 1), inflate=0.01)]),
        *sleeve_layers(spec),
    ]


def gifted_visuals(spec: Build) -> list[dict[str, Any]]:
    return [
        # Realistic 3D layered hair with bangs, side locks, crown volume, and back locks
        bone("gifted_hair_crown", "accessory_1", [0, 31.5, 0], [cube([-4.3, 31.2, -4.3], [8.6, 1.3, 8.6], HAIR_3D_SURFACE, inflate=0.05)]),
        bone("gifted_hair_back", "accessory_1", [0, 27.0, 4.0], [cube([-4.2, 20.0, 3.8], [8.4, 9.5, 1.5], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("gifted_hair_left_01", "accessory_1", [-4.2, 28.0, -3.0], [cube([-4.6, 24.5, -3.8], [1.2, 6.0, 7.4], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("gifted_hair_left_02", "gifted_hair_left_01", [-4.4, 24.5, -3.0], [cube([-4.7, 21.0, -3.7], [1.1, 4.0, 5.8], HAIR_3D_SURFACE, inflate=0.02)]),
        bone("gifted_hair_center", "accessory_1", [0, 29.0, -4.2], [cube([-4.2, 27.8, -4.5], [8.4, 3.4, 1.2], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("gifted_hair_right_01", "accessory_1", [4.2, 28.0, -3.0], [cube([3.4, 24.5, -3.8], [1.2, 6.0, 7.4], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("gifted_hair_right_02", "gifted_hair_right_01", [4.4, 24.5, -3.0], [cube([3.6, 21.0, -3.7], [1.1, 4.0, 5.8], HAIR_3D_SURFACE, inflate=0.02)]),
        # Clothing motion layers
        bone("gifted_denim_front", "accessory_2", [0, 18.0, -2.1], [cube([-3.3, 13.2, -2.12], [6.6, 9.5, 0.4], box_faces(16, 32, 7, 9, 1), inflate=0.01)]),
        bone("gifted_denim_back", "accessory_2", [0, 18.0, 2.05], [cube([-3.3, 13.2, 1.72], [6.6, 9.5, 0.5], box_faces(16, 32, 7, 9, 1), inflate=0.01)]),
        bone("gifted_tshirt_front", "accessory_2", [0, 17.9, -2.25], [cube([-1.3, 14.0, -2.45], [2.6, 7.8, 0.3], box_faces(16, 16, 3, 8, 1), inflate=0.01)]),
        bone("gifted_zipper", "accessory_2", [0, 17.9, -2.35], [cube([-0.15, 13.4, -2.6], [0.3, 9.1, 0.2], box_faces(16, 32, 1, 9, 1), inflate=0.01)]),
        bone("gifted_front_pocket", "accessory_2", [2.0, 15.9, -2.15], [cube([0.7, 14.2, -2.35], [2.1, 2.3, 0.3], box_faces(16, 32, 2, 2, 1), inflate=0.01)]),
        bone("gifted_watch", "right_arm_lower", [-3.9, 12.0, -1.6], [cube([-4.8, 11.4, -2.1], [1.3, 1.1, 0.4], box_faces(40, 32, 1, 1, 1), inflate=0.01)]),
        bone("gifted_jacket_tail", "accessory_2", [0, 13.0, 2.0], [cube([-4.0, 10.8, 1.8], [8.0, 2.5, 0.4], box_faces(16, 32, 8, 4, 1), inflate=0.01)]),
        bone("gifted_collar_left", "accessory_2", [2.5, 21.5, -2.1], [cube([0.5, 19.8, -2.4], [3.0, 2.6, 0.4], box_faces(16, 32, 4, 3, 1), inflate=0.01)]),
        bone("gifted_collar_right", "accessory_2", [-2.5, 21.5, -2.1], [cube([-3.5, 19.8, -2.4], [3.0, 2.6, 0.4], box_faces(16, 32, 4, 3, 1), inflate=0.01)]),
        *sleeve_layers(spec),
    ]


def scout_visuals(spec: Build) -> list[dict[str, Any]]:
    return [
        # Realistic 3D auburn flowing hair with full bangs across forehead and side locks
        bone("scout_hair_crown", "accessory_1", [0, 31.5, 0], [cube([-4.3, 31.2, -4.3], [8.6, 1.3, 8.6], HAIR_3D_SURFACE, inflate=0.05)]),
        bone("scout_hair_back", "accessory_1", [0, 27.0, 4.0], [cube([-4.2, 18.0, 3.8], [8.4, 11.5, 1.5], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("scout_hair_left_01", "accessory_1", [-4.2, 28.0, -3.0], [cube([-4.6, 23.5, -3.8], [1.2, 7.0, 7.4], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("scout_hair_left_02", "scout_hair_left_01", [-4.4, 23.5, -3.0], [cube([-4.7, 19.0, -3.7], [1.1, 5.0, 5.8], HAIR_3D_SURFACE, inflate=0.02)]),
        bone("scout_hair_front_left", "accessory_1", [-2.1, 29.0, -4.2], [cube([-4.3, 27.6, -4.6], [4.3, 3.6, 1.3], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("scout_hair_front_right", "accessory_1", [2.1, 29.0, -4.2], [cube([0.0, 27.6, -4.6], [4.3, 3.6, 1.3], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("scout_hair_right_01", "accessory_1", [4.2, 28.0, -3.0], [cube([3.4, 23.5, -3.8], [1.2, 7.0, 7.4], HAIR_3D_SURFACE, inflate=0.03)]),
        bone("scout_hair_right_02", "scout_hair_right_01", [4.4, 23.5, -3.0], [cube([3.6, 19.0, -3.7], [1.1, 5.0, 5.8], HAIR_3D_SURFACE, inflate=0.02)]),
        # Clothing motion layers
        bone("scout_jacket_front", "accessory_2", [0, 18.0, -2.1], [cube([-3.4, 13.2, -2.15], [6.8, 9.5, 0.4], box_faces(16, 32, 7, 9, 1), inflate=0.01)]),
        bone("scout_jacket_back", "accessory_2", [0, 18.0, 2.05], [cube([-3.4, 13.2, 1.72], [6.8, 9.5, 0.5], box_faces(16, 32, 7, 9, 1), inflate=0.01)]),
        bone("scout_tshirt_front", "accessory_2", [0, 17.9, -2.25], [cube([-1.4, 14.0, -2.48], [2.8, 7.8, 0.3], box_faces(16, 16, 3, 8, 1), inflate=0.01)]),
        bone("scout_zipper", "accessory_2", [0, 17.9, -2.35], [cube([-0.15, 13.4, -2.62], [0.3, 9.1, 0.2], box_faces(16, 32, 1, 9, 1), inflate=0.01)]),
        bone("scout_left_pocket", "accessory_2", [2.0, 15.9, -2.15], [cube([0.7, 14.2, -2.38], [2.1, 2.3, 0.3], box_faces(16, 32, 2, 2, 1), inflate=0.01)]),
        bone("scout_right_pocket", "accessory_2", [-2.0, 15.9, -2.15], [cube([-2.8, 14.2, -2.38], [2.1, 2.3, 0.3], box_faces(16, 32, 2, 2, 1), inflate=0.01)]),
        bone("scout_watch", "left_arm_lower", [3.9, 12.0, -1.6], [cube([3.5, 11.4, -2.1], [1.3, 1.1, 0.4], box_faces(48, 48, 1, 1, 1), inflate=0.01)]),
        bone("scout_jacket_tail", "accessory_2", [0, 13.0, 2.0], [cube([-4.1, 10.8, 1.8], [8.2, 2.5, 0.4], box_faces(16, 32, 8, 4, 1), inflate=0.01)]),
        bone("scout_collar_left", "accessory_2", [2.5, 21.5, -2.1], [cube([0.5, 19.8, -2.4], [3.0, 2.6, 0.4], box_faces(16, 32, 4, 3, 1), inflate=0.01)]),
        bone("scout_collar_right", "accessory_2", [-2.5, 21.5, -2.1], [cube([-3.5, 19.8, -2.4], [3.0, 2.6, 0.4], box_faces(16, 32, 4, 3, 1), inflate=0.01)]),
        *sleeve_layers(spec),
    ]


def visual_bones(spec: Build) -> list[dict[str, Any]]:
    if spec.role == "seer":
        return seer_visuals(spec)
    if spec.role == "guardian":
        return guardian_visuals(spec)
    if spec.role == "gifted":
        return gifted_visuals(spec)
    if spec.role == "scout":
        return scout_visuals(spec)
    raise ValueError(spec.role)


def model(role: str) -> dict[str, Any]:
    spec = BUILDS[role]
    bones = anatomy(spec) + visual_bones(spec)
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": f"geometry.riftcompanions.{role}",
                "texture_width": TEXTURE_SIZE,
                "texture_height": TEXTURE_SIZE,
                "visible_bounds_width": 4.45,
                "visible_bounds_height": 4.65,
                "visible_bounds_offset": [0, 1.70, 0],
                "riftcompanions_rig_contract": "shared_humanoid_v2",
                "riftcompanions_visual_contract": "faceted_character_64_v2",
            },
            "bones": bones,
        }],
    }


def main() -> None:
    for role in BUILDS:
        (GEO / f"{role}.geo.json").write_text(json.dumps(model(role), indent=2) + "\n", encoding="utf-8")
    print("Generated four 3D GeckoLib models with 64px player skin UV mapping and realistic 3D hair/eye bones.")


if __name__ == "__main__":
    main()
