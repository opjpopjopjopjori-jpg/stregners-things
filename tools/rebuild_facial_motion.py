#!/usr/bin/env python3
"""Build layered body, face, and secondary-motion GeckoLib clips.

The result is intentionally split across three non-overlapping controller bone
sets: body clips own canonical locomotion/action bones; face clips own eyes,
lids, brows, jaw, and mouth; secondary clips own hair/clothing layers. This
keeps blink, gaze, expression, and hair motion alive beneath movement, combat,
talk, recovery, and power poses without granting any animation gameplay power.
"""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
ANIM = ROOT / "src/main/resources/assets/riftcompanions/animations"
ROLES = ("seer", "guardian", "gifted", "scout")

HAIR: dict[str, list[str]] = {
    "seer": ["seer_hair_left_01", "seer_hair_left_02", "seer_hair_center_01", "seer_hair_center_02", "seer_hair_right_01", "seer_hair_right_02", "seer_hair_back"],
    "guardian": ["guardian_hair_short", "guardian_hair_back", "guardian_sideburn_left", "guardian_sideburn_right", "guardian_beard"],
    "gifted": ["gifted_hair_left_01", "gifted_hair_left_02", "gifted_hair_center", "gifted_hair_right_01", "gifted_hair_right_02", "gifted_hair_back"],
    "scout": ["scout_hair_left_01", "scout_hair_left_02", "scout_hair_front_left", "scout_hair_front_right", "scout_hair_right_01", "scout_hair_right_02", "scout_hair_back"],
}

CLOTH: dict[str, list[str]] = {
    "seer": ["seer_shirt_tail", "seer_collar_left", "seer_collar_right"],
    "guardian": ["guardian_coat_tail", "guardian_collar_left", "guardian_collar_right"],
    "gifted": ["gifted_jacket_tail", "gifted_collar_left", "gifted_collar_right"],
    "scout": ["scout_jacket_tail", "scout_collar_left", "scout_collar_right"],
}

FACE_BONES = {
    "head", "jaw", "mouth_neutral", "eye_left_pupil", "eye_right_pupil", "eye_left_glint", "eye_right_glint",
    "eye_left_upper_lid", "eye_right_upper_lid", "eye_left_lower_lid", "eye_right_lower_lid",
    "brow_left", "brow_right",
}

# Differentiated movement direction: reserved, heavy, precise, athletic.
STYLE = {
    "seer": {"walk_len": 0.76, "walk_leg": 22, "walk_arm": 15, "walk_bob": -0.18, "run_len": 0.60, "run_leg": 37, "run_arm": 25, "lean": 9, "hair": 3.0},
    "guardian": {"walk_len": 0.86, "walk_leg": 20, "walk_arm": 17, "walk_bob": -0.48, "run_len": 0.66, "run_leg": 33, "run_arm": 23, "lean": 14, "hair": 1.4},
    "gifted": {"walk_len": 0.72, "walk_leg": 25, "walk_arm": 13, "walk_bob": -0.24, "run_len": 0.57, "run_leg": 40, "run_arm": 22, "lean": 8, "hair": 3.5},
    "scout": {"walk_len": 0.62, "walk_leg": 32, "walk_arm": 25, "walk_bob": -0.31, "run_len": 0.46, "run_leg": 53, "run_arm": 40, "lean": 18, "hair": 5.0},
}


def rotation(*frames: tuple[float, list[float]]) -> dict[str, dict[str, list[float]]]:
    return {f"{at:.2f}": value for at, value in frames}


def position(*frames: tuple[float, list[float]]) -> dict[str, dict[str, list[float]]]:
    return {f"{at:.2f}": value for at, value in frames}


def animation(length: float, loop: bool, bones: dict[str, Any]) -> dict[str, Any]:
    return {"animation_length": length, "loop": loop, "bones": bones}


def key(role: str, name: str) -> str:
    return f"animation.{role}.{name}"


def strip_layered_bones(role: str, animations: dict[str, Any]) -> None:
    layered = FACE_BONES | set(HAIR[role]) | set(CLOTH[role])
    for name in list(animations):
        if name.startswith(f"animation.{role}.face_") or name.startswith(f"animation.{role}.secondary_"):
            animations.pop(name)
            continue
        bones = animations[name].get("bones", {})
        for bone in layered:
            bones.pop(bone, None)


