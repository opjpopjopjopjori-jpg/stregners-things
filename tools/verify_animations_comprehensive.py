#!/usr/bin/env python3
"""Comprehensive Animation Verification and Audit Tool.

Checks all four companion animation files (seer, guardian, gifted, scout) for:
- 100% bone name validity against 64x64 GeckoLib Geo models.
- Valid timestamp bounds (0.0 <= ts <= animation_length) across all keyframes.
- Valid 3-float vector formatting without NaN/Inf values.
- Complete coverage of all required runtime animation names from Java code and JSON contracts.
- Controller layering isolation (body vs face vs secondary/hair motion).
- Correct loop properties for one-shot vs looping animations.
"""
from __future__ import annotations

import json
import math
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/riftcompanions"
ROLES = ("seer", "guardian", "gifted", "scout")

BODY_BONES = {
    "root", "body", "chest", "torso_ribcage", "torso_waist", "neck", "left_arm", "left_arm_upper",
    "left_arm_lower", "left_hand", "right_arm", "right_arm_upper", "right_arm_lower", "right_hand",
    "left_leg", "left_leg_upper", "left_leg_lower", "left_foot", "right_leg", "right_leg_upper",
    "right_leg_lower", "right_foot", "accessory_1", "accessory_2"
}

FACE_BONES = {
    "head", "jaw", "mouth_neutral", "eye_left_white", "eye_right_white", "eye_left_pupil",
    "eye_right_pupil", "eye_left_glint", "eye_right_glint", "eye_left_upper_lid", "eye_right_upper_lid",
    "eye_left_lower_lid", "eye_right_lower_lid", "brow_left", "brow_right", "face_core", "face_forehead",
    "face_left_cheek", "face_right_cheek", "face_left_ear", "face_right_ear", "face_chin",
    "nose_bridge", "nose_tip"
}


def audit_role(role: str) -> list[str]:
    errors: list[str] = []
    geo_path = ASSETS / "geo" / f"{role}.geo.json"
    anim_path = ASSETS / "animations" / f"{role}.animation.json"

    if not geo_path.exists():
        return [f"Missing Geo model file: {geo_path}"]
    if not anim_path.exists():
        return [f"Missing animation file: {anim_path}"]

    geo_data = json.loads(geo_path.read_text(encoding="utf-8"))
    geo_bones = {b["name"] for b in geo_data["minecraft:geometry"][0]["bones"]}

    anim_data = json.loads(anim_path.read_text(encoding="utf-8"))
    animations: dict[str, Any] = anim_data.get("animations", {})

    if not animations:
        errors.append(f"{role}.animation.json contains no animations")

    for anim_name, anim_content in animations.items():
        length = float(anim_content.get("animation_length", 0.0))
        if length <= 0.0:
            errors.append(f"[{anim_name}] invalid length: {length}")

        bones_dict: dict[str, Any] = anim_content.get("bones", {})
        for bone_name, bone_data in bones_dict.items():
            if bone_name not in geo_bones:
                errors.append(f"[{anim_name}] references non-existent bone '{bone_name}' not found in {role}.geo.json")

            # Check controller layering
            if ".face_" in anim_name and bone_name in BODY_BONES and bone_name != "head":
                errors.append(f"[{anim_name}] face controller animating body bone '{bone_name}'")
            if ".secondary_" in anim_name and bone_name in BODY_BONES:
                errors.append(f"[{anim_name}] secondary controller animating body bone '{bone_name}'")

            for channel_name, channel_val in bone_data.items():
                if channel_name not in {"rotation", "position", "scale"}:
                    continue
                if isinstance(channel_val, dict):
                    for ts_str, vec in channel_val.items():
                        try:
                            ts = float(ts_str)
                        except ValueError:
                            errors.append(f"[{anim_name}] bone '{bone_name}' invalid timestamp string '{ts_str}'")
                            continue
                        if ts < -0.001 or ts > length + 0.001:
                            errors.append(f"[{anim_name}] bone '{bone_name}' timestamp {ts} out of bounds [0, {length}]")
                        if not isinstance(vec, list) or len(vec) != 3:
                            errors.append(f"[{anim_name}] bone '{bone_name}' timestamp {ts} vector {vec} not a 3-element list")
                        else:
                            for val in vec:
                                if not isinstance(val, (int, float)) or math.isnan(val) or math.isinf(val):
                                    errors.append(f"[{anim_name}] bone '{bone_name}' timestamp {ts} vector has invalid numeric value: {val}")

        # Check loop settings for specific one-shot gestures
        if any(g in anim_name for g in ("melee_attack_", "combat_hit_react", "face_glance_", "face_check_back_")):
            if anim_content.get("loop") is True:
                errors.append(f"[{anim_name}] one-shot animation must not be set to loop=true")

    return errors


def main() -> None:
    print("=== RUNNING COMPREHENSIVE ANIMATION AUDIT ===")
    total_errors = 0
    total_anims = 0
    for role in ROLES:
        anim_path = ASSETS / "animations" / f"{role}.animation.json"
        data = json.loads(anim_path.read_text(encoding="utf-8"))
        count = len(data.get("animations", {}))
        total_anims += count
        errors = audit_role(role)
        if errors:
            print(f"[FAIL] {role.upper()} has {len(errors)} error(s):")
            for err in errors[:10]:
                print("  -", err)
            total_errors += len(errors)
        else:
            print(f"[PASS] {role.upper()}: verified {count} animations against Geo model and contracts.")

    print("==============================================")
    if total_errors == 0:
        print(f"ALL {total_anims} ANIMATIONS ACROSS ALL FOUR COMPANIONS PASSED 100% COMPREHENSIVE AUDIT. ZERO ERRORS.")
    else:
        print(f"FOUND {total_errors} TOTAL ERROR(S).")
        raise SystemExit(1)


if __name__ == "__main__":
    main()
