#!/usr/bin/env python3
"""Legacy migration guard for pre-modular companion Geo JSON.

Current 512px faceted assets already declare shared_humanoid_v2 and this tool
leaves them unchanged. It is retained only to migrate an older generated asset
into the animation-safe backbone; it never alters hitboxes or gameplay behavior.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GEO = ROOT / "src/main/resources/assets/riftcompanions/geo"
ROLES = ("guardian", "seer", "gifted", "scout")


def clone(value):
    return json.loads(json.dumps(value))


def bone(name: str, parent: str | None, pivot: list[float], cubes: list[dict] | None = None) -> dict:
    result: dict = {"name": name, "pivot": pivot}
    if parent:
        result["parent"] = parent
    if cubes:
        result["cubes"] = cubes
    return result


def migrate(role: str) -> None:
    path = GEO / f"{role}.geo.json"
    payload = json.loads(path.read_text(encoding="utf-8"))
    geometry = payload["minecraft:geometry"][0]
    description = geometry["description"]
    if description.get("riftcompanions_rig_contract") == "shared_humanoid_v2":
        return

    source = {entry["name"]: clone(entry) for entry in geometry["bones"]}
    root = source["root"]
    body = source["body"]
    head = source["head"]
    left_arm = source["left_arm"]
    right_arm = source["right_arm"]
    left_leg = source["left_leg"]
    right_leg = source["right_leg"]

    body_cubes = body.pop("cubes", [])
    left_arm_cubes = left_arm.pop("cubes", [])
    right_arm_cubes = right_arm.pop("cubes", [])
    left_leg_cubes = left_leg.pop("cubes", [])
    right_leg_cubes = right_leg.pop("cubes", [])

    body["parent"] = "root"
    left_arm["parent"] = "chest"
    right_arm["parent"] = "chest"
    left_leg["parent"] = "root"
    right_leg["parent"] = "root"
    head["parent"] = "chest"

    left_arm_pivot = clone(left_arm["pivot"])
    right_arm_pivot = clone(right_arm["pivot"])
    left_leg_pivot = clone(left_leg["pivot"])
    right_leg_pivot = clone(right_leg["pivot"])

    extras = [entry for name, entry in source.items() if name not in {
        "root", "body", "head", "left_arm", "right_arm", "left_leg", "right_leg"
    }]
    for entry in extras:
        name = entry["name"]
        if name in {"hat_brim", "hat_crown", "braid"}:
            entry["parent"] = "accessory_1"
        elif name in {"left_rune_band"}:
            entry["parent"] = "left_arm_lower"
        elif name in {"right_rune_band"}:
            entry["parent"] = "right_arm_lower"
        else:
            entry["parent"] = "accessory_2"

    canonical = [
        root,
        body,
        bone("chest", "body", [0, 20, 0], body_cubes),
        head,
        bone("jaw", "head", [0, 24, -4]),
        left_arm,
        bone("left_arm_upper", "left_arm", left_arm_pivot, left_arm_cubes),
        bone("left_arm_lower", "left_arm_upper", [left_arm_pivot[0], 16, left_arm_pivot[2]]),
        bone("left_hand", "left_arm_lower", [left_arm_pivot[0], 12, left_arm_pivot[2]]),
        right_arm,
        bone("right_arm_upper", "right_arm", right_arm_pivot, right_arm_cubes),
        bone("right_arm_lower", "right_arm_upper", [right_arm_pivot[0], 16, right_arm_pivot[2]]),
        bone("right_hand", "right_arm_lower", [right_arm_pivot[0], 12, right_arm_pivot[2]]),
        left_leg,
        bone("left_leg_upper", "left_leg", left_leg_pivot, left_leg_cubes),
        bone("left_leg_lower", "left_leg_upper", [left_leg_pivot[0], 6, left_leg_pivot[2]]),
        bone("left_foot", "left_leg_lower", [left_leg_pivot[0], 0, left_leg_pivot[2] - 0.5]),
        right_leg,
        bone("right_leg_upper", "right_leg", right_leg_pivot, right_leg_cubes),
        bone("right_leg_lower", "right_leg_upper", [right_leg_pivot[0], 6, right_leg_pivot[2]]),
        bone("right_foot", "right_leg_lower", [right_leg_pivot[0], 0, right_leg_pivot[2] - 0.5]),
        bone("accessory_1", "head", [0, 24, 0]),
        bone("accessory_2", "chest", [0, 20, 0]),
    ]
    geometry["bones"] = canonical + extras
    description["riftcompanions_rig_contract"] = "shared_humanoid_v2"
    path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    for role in ROLES:
        migrate(role)
    print("Upgraded companion geometry to shared_humanoid_v2")


if __name__ == "__main__":
    main()