def body_clips(role: str) -> dict[str, Any]:
    style = STYLE[role]
    wl, wa, wb, walk_length = style["walk_leg"], style["walk_arm"], style["walk_bob"], style["walk_len"]
    half_walk = walk_length / 2
    walk = animation(walk_length, True, {
        "left_leg": {"rotation": rotation((0.0, [wl, 0, 0]), (half_walk, [-wl, 0, 0]), (walk_length, [wl, 0, 0]))},
        "right_leg": {"rotation": rotation((0.0, [-wl, 0, 0]), (half_walk, [wl, 0, 0]), (walk_length, [-wl, 0, 0]))},
        "left_leg_lower": {"rotation": rotation((0.0, [6, 0, 0]), (half_walk, [22, 0, 0]), (walk_length, [6, 0, 0]))},
        "right_leg_lower": {"rotation": rotation((0.0, [22, 0, 0]), (half_walk, [6, 0, 0]), (walk_length, [22, 0, 0]))},
        "left_foot": {"rotation": rotation((0.0, [-5, 0, 0]), (half_walk, [9, 0, 0]), (walk_length, [-5, 0, 0]))},
        "right_foot": {"rotation": rotation((0.0, [9, 0, 0]), (half_walk, [-5, 0, 0]), (walk_length, [9, 0, 0]))},
        "left_arm": {"rotation": rotation((0.0, [-wa, 0, 2]), (half_walk, [wa, 0, -2]), (walk_length, [-wa, 0, 2]))},
        "right_arm": {"rotation": rotation((0.0, [wa, 0, -2]), (half_walk, [-wa, 0, 2]), (walk_length, [wa, 0, -2]))},
        "left_arm_lower": {"rotation": rotation((0.0, [7, 0, 0]), (half_walk, [15, 0, 0]), (walk_length, [7, 0, 0]))},
        "right_arm_lower": {"rotation": rotation((0.0, [15, 0, 0]), (half_walk, [7, 0, 0]), (walk_length, [15, 0, 0]))},
        "body": {"position": position((0.0, [0, 0, 0]), (half_walk, [0, wb, 0]), (walk_length, [0, 0, 0]))},
        "chest": {"rotation": rotation((0.0, [style["lean"] * 0.10, 0, 0]), (half_walk, [style["lean"] * 0.18, 0, 0]), (walk_length, [style["lean"] * 0.10, 0, 0]))},
    })

    rl, ra, run_length, lean = style["run_leg"], style["run_arm"], style["run_len"], style["lean"]
    half_run = run_length / 2
    run = animation(run_length, True, {
        "left_leg": {"rotation": rotation((0.0, [rl, 0, 0]), (half_run, [-rl, 0, 0]), (run_length, [rl, 0, 0]))},
        "right_leg": {"rotation": rotation((0.0, [-rl, 0, 0]), (half_run, [rl, 0, 0]), (run_length, [-rl, 0, 0]))},
        "left_leg_lower": {"rotation": rotation((0.0, [30, 0, 0]), (half_run, [8, 0, 0]), (run_length, [30, 0, 0]))},
        "right_leg_lower": {"rotation": rotation((0.0, [8, 0, 0]), (half_run, [30, 0, 0]), (run_length, [8, 0, 0]))},
        "left_foot": {"rotation": rotation((0.0, [-13, 0, 0]), (half_run, [14, 0, 0]), (run_length, [-13, 0, 0]))},
        "right_foot": {"rotation": rotation((0.0, [14, 0, 0]), (half_run, [-13, 0, 0]), (run_length, [14, 0, 0]))},
        "left_arm": {"rotation": rotation((0.0, [-ra, 0, 4]), (half_run, [ra, 0, -4]), (run_length, [-ra, 0, 4]))},
        "right_arm": {"rotation": rotation((0.0, [ra, 0, -4]), (half_run, [-ra, 0, 4]), (run_length, [ra, 0, -4]))},
        "left_arm_lower": {"rotation": rotation((0.0, [18, 0, 0]), (half_run, [5, 0, 0]), (run_length, [18, 0, 0]))},
        "right_arm_lower": {"rotation": rotation((0.0, [5, 0, 0]), (half_run, [18, 0, 0]), (run_length, [5, 0, 0]))},
        "body": {"position": position((0.0, [0, 0, 0]), (half_run, [0, -0.46, 0]), (run_length, [0, 0, 0]))},
        "chest": {"rotation": rotation((0.0, [lean, 0, 0]), (half_run, [lean + 2, 0, 0]), (run_length, [lean, 0, 0]))},
    })

    combat = animation(1.05, True, {
        "body": {"rotation": rotation((0.0, [4, 0, 0]), (0.52, [2, 0, 0]), (1.05, [4, 0, 0]))},
        "chest": {"rotation": rotation((0.0, [style["lean"] * 0.38, 0, 0]), (0.52, [style["lean"] * 0.25, 0, 0]), (1.05, [style["lean"] * 0.38, 0, 0]))},
        "left_arm": {"rotation": rotation((0.0, [-18 if role == "seer" else -25, 0, 8]), (0.52, [-13, 0, 5]), (1.05, [-18 if role == "seer" else -25, 0, 8]))},
        "right_arm": {"rotation": rotation((0.0, [-18 if role == "seer" else -25, 0, -8]), (0.52, [-13, 0, -5]), (1.05, [-18 if role == "seer" else -25, 0, -8]))},
        "left_leg": {"rotation": rotation((0.0, [5, 0, 0]), (1.05, [5, 0, 0]))},
        "right_leg": {"rotation": rotation((0.0, [-5, 0, 0]), (1.05, [-5, 0, 0]))},
    })

    def attack(hand: str, strength: float, lateral: float) -> dict[str, Any]:
        opposite = "left_arm" if hand == "right_arm" else "right_arm"
        return animation(0.64, False, {
            "body": {"rotation": rotation((0.0, [0, 0, 0]), (0.22, [strength * 0.25, 0, lateral]), (0.64, [0, 0, 0]))},
            hand: {"rotation": rotation((0.0, [-8, 0, 0]), (0.22, [-strength, 0, -lateral]), (0.64, [0, 0, 0]))},
            opposite: {"rotation": rotation((0.0, [0, 0, 0]), (0.22, [-22, 0, lateral * 0.55]), (0.64, [0, 0, 0]))},
        })

    attack_strength = 72 if role == "guardian" else 84 if role == "scout" else 65
    return {
        "walk": walk,
        "run": run,
        "combat_ready": combat,
        "melee_attack_1": attack("right_arm", attack_strength, -11),
        "melee_attack_2": attack("left_arm", attack_strength - 7, 11),
        "melee_attack_3": animation(0.72, False, {
            "body": {"rotation": rotation((0.0, [0, 0, 0]), (0.28, [12, 0, 12 if role in {"guardian", "scout"} else -8]), (0.72, [0, 0, 0]))},
            "right_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.28, [-(attack_strength - 4), 0, -15]), (0.72, [0, 0, 0]))},
            "left_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.28, [-28, 0, 12]), (0.72, [0, 0, 0]))},
            "left_leg": {"rotation": rotation((0.0, [0, 0, 0]), (0.28, [10, 0, 0]), (0.72, [0, 0, 0]))},
        }),
    }


