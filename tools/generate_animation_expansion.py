#!/usr/bin/env python3
"""Expand companion GeckoLib animation resources with the shared rig v2 clip set.

Gameplay is intentionally absent from this generator. Marker timing and server
safety ownership are declared in animation_contracts and Java source, not in a
client animation asset.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANIM = ROOT / "src/main/resources/assets/riftcompanions/animations"
ROLES = ("guardian", "seer", "gifted", "scout")


def animation(length: float, loop: bool, bones: dict) -> dict:
    return {"animation_length": length, "loop": loop, "bones": bones}


def rot(frames: dict[str, list[float]]) -> dict:
    return {"rotation": frames}


def pos(frames: dict[str, list[float]]) -> dict:
    return {"position": frames}


def common(prefix: str, guardian: bool = False) -> dict:
    chest = "chest"
    left = "left_arm_upper"
    right = "right_arm_upper"
    left_leg = "left_leg_upper"
    right_leg = "right_leg_upper"
    attack_weight = 1.15 if guardian else 1.0
    return {
        f"animation.{prefix}.idle_observe": animation(2.6, True, {
            chest: rot({"0.0": [0, 0, 0], "1.3": [1.2, 0, 0], "2.6": [0, 0, 0]}),
            "head": rot({"0.0": [-2, -12, 0], "1.1": [1, 14, 0], "2.6": [-2, -12, 0]})
        }),
        f"animation.{prefix}.idle_alert": animation(0.45, True, {
            chest: rot({"0.0": [-4, 0, 0], "0.22": [-6, 0, 0], "0.45": [-4, 0, 0]}),
            "head": rot({"0.0": [-8, 0, 0], "0.22": [-11, 3, 0], "0.45": [-8, 0, 0]})
        }),
        f"animation.{prefix}.walk_crouched": animation(0.82, True, {
            left_leg: rot({"0.0": [20, 0, 0], "0.41": [-20, 0, 0], "0.82": [20, 0, 0]}),
            right_leg: rot({"0.0": [-20, 0, 0], "0.41": [20, 0, 0], "0.82": [-20, 0, 0]}),
            chest: pos({"0.0": [0, -1.0, 0], "0.41": [0, -1.2, 0], "0.82": [0, -1.0, 0]}),
            "head": rot({"0.0": [7, 0, 0], "0.82": [7, 0, 0]})
        }),
        f"animation.{prefix}.run": animation(0.48, True, {
            left_leg: rot({"0.0": [43, 0, 0], "0.24": [-43, 0, 0], "0.48": [43, 0, 0]}),
            right_leg: rot({"0.0": [-43, 0, 0], "0.24": [43, 0, 0], "0.48": [-43, 0, 0]}),
            left: rot({"0.0": [-35, 0, 4], "0.24": [35, 0, -4], "0.48": [-35, 0, 4]}),
            right: rot({"0.0": [35, 0, -4], "0.24": [-35, 0, 4], "0.48": [35, 0, -4]}),
            chest: pos({"0.0": [0, 0, 0], "0.24": [0, -0.55, 0], "0.48": [0, 0, 0]})
        }),
        f"animation.{prefix}.turn_left": animation(0.24, False, {
            "root": rot({"0.0": [0, 0, 0], "0.12": [0, -14, 0], "0.24": [0, 0, 0]}),
            chest: rot({"0.0": [0, 0, 0], "0.12": [0, -9, 0], "0.24": [0, 0, 0]})
        }),
        f"animation.{prefix}.turn_right": animation(0.24, False, {
            "root": rot({"0.0": [0, 0, 0], "0.12": [0, 14, 0], "0.24": [0, 0, 0]}),
            chest: rot({"0.0": [0, 0, 0], "0.12": [0, 9, 0], "0.24": [0, 0, 0]})
        }),
        f"animation.{prefix}.melee_attack_1": animation(0.82, False, {
            chest: rot({"0.0": [0, 0, 0], "0.18": [8 * attack_weight, -10, 0], "0.42": [-10 * attack_weight, 14, 0], "0.82": [0, 0, 0]}),
            right: rot({"0.0": [0, 0, 0], "0.18": [22, -28, -12], "0.42": [-72 * attack_weight, 35, -4], "0.82": [0, 0, 0]}),
            left: rot({"0.0": [0, 0, 0], "0.18": [-8, 8, 8], "0.42": [14, -10, 4], "0.82": [0, 0, 0]})
        }),
        f"animation.{prefix}.melee_attack_2": animation(0.88, False, {
            chest: rot({"0.0": [0, 0, 0], "0.18": [7 * attack_weight, 13, 0], "0.46": [-12 * attack_weight, -18, 0], "0.88": [0, 0, 0]}),
            left: rot({"0.0": [0, 0, 0], "0.18": [20, 30, 12], "0.46": [-76 * attack_weight, -38, 4], "0.88": [0, 0, 0]}),
            right: rot({"0.0": [0, 0, 0], "0.18": [-8, -8, -8], "0.46": [14, 10, -4], "0.88": [0, 0, 0]})
        }),
        f"animation.{prefix}.melee_attack_3": animation(1.10, False, {
            chest: rot({"0.0": [0, 0, 0], "0.28": [14 * attack_weight, 0, 0], "0.58": [-18 * attack_weight, 0, 0], "1.10": [0, 0, 0]}),
            left: rot({"0.0": [0, 0, 0], "0.28": [24, 0, 18], "0.58": [-92 * attack_weight, 0, 8], "1.10": [0, 0, 0]}),
            right: rot({"0.0": [0, 0, 0], "0.28": [24, 0, -18], "0.58": [-92 * attack_weight, 0, -8], "1.10": [0, 0, 0]})
        }),
        f"animation.{prefix}.melee_block": animation(0.72, False, {
            chest: rot({"0.0": [0, 0, 0], "0.18": [-8, 0, 0], "0.48": [-8, 0, 0], "0.72": [0, 0, 0]}),
            left: rot({"0.0": [0, 0, 0], "0.18": [-76, 0, 26], "0.48": [-72, 0, 22], "0.72": [0, 0, 0]}),
            right: rot({"0.0": [0, 0, 0], "0.18": [-76, 0, -26], "0.48": [-72, 0, -22], "0.72": [0, 0, 0]})
        }),
        f"animation.{prefix}.combat_hit_react": animation(0.34, False, {
            chest: rot({"0.0": [0, 0, 0], "0.08": [9, 0, 0], "0.34": [0, 0, 0]}),
            "head": rot({"0.0": [0, 0, 0], "0.08": [12, 0, 0], "0.34": [0, 0, 0]})
        }),
        f"animation.{prefix}.combat_dodge_left": animation(0.32, False, {
            "root": pos({"0.0": [0, 0, 0], "0.16": [-1.2, 0, 0], "0.32": [0, 0, 0]}),
            chest: rot({"0.0": [0, 0, 0], "0.16": [0, 0, -10], "0.32": [0, 0, 0]})
        }),
        f"animation.{prefix}.combat_dodge_right": animation(0.32, False, {
            "root": pos({"0.0": [0, 0, 0], "0.16": [1.2, 0, 0], "0.32": [0, 0, 0]}),
            chest: rot({"0.0": [0, 0, 0], "0.16": [0, 0, 10], "0.32": [0, 0, 0]})
        }),
        f"animation.{prefix}.combat_dodge_back": animation(0.32, False, {
            "root": pos({"0.0": [0, 0, 0], "0.16": [0, 0, 1.0], "0.32": [0, 0, 0]}),
            chest: rot({"0.0": [0, 0, 0], "0.16": [8, 0, 0], "0.32": [0, 0, 0]})
        }),
        f"animation.{prefix}.interact_talk": animation(1.6, True, {
            "head": rot({"0.0": [0, -4, 0], "0.55": [2, 5, 0], "1.1": [-1, -3, 0], "1.6": [0, -4, 0]}),
            "jaw": rot({"0.0": [0, 0, 0], "0.22": [9, 0, 0], "0.42": [0, 0, 0], "0.74": [7, 0, 0], "1.0": [0, 0, 0], "1.6": [0, 0, 0]})
        }),
        f"animation.{prefix}.interact_point": animation(0.72, False, {
            right: rot({"0.0": [0, 0, 0], "0.22": [-88, 0, -12], "0.48": [-82, 0, -10], "0.72": [0, 0, 0]}),
            "head": rot({"0.0": [0, 0, 0], "0.22": [-3, 13, 0], "0.72": [0, 2, 0]})
        }),
        f"animation.{prefix}.interact_pickup": animation(0.58, False, {
            chest: rot({"0.0": [0, 0, 0], "0.18": [28, 0, 0], "0.34": [22, 0, 0], "0.58": [0, 0, 0]}),
            right: rot({"0.0": [0, 0, 0], "0.18": [42, 0, -8], "0.34": [-32, 0, -6], "0.58": [0, 0, 0]})
        }),
        f"animation.{prefix}.interact_use": animation(1.15, True, {
            chest: rot({"0.0": [0, 0, 0], "0.58": [-3, 0, 0], "1.15": [0, 0, 0]}),
            "head": rot({"0.0": [-3, 0, 0], "1.15": [-3, 0, 0]}),
            right: rot({"0.0": [-28, 0, -8], "1.15": [-28, 0, -8]})
        }),
        f"animation.{prefix}.stuck_struggle": animation(0.72, True, {
            chest: rot({"0.0": [0, 0, 0], "0.18": [4, -6, 0], "0.36": [-3, 6, 0], "0.72": [0, 0, 0]}),
            left: rot({"0.0": [-18, 0, 8], "0.36": [-34, 0, 14], "0.72": [-18, 0, 8]}),
            right: rot({"0.0": [-18, 0, -8], "0.36": [-34, 0, -14], "0.72": [-18, 0, -8]})
        }),
        f"animation.{prefix}.spawn_appear": animation(0.68, False, {
            chest: pos({"0.0": [0, -0.8, 0], "0.20": [0, -0.2, 0], "0.68": [0, 0, 0]}),
            "head": rot({"0.0": [12, 0, 0], "0.68": [0, 0, 0]})
        }),
        f"animation.{prefix}.dismiss_fade": animation(0.52, False, {
            chest: pos({"0.0": [0, 0, 0], "0.52": [0, -0.6, 0]}),
            "head": rot({"0.0": [0, 0, 0], "0.52": [9, 0, 0]})
        }),
        f"animation.{prefix}.emotion_fear": animation(0.56, False, {
            chest: rot({"0.0": [0, 0, 0], "0.16": [7, 0, 0], "0.56": [0, 0, 0]}),
            "head": rot({"0.0": [0, 0, 0], "0.16": [11, 0, 0], "0.56": [0, 0, 0]})
        }),
        f"animation.{prefix}.emotion_relief": animation(0.72, False, {
            chest: rot({"0.0": [-4, 0, 0], "0.36": [2, 0, 0], "0.72": [0, 0, 0]}),
            "head": rot({"0.0": [-4, 0, 0], "0.72": [0, 0, 0]})
        }),
        f"animation.{prefix}.emotion_determined": animation(0.62, False, {
            chest: rot({"0.0": [0, 0, 0], "0.20": [-7, 0, 0], "0.62": [-4, 0, 0]}),
            "head": rot({"0.0": [0, 0, 0], "0.20": [-8, 0, 0], "0.62": [-4, 0, 0]})
        }),
        f"animation.{prefix}.emotion_exhausted": animation(1.20, True, {
            chest: rot({"0.0": [8, 0, 0], "0.60": [5, 0, 0], "1.20": [8, 0, 0]}),
            "head": rot({"0.0": [14, 0, 0], "0.60": [10, 0, 0], "1.20": [14, 0, 0]}),
            left: rot({"0.0": [18, 0, 10], "1.20": [18, 0, 10]}),
            right: rot({"0.0": [18, 0, -10], "1.20": [18, 0, -10]})
        }),
        f"animation.{prefix}.calm_look_around": animation(2.8, True, {
            "head": rot({"0.0": [0, -18, 0], "1.0": [2, 16, 0], "2.0": [-1, 5, 0], "2.8": [0, -18, 0]}),
            chest: rot({"0.0": [0, -2, 0], "1.0": [0, 3, 0], "2.8": [0, -2, 0]})
        }),
        f"animation.{prefix}.calm_sit_rest": animation(2.0, True, {
            left_leg: rot({"0.0": [72, 0, 0], "1.0": [68, 0, 0], "2.0": [72, 0, 0]}),
            right_leg: rot({"0.0": [72, 0, 0], "1.0": [68, 0, 0], "2.0": [72, 0, 0]}),
            chest: pos({"0.0": [0, -2.0, 0], "2.0": [0, -2.0, 0]})
        })
    }


def role_specific(role: str) -> dict:
    if role == "guardian":
        return {
            "animation.guardian.guard_stance": animation(1.4, True, {
                "chest": rot({"0.0": [5, 0, 0], "0.7": [7, 0, 0], "1.4": [5, 0, 0]}),
                "head": rot({"0.0": [0, -7, 0], "0.7": [0, 7, 0], "1.4": [0, -7, 0]}),
                "left_arm_upper": rot({"0.0": [-32, 0, 15], "0.7": [-28, 0, 12], "1.4": [-32, 0, 15]}),
                "right_arm_upper": rot({"0.0": [-32, 0, -15], "0.7": [-28, 0, -12], "1.4": [-32, 0, -15]})
            }),
            "animation.guardian.protect": animation(0.62, False, {
                "chest": rot({"0.0": [0, 0, 0], "0.24": [-15, 0, 0], "0.62": [0, 0, 0]}),
                "left_arm_upper": rot({"0.0": [0, 0, 0], "0.24": [-75, 0, 22], "0.62": [-12, 0, 2]}),
                "right_arm_upper": rot({"0.0": [0, 0, 0], "0.24": [-75, 0, -22], "0.62": [-12, 0, -2]})
            })
        }
    if role == "seer":
        return {
            "animation.seer.hive_resist": animation(0.68, False, {
                "chest": rot({"0.0": [0, 0, 0], "0.18": [7, -5, 0], "0.44": [5, 4, 0], "0.68": [0, 0, 0]}),
                "left_arm_upper": rot({"0.0": [0, 0, 0], "0.18": [16, 0, 14], "0.68": [0, 0, 0]}),
                "right_arm_upper": rot({"0.0": [0, 0, 0], "0.18": [16, 0, -14], "0.68": [0, 0, 0]})
            }),
            "animation.seer.hive_release_suspend": animation(0.62, False, {
                "right_arm_upper": rot({"0.0": [-72, 7, -5], "0.18": [-106, 5, -4], "0.62": [-8, 0, 0]}),
                "left_arm_upper": rot({"0.0": [-30, -5, 3], "0.18": [-55, -6, 4], "0.62": [-4, 0, 0]}),
                "chest": rot({"0.0": [-6, 0, 0], "0.18": [-13, 0, 0], "0.62": [0, 0, 0]})
            }),
            "animation.seer.hive_release_redirect": animation(0.68, False, {
                "right_arm_upper": rot({"0.0": [-72, 7, -5], "0.24": [-76, 42, -8], "0.68": [-8, 0, 0]}),
                "chest": rot({"0.0": [-6, 0, 0], "0.24": [-8, 14, 0], "0.68": [0, 0, 0]})
            }),
            "animation.seer.hive_release_shatter": animation(0.76, False, {
                "left_arm_upper": rot({"0.0": [-30, -5, 3], "0.28": [-90, -10, 14], "0.76": [-6, 0, 0]}),
                "right_arm_upper": rot({"0.0": [-72, 7, -5], "0.28": [-100, 12, -16], "0.76": [-6, 0, 0]}),
                "chest": rot({"0.0": [-6, 0, 0], "0.28": [-17, 0, 0], "0.76": [0, 0, 0]})
            }),
            "animation.seer.hive_recovery": animation(1.20, False, {
                "chest": rot({"0.0": [-8, 0, 0], "0.36": [11, 0, 0], "1.20": [0, 0, 0]}),
                "head": rot({"0.0": [-10, 0, 0], "0.36": [12, 0, 0], "1.20": [0, 0, 0]}),
                "right_arm_upper": rot({"0.0": [-20, 0, 0], "0.36": [16, 0, -8], "1.20": [0, 0, 0]})
            })
        }
    if role == "gifted":
        return {
            "animation.gifted.power_notice": animation(0.48, False, {
                "head": rot({"0.0": [0, 0, 0], "0.18": [-10, 0, 0], "0.48": [-5, 0, 0]}),
                "right_arm_upper": rot({"0.0": [0, 0, 0], "0.18": [-28, 0, -6], "0.48": [-42, 0, -8]})
            }),
            "animation.gifted.power_focus": animation(0.90, True, {
                "chest": rot({"0.0": [-3, 0, 0], "0.45": [-7, 0, 0], "0.90": [-3, 0, 0]}),
                "left_arm_upper": rot({"0.0": [-58, 0, 8], "0.45": [-69, 0, 10], "0.90": [-58, 0, 8]}),
                "right_arm_upper": rot({"0.0": [-58, 0, -8], "0.45": [-69, 0, -10], "0.90": [-58, 0, -8]})
            }),
            "animation.gifted.push_release": animation(0.42, False, {
                "left_arm_upper": rot({"0.0": [-69, 0, 10], "0.12": [-102, 0, 8], "0.42": [0, 0, 0]}),
                "right_arm_upper": rot({"0.0": [-69, 0, -10], "0.12": [-102, 0, -8], "0.42": [0, 0, 0]}),
                "chest": rot({"0.0": [-7, 0, 0], "0.12": [-14, 0, 0], "0.42": [0, 0, 0]})
            }),
            "animation.gifted.shield_hold": animation(1.15, True, {
                "left_arm_upper": rot({"0.0": [-84, 0, 28], "0.58": [-76, 0, 25], "1.15": [-84, 0, 28]}),
                "right_arm_upper": rot({"0.0": [-84, 0, -28], "0.58": [-76, 0, -25], "1.15": [-84, 0, -28]})
            }),
            "animation.gifted.rescue_pull": animation(0.66, False, {
                "chest": rot({"0.0": [-5, 0, 0], "0.24": [-12, 0, 0], "0.66": [0, 0, 0]}),
                "left_arm_upper": rot({"0.0": [-58, 0, 12], "0.24": [-95, 0, 20], "0.66": [-6, 0, 0]}),
                "right_arm_upper": rot({"0.0": [-58, 0, -12], "0.24": [-95, 0, -20], "0.66": [-6, 0, 0]})
            }),
            "animation.gifted.exhausted_recovery": animation(1.35, False, {
                "chest": rot({"0.0": [12, 0, 0], "0.55": [7, 0, 0], "1.35": [0, 0, 0]}),
                "head": rot({"0.0": [17, 0, 0], "0.55": [8, 0, 0], "1.35": [0, 0, 0]})
            })
        }
    return {
        "animation.scout.ranged_draw": animation(0.36, False, {
            "left_arm_upper": rot({"0.0": [-18, 0, 12], "0.18": [-62, 0, 22], "0.36": [-62, 0, 22]}),
            "right_arm_upper": rot({"0.0": [-18, 0, -12], "0.18": [-28, 0, -16], "0.36": [-28, 0, -16]})
        }),
        "animation.scout.ranged_aim": animation(0.80, True, {
            "chest": rot({"0.0": [4, 0, 0], "0.40": [2, 0, 0], "0.80": [4, 0, 0]}),
            "left_arm_upper": rot({"0.0": [-61, 0, 18], "0.40": [-66, 0, 21], "0.80": [-61, 0, 18]}),
            "right_arm_upper": rot({"0.0": [-20, 0, -14], "0.40": [-28, 0, -16], "0.80": [-20, 0, -14]})
        }),
        "animation.scout.ranged_release": animation(0.34, False, {
            "right_arm_upper": rot({"0.0": [-28, 0, -16], "0.11": [8, 0, -4], "0.34": [0, 0, 0]}),
            "left_arm_upper": rot({"0.0": [-66, 0, 21], "0.34": [0, 0, 0]})
        }),
        "animation.scout.route_point": animation(0.76, False, {
            "right_arm_upper": rot({"0.0": [0, 0, 0], "0.22": [-92, 0, -14], "0.52": [-85, 0, -12], "0.76": [0, 0, 0]}),
            "head": rot({"0.0": [0, 0, 0], "0.22": [-3, 14, 0], "0.76": [0, 4, 0]})
        }),
        "animation.scout.lookout": animation(1.70, True, {
            "chest": rot({"0.0": [0, -4, 0], "0.85": [0, -3, 0], "1.70": [0, -4, 0]}),
            "head": rot({"0.0": [-8, -25, 0], "0.85": [-5, 28, 0], "1.70": [-8, -25, 0]})
        }),
        "animation.scout.mind_anchor_call": animation(0.68, False, {
            "left_arm_upper": rot({"0.0": [0, 0, 0], "0.18": [-75, 0, 15], "0.46": [-78, 0, 15], "0.68": [0, 0, 0]}),
            "right_arm_upper": rot({"0.0": [0, 0, 0], "0.18": [-45, 0, -6], "0.46": [-53, 0, -8], "0.68": [0, 0, 0]})
        }),
        "animation.scout.mind_anchor_hold": animation(0.92, True, {
            "left_arm_upper": rot({"0.0": [-62, 0, 14], "0.46": [-68, 0, 16], "0.92": [-62, 0, 14]}),
            "right_arm_upper": rot({"0.0": [-48, 0, -10], "0.46": [-54, 0, -12], "0.92": [-48, 0, -10]})
        }),
        "animation.scout.anchor_call": animation(0.68, False, {
            "left_arm_upper": rot({"0.0": [0, 0, 0], "0.18": [-75, 0, 15], "0.46": [-78, 0, 15], "0.68": [0, 0, 0]}),
            "right_arm_upper": rot({"0.0": [0, 0, 0], "0.18": [-45, 0, -6], "0.46": [-53, 0, -8], "0.68": [0, 0, 0]})
        })
    }


def expand(role: str) -> None:
    path = ANIM / f"{role}.animation.json"
    payload = json.loads(path.read_text(encoding="utf-8"))
    clips = payload.setdefault("animations", {})
    clips.update(common(role, guardian=role == "guardian"))
    clips.update(role_specific(role))
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    for role in ROLES:
        expand(role)
    print("Expanded companion GeckoLib animation clip set")


if __name__ == "__main__":
    main()