def power_body_clips(role: str) -> dict[str, Any]:
    if role == "seer":
        return {
            "hive_focus": animation(1.25, True, {
                "body": {"rotation": rotation((0.0, [-3, 0, 0]), (0.36, [-8, 0, 0]), (1.25, [-6, 0, 0]))},
                "chest": {"rotation": rotation((0.0, [-2, 0, 0]), (0.36, [-7, 0, 0]), (1.25, [-5, 0, 0]))},
                "right_arm": {"rotation": rotation((0.0, [-20, 0, -4]), (0.36, [-76, 10, -8]), (1.25, [-70, 6, -5]))},
                "right_arm_lower": {"rotation": rotation((0.0, [8, 0, 0]), (0.36, [22, 0, 0]), (1.25, [18, 0, 0]))},
                "left_arm": {"rotation": rotation((0.0, [-10, 0, 3]), (0.36, [-34, -8, 6]), (1.25, [-28, -5, 4]))},
            }),
            "hive_release_suspend": animation(0.62, False, {
                "body": {"rotation": rotation((0.0, [-6, 0, 0]), (0.28, [-14, 0, 0]), (0.62, [0, 0, 0]))},
                "right_arm": {"rotation": rotation((0.0, [-70, 6, -5]), (0.28, [-108, 5, -4]), (0.62, [-8, 0, 0]))},
                "left_arm": {"rotation": rotation((0.0, [-28, -5, 4]), (0.28, [-56, -6, 4]), (0.62, [-4, 0, 0]))},
            }),
            "hive_release_redirect": animation(0.68, False, {
                "chest": {"rotation": rotation((0.0, [-5, 0, 0]), (0.30, [-10, 20, 0]), (0.68, [0, 0, 0]))},
                "right_arm": {"rotation": rotation((0.0, [-68, 6, -5]), (0.30, [-92, 30, -12]), (0.68, [-6, 0, 0]))},
                "left_arm": {"rotation": rotation((0.0, [-25, -5, 3]), (0.30, [-48, 10, 8]), (0.68, [-4, 0, 0]))},
            }),
            "hive_release_shatter": animation(0.76, False, {
                "body": {"rotation": rotation((0.0, [-5, 0, 0]), (0.34, [-17, 0, 0]), (0.76, [0, 0, 0]))},
                "right_arm": {"rotation": rotation((0.0, [-62, 4, -4]), (0.34, [-105, 0, -18]), (0.76, [-8, 0, 0]))},
                "left_arm": {"rotation": rotation((0.0, [-30, -4, 4]), (0.34, [-94, 0, 18]), (0.76, [-5, 0, 0]))},
            }),
        }
    if role == "gifted":
        return {
            "power_focus": animation(0.90, True, {
                "body": {"rotation": rotation((0.0, [-2, 0, 0]), (0.45, [-7, 0, 0]), (0.90, [-3, 0, 0]))},
                "chest": {"rotation": rotation((0.0, [-2, 0, 0]), (0.45, [-6, 0, 0]), (0.90, [-3, 0, 0]))},
                "left_arm": {"rotation": rotation((0.0, [-48, 0, 9]), (0.45, [-70, 0, 12]), (0.90, [-58, 0, 8]))},
                "right_arm": {"rotation": rotation((0.0, [-48, 0, -9]), (0.45, [-70, 0, -12]), (0.90, [-58, 0, -8]))},
                "left_arm_lower": {"rotation": rotation((0.0, [16, 0, 0]), (0.45, [25, 0, 0]), (0.90, [18, 0, 0]))},
                "right_arm_lower": {"rotation": rotation((0.0, [16, 0, 0]), (0.45, [25, 0, 0]), (0.90, [18, 0, 0]))},
            }),
            "push_release": animation(0.42, False, {
                "body": {"rotation": rotation((0.0, [-6, 0, 0]), (0.12, [-15, 0, 0]), (0.42, [0, 0, 0]))},
                "left_arm": {"rotation": rotation((0.0, [-58, 0, 8]), (0.12, [-106, 0, 10]), (0.42, [0, 0, 0]))},
                "right_arm": {"rotation": rotation((0.0, [-58, 0, -8]), (0.12, [-106, 0, -10]), (0.42, [0, 0, 0]))},
            }),
            "shield_hold": animation(1.15, True, {
                "body": {"rotation": rotation((0.0, [-5, 0, 0]), (1.15, [-5, 0, 0]))},
                "left_arm": {"rotation": rotation((0.0, [-82, 0, 28]), (0.58, [-76, 0, 25]), (1.15, [-82, 0, 28]))},
                "right_arm": {"rotation": rotation((0.0, [-82, 0, -28]), (0.58, [-76, 0, -25]), (1.15, [-82, 0, -28]))},
            }),
            "rescue_pull": animation(0.66, False, {
                "body": {"rotation": rotation((0.0, [-4, 0, 0]), (0.24, [-13, 0, 0]), (0.66, [0, 0, 0]))},
                "left_arm": {"rotation": rotation((0.0, [-54, 0, 12]), (0.24, [-96, 0, 21]), (0.66, [-4, 0, 0]))},
                "right_arm": {"rotation": rotation((0.0, [-54, 0, -12]), (0.24, [-96, 0, -21]), (0.66, [-4, 0, 0]))},
            }),
        }
    return {}


def context_body_clips(role: str) -> dict[str, Any]:
    """Role-flavored, visual-only contextual interaction gestures."""
    gentle = {"seer": 18, "guardian": 12, "gifted": 16, "scout": 24}[role]
    point = {"seer": 44, "guardian": 54, "gifted": 42, "scout": 62}[role]
    return {
        "context_animal_greet": animation(1.18, False, {
            "body": {"rotation": rotation((0.0, [0, 0, 0]), (0.34, [5, 0, 0]), (1.18, [0, 0, 0]))},
            "right_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.34, [-gentle, 0, -8]), (0.74, [-gentle + 4, 0, -5]), (1.18, [0, 0, 0]))},
            "left_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.34, [-10, 0, 5]), (1.18, [0, 0, 0]))},
        }),
        "context_animal_observe": animation(1.32, False, {
            "body": {"rotation": rotation((0.0, [0, 0, 0]), (0.38, [8, 0, 0]), (0.90, [5, 0, 0]), (1.32, [0, 0, 0]))},
            "left_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.38, [-22, 0, 8]), (0.90, [-18, 0, 5]), (1.32, [0, 0, 0]))},
            "right_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.38, [-14, 0, -6]), (1.32, [0, 0, 0]))},
        }),
        "context_field_note": animation(1.20, False, {
            "body": {"rotation": rotation((0.0, [0, 0, 0]), (0.32, [6, 0, 0]), (1.20, [0, 0, 0]))},
            "left_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.32, [-48, 0, 12]), (0.78, [-40, 0, 10]), (1.20, [0, 0, 0]))},
            "right_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.32, [-25, 0, -8]), (0.78, [-30, 0, -6]), (1.20, [0, 0, 0]))},
        }),
        "context_threat_brief": animation(0.94, False, {
            "body": {"rotation": rotation((0.0, [0, 0, 0]), (0.24, [4, 0, 0]), (0.94, [0, 0, 0]))},
            "right_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.24, [-point, 0, -12]), (0.62, [-point + 6, 0, -9]), (0.94, [0, 0, 0]))},
            "left_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.24, [-15, 0, 8]), (0.94, [0, 0, 0]))},
        }),
        "context_loot_note": animation(1.06, False, {
            "body": {"rotation": rotation((0.0, [0, 0, 0]), (0.30, [4, 0, 0]), (1.06, [0, 0, 0]))},
            "left_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.30, [-38, 0, 18]), (0.70, [-32, 0, 14]), (1.06, [0, 0, 0]))},
            "right_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.30, [-38, 0, -18]), (0.70, [-32, 0, -14]), (1.06, [0, 0, 0]))},
        }),
        "context_biome_brief": animation(1.24, False, {
            "body": {"rotation": rotation((0.0, [0, 0, 0]), (0.34, [-2, 0, 0]), (1.24, [0, 0, 0]))},
            "right_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.34, [-point + 8, 0, -8]), (0.80, [-point + 12, 0, -5]), (1.24, [0, 0, 0]))},
        }),
        "context_structure_brief": animation(1.18, False, {
            "body": {"rotation": rotation((0.0, [0, 0, 0]), (0.38, [7, 0, 0]), (1.18, [0, 0, 0]))},
            "left_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.38, [-42, 0, 12]), (0.78, [-34, 0, 10]), (1.18, [0, 0, 0]))},
            "right_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.38, [-20, 0, -8]), (1.18, [0, 0, 0]))},
        }),
        "context_rest_request": animation(1.42, False, {
            "body": {"rotation": rotation((0.0, [0, 0, 0]), (0.42, [9, 0, 0]), (0.92, [6, 0, 0]), (1.42, [0, 0, 0]))},
            "left_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.42, [-42, 0, 14]), (0.92, [-30, 0, 10]), (1.42, [0, 0, 0]))},
            "right_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.42, [-42, 0, -14]), (0.92, [-30, 0, -10]), (1.42, [0, 0, 0]))},
        }),
        "context_route_note": animation(1.02, False, {
            "body": {"rotation": rotation((0.0, [0, 0, 0]), (0.26, [3, 0, 0]), (1.02, [0, 0, 0]))},
            "right_arm": {"rotation": rotation((0.0, [0, 0, 0]), (0.26, [-point, 0, -15]), (0.68, [-point + 8, 0, -10]), (1.02, [0, 0, 0]))},
        }),
    }


def face_clips(role: str) -> dict[str, Any]:
    style = STYLE[role]
    idle_length = 4.80 if role != "guardian" else 5.20
    blink_one = idle_length * 0.29
    blink_two = idle_length * 0.74
    eye_bias = 0.14 if role == "scout" else 0.10
    def blink_frames(at: float, close: float) -> dict[str, Any]:
        return {"position": position((0.0, [0, 0, 0]), (at - 0.09, [0, 0, 0]), (at, [0, close, 0]), (at + 0.12, [0, 0, 0]), (idle_length, [0, 0, 0]))}
    face_idle = animation(idle_length, True, {
        "eye_left_pupil": {"position": position((0.0, [0, 0, 0]), (idle_length * 0.18, [eye_bias, 0.02, 0]), (idle_length * 0.55, [-eye_bias, -0.01, 0]), (idle_length, [0, 0, 0]))},
        "eye_right_pupil": {"position": position((0.0, [0, 0, 0]), (idle_length * 0.18, [eye_bias, 0.02, 0]), (idle_length * 0.55, [-eye_bias, -0.01, 0]), (idle_length, [0, 0, 0]))},
        "eye_left_upper_lid": blink_frames(blink_one, -0.72),
        "eye_right_upper_lid": blink_frames(blink_two, -0.72),
        "eye_left_lower_lid": {"position": position((0.0, [0, 0, 0]), (blink_one - 0.09, [0, 0, 0]), (blink_one, [0, 0.38, 0]), (blink_one + 0.12, [0, 0, 0]), (idle_length, [0, 0, 0]))},
        "eye_right_lower_lid": {"position": position((0.0, [0, 0, 0]), (blink_two - 0.09, [0, 0, 0]), (blink_two, [0, 0.38, 0]), (blink_two + 0.12, [0, 0, 0]), (idle_length, [0, 0, 0]))},
        "brow_left": {"rotation": rotation((0.0, [0, 0, 0]), (idle_length * 0.42, [-2, 0, 0]), (idle_length, [0, 0, 0]))},
        "brow_right": {"rotation": rotation((0.0, [0, 0, 0]), (idle_length * 0.42, [-2, 0, 0]), (idle_length, [0, 0, 0]))},
    })
    face_alert = animation(1.70, True, {
        "eye_left_pupil": {"position": position((0.0, [0.10, -0.02, 0]), (0.85, [-0.08, -0.02, 0]), (1.70, [0.10, -0.02, 0]))},
        "eye_right_pupil": {"position": position((0.0, [0.10, -0.02, 0]), (0.85, [-0.08, -0.02, 0]), (1.70, [0.10, -0.02, 0]))},
        "brow_left": {"rotation": rotation((0.0, [-5, 0, 0]), (1.70, [-5, 0, 0]))},
        "brow_right": {"rotation": rotation((0.0, [-5, 0, 0]), (1.70, [-5, 0, 0]))},
    })
    face_talk = animation(1.60, True, {
        "jaw": {"rotation": rotation((0.0, [0, 0, 0]), (0.18, [9, 0, 0]), (0.36, [0, 0, 0]), (0.62, [7, 0, 0]), (0.82, [0, 0, 0]), (1.10, [8, 0, 0]), (1.30, [0, 0, 0]), (1.60, [0, 0, 0]))},
        "mouth_neutral": {"position": position((0.0, [0, 0, 0]), (0.18, [0, -0.20, 0]), (0.36, [0, 0, 0]), (0.62, [0, -0.16, 0]), (1.60, [0, 0, 0]))},
        "eye_left_pupil": {"position": position((0.0, [0, 0, 0]), (0.60, [0.08, 0, 0]), (1.60, [0, 0, 0]))},
        "eye_right_pupil": {"position": position((0.0, [0, 0, 0]), (0.60, [0.08, 0, 0]), (1.60, [0, 0, 0]))},
        "brow_left": {"rotation": rotation((0.0, [0, 0, 0]), (0.50, [-3, 0, 0]), (1.60, [0, 0, 0]))},
        "brow_right": {"rotation": rotation((0.0, [0, 0, 0]), (0.50, [-3, 0, 0]), (1.60, [0, 0, 0]))},
    })
    face_combat = animation(1.0, True, {
        "eye_left_pupil": {"position": position((0.0, [0, -0.05, 0]), (1.0, [0, -0.05, 0]))},
        "eye_right_pupil": {"position": position((0.0, [0, -0.05, 0]), (1.0, [0, -0.05, 0]))},
        "eye_left_lower_lid": {"position": position((0.0, [0, 0.16, 0]), (1.0, [0, 0.16, 0]))},
        "eye_right_lower_lid": {"position": position((0.0, [0, 0.16, 0]), (1.0, [0, 0.16, 0]))},
        "brow_left": {"rotation": rotation((0.0, [-10, 0, 0]), (1.0, [-10, 0, 0]))},
        "brow_right": {"rotation": rotation((0.0, [-10, 0, 0]), (1.0, [-10, 0, 0]))},
    })
    power_brow = -12 if role in {"seer", "gifted"} else -7
    face_power = animation(1.20, True, {
        "eye_left_pupil": {"position": position((0.0, [0, -0.06, 0]), (0.60, [0.04, -0.08, 0]), (1.20, [0, -0.06, 0]))},
        "eye_right_pupil": {"position": position((0.0, [0, -0.06, 0]), (0.60, [0.04, -0.08, 0]), (1.20, [0, -0.06, 0]))},
        "brow_left": {"rotation": rotation((0.0, [power_brow, 0, 0]), (1.20, [power_brow, 0, 0]))},
        "brow_right": {"rotation": rotation((0.0, [power_brow, 0, 0]), (1.20, [power_brow, 0, 0]))},
    })
    face_context = animation(1.20, True, {
        "head": {"rotation": rotation((0.0, [-2, 0, 0]), (0.60, [-1, 2, 0]), (1.20, [-2, 0, 0]))},
        "eye_left_pupil": {"position": position((0.0, [0, 0, 0]), (0.60, [0.06, -0.02, 0]), (1.20, [0, 0, 0]))},
        "eye_right_pupil": {"position": position((0.0, [0, 0, 0]), (0.60, [0.06, -0.02, 0]), (1.20, [0, 0, 0]))},
        "brow_left": {"rotation": rotation((0.0, [-2, 0, 0]), (1.20, [-2, 0, 0]))},
        "brow_right": {"rotation": rotation((0.0, [-2, 0, 0]), (1.20, [-2, 0, 0]))},
    })
    face_recovery = animation(2.10, True, {
        "head": {"rotation": rotation((0.0, [9, 0, 0]), (2.10, [9, 0, 0]))},
        "eye_left_upper_lid": {"position": position((0.0, [0, -0.24, 0]), (1.05, [0, -0.34, 0]), (2.10, [0, -0.24, 0]))},
        "eye_right_upper_lid": {"position": position((0.0, [0, -0.24, 0]), (1.05, [0, -0.34, 0]), (2.10, [0, -0.24, 0]))},
        "eye_left_lower_lid": {"position": position((0.0, [0, 0.18, 0]), (2.10, [0, 0.18, 0]))},
        "eye_right_lower_lid": {"position": position((0.0, [0, 0.18, 0]), (2.10, [0, 0.18, 0]))},
        "brow_left": {"rotation": rotation((0.0, [4, 0, 0]), (2.10, [4, 0, 0]))},
        "brow_right": {"rotation": rotation((0.0, [4, 0, 0]), (2.10, [4, 0, 0]))},
    })
    glance_yaw = {"seer": 25, "guardian": 20, "gifted": 23, "scout": 29}[role]
    def glance(name: str, direction: int) -> dict[str, Any]:
        # Eye lead -> smooth head turn -> short hold -> head return -> eye return.
        return animation(0.95, False, {
            "head": {"rotation": rotation((0.0, [0, 0, 0]), (0.10, [0, 0, 0]),
                                            (0.30, [-1, direction * glance_yaw, direction * 2]),
                                            (0.52, [-1, direction * glance_yaw, direction * 2]),
                                            (0.78, [0, direction * 5, 0]), (0.95, [0, 0, 0]))},
            "eye_left_pupil": {"position": position((0.0, [0, 0, 0]), (0.08, [direction * 0.18, 0, 0]),
                                                       (0.58, [direction * 0.18, 0, 0]), (0.82, [direction * 0.06, 0, 0]),
                                                       (0.95, [0, 0, 0]))},
            "eye_right_pupil": {"position": position((0.0, [0, 0, 0]), (0.08, [direction * 0.18, 0, 0]),
                                                        (0.58, [direction * 0.18, 0, 0]), (0.82, [direction * 0.06, 0, 0]),
                                                        (0.95, [0, 0, 0]))},
            "brow_left": {"rotation": rotation((0.0, [0, 0, 0]), (0.30, [-3, 0, 0]), (0.78, [-1, 0, 0]), (0.95, [0, 0, 0]))},
            "brow_right": {"rotation": rotation((0.0, [0, 0, 0]), (0.30, [-3, 0, 0]), (0.78, [-1, 0, 0]), (0.95, [0, 0, 0]))},
        })
    def rear_check(name: str, direction: int) -> dict[str, Any]:
        # A deliberate over-shoulder inspection, not a static 180-degree stare.
        return animation(1.35, False, {
            "head": {"rotation": rotation((0.0, [0, 0, 0]), (0.10, [0, 0, 0]),
                                            (0.38, [-3, direction * 55, direction * 3]),
                                            (0.72, [-3, direction * 55, direction * 3]),
                                            (1.08, [0, direction * 8, 0]), (1.35, [0, 0, 0]))},
            "eye_left_pupil": {"position": position((0.0, [0, 0, 0]), (0.08, [direction * 0.20, 0, 0]),
                                                       (0.76, [direction * 0.20, 0, 0]), (1.12, [direction * 0.05, 0, 0]),
                                                       (1.35, [0, 0, 0]))},
            "eye_right_pupil": {"position": position((0.0, [0, 0, 0]), (0.08, [direction * 0.20, 0, 0]),
                                                        (0.76, [direction * 0.20, 0, 0]), (1.12, [direction * 0.05, 0, 0]),
                                                        (1.35, [0, 0, 0]))},
            "brow_left": {"rotation": rotation((0.0, [0, 0, 0]), (0.38, [-6, 0, 0]), (0.98, [-2, 0, 0]), (1.35, [0, 0, 0]))},
            "brow_right": {"rotation": rotation((0.0, [0, 0, 0]), (0.38, [-6, 0, 0]), (0.98, [-2, 0, 0]), (1.35, [0, 0, 0]))},
        })
    # Head orientation belongs exclusively to this controller, never the body walk/run clips.
    face_idle["bones"]["head"] = {"rotation": rotation((0.0, [0, 0, 0]), (idle_length, [0, 0, 0]))}
    # Alert expression is eyes/brows only; meaningful lateral/rear attention is
    # emitted by the finite gait gesture controller, never an idle head scan.
    face_alert["bones"]["head"] = {"rotation": rotation((0.0, [-1, 0, 0]), (1.70, [-1, 0, 0]))}
    face_talk["bones"]["head"] = {"rotation": rotation((0.0, [0, -4, 0]), (0.55, [2, 5, 0]), (1.10, [-1, -3, 0]), (1.60, [0, -4, 0]))}
    face_combat["bones"]["head"] = {"rotation": rotation((0.0, [-3, 0, 0]), (1.0, [-3, 0, 0]))}
    face_power["bones"]["head"] = {"rotation": rotation((0.0, [-7 if role in {"seer", "gifted"} else -3, 0, 0]), (1.20, [-7 if role in {"seer", "gifted"} else -3, 0, 0]))}
    return {"face_idle": face_idle, "face_alert": face_alert, "face_talk": face_talk,
            "face_combat": face_combat, "face_power": face_power, "face_context": face_context, "face_recovery": face_recovery,
            "face_glance_left": glance("left", -1), "face_glance_right": glance("right", 1),
            "face_check_back_left": rear_check("back_left", -1), "face_check_back_right": rear_check("back_right", 1)}


def secondary_clip(role: str, name: str, length: float, amplitude: float, cloth_amplitude: float) -> dict[str, Any]:
    bones: dict[str, Any] = {}
    for index, hair in enumerate(HAIR[role]):
        sign = -1 if index % 2 else 1
        factor = 0.64 + 0.16 * (index % 3)
        amount = round(amplitude * factor, 2)
        bones[hair] = {"rotation": rotation((0.0, [0, 0, sign * amount]),
                                              (length * 0.50, [0, 0, -sign * amount]),
                                              (length, [0, 0, sign * amount]))}
    for index, cloth in enumerate(CLOTH[role]):
        sign = -1 if index % 2 else 1
        amount = round(cloth_amplitude * (0.70 + index * 0.12), 2)
        bones[cloth] = {"rotation": rotation((0.0, [amount, 0, sign * amount]),
                                               (length * 0.50, [-amount, 0, -sign * amount]),
                                               (length, [amount, 0, sign * amount]))}
    return animation(length, True, bones)


def secondary_clips(role: str) -> dict[str, Any]:
    style = STYLE[role]
    return {
        "secondary_idle": secondary_clip(role, "idle", 3.60, style["hair"] * 0.42, 0.65),
        "secondary_walk": secondary_clip(role, "walk", style["walk_len"], style["hair"], 1.10),
        "secondary_run": secondary_clip(role, "run", style["run_len"], style["hair"] * 1.58, 1.80),
        "secondary_combat": secondary_clip(role, "combat", 1.05, style["hair"] * 0.72, 0.85),
        "secondary_power": secondary_clip(role, "power", 1.20, style["hair"] * 0.86, 0.70),
        "secondary_context": secondary_clip(role, "context", 1.20, style["hair"] * 0.48, 0.56),
        "secondary_recovery": secondary_clip(role, "recovery", 2.10, style["hair"] * 0.30, 0.35),
    }


def patch_role(role: str) -> None:
    path = ANIM / f"{role}.animation.json"
    payload = json.loads(path.read_text(encoding="utf-8"))
    animations: dict[str, Any] = payload["animations"]
    strip_layered_bones(role, animations)

    for name, data in body_clips(role).items():
        animations[key(role, name)] = data
    for name, data in power_body_clips(role).items():
        animations[key(role, name)] = data
    for name, data in context_body_clips(role).items():
        animations[key(role, name)] = data
    for name, data in face_clips(role).items():
        animations[key(role, name)] = data
    for name, data in secondary_clips(role).items():
        animations[key(role, name)] = data

    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    for role in ROLES:
        patch_role(role)
    print("Built layered body, face, and secondary-motion clips for all four companions.")


if __name__ == "__main__":
    main()
