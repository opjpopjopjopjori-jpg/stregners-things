#!/usr/bin/env python3
"""Append deterministic, interruptible social/world interaction clips to all companion animation atlases."""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ANIMATIONS = ROOT / "src/main/resources/assets/riftcompanions/animations"
ROLES = ("seer", "guardian", "gifted", "scout")


def social_clips(role: str) -> dict[str, dict]:
    prefix = f"animation.{role}."
    clips = {
        "social_listen": {
            "animation_length": 1.35,
            "bones": {
                "head": {"rotation": {"0.0": [0, -4, 0], "0.42": [5, 3, 0], "0.76": [-3, -2, 0], "1.35": [0, -4, 0]}},
                "body": {"rotation": {"0.0": [0, 0, 0], "0.42": [2, 0, 0], "1.35": [0, 0, 0]}},
            },
        },
        "social_point": {
            "animation_length": 0.78,
            "bones": {
                "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.23": [-42, 0, -18], "0.52": [-32, 0, -12], "0.78": [0, 0, 0]}},
                "head": {"rotation": {"0.0": [0, 0, 0], "0.23": [0, 12, 0], "0.78": [0, 0, 0]}},
            },
        },
        "social_reassure": {
            "animation_length": 1.05,
            "bones": {
                "left_arm": {"rotation": {"0.0": [0, 0, 0], "0.30": [-20, 0, 16], "0.65": [-15, 0, 11], "1.05": [0, 0, 0]}},
                "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.30": [-20, 0, -16], "0.65": [-15, 0, -11], "1.05": [0, 0, 0]}},
                "head": {"rotation": {"0.0": [0, 0, 0], "0.34": [3, 0, 0], "1.05": [0, 0, 0]}},
            },
        },
        "social_gear_check": {
            "animation_length": 1.20,
            "bones": {
                "head": {"rotation": {"0.0": [0, 0, 0], "0.26": [18, 0, 0], "0.78": [12, 0, 0], "1.20": [0, 0, 0]}},
                "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.28": [-36, 0, -8], "0.82": [-30, 0, -4], "1.20": [0, 0, 0]}},
            },
        },
        "social_observe": {
            "animation_length": 1.40,
            "bones": {
                "head": {"rotation": {"0.0": [0, -10, 0], "0.48": [0, 10, 0], "0.95": [0, 4, 0], "1.40": [0, -10, 0]}},
                "body": {"rotation": {"0.0": [0, 0, 0], "0.48": [0, 5, 0], "1.40": [0, 0, 0]}},
            },
        },
        "social_campfire": {
            "animation_length": 1.55,
            "bones": {
                "body": {"rotation": {"0.0": [0, 0, 0], "0.38": [8, 0, 0], "0.95": [6, 0, 0], "1.55": [0, 0, 0]}},
                "left_arm": {"rotation": {"0.0": [0, 0, 0], "0.38": [-18, 0, 12], "1.55": [0, 0, 0]}},
                "head": {"rotation": {"0.0": [0, 0, 0], "0.52": [5, -4, 0], "1.55": [0, 0, 0]}},
            },
        },
        "social_weather": {
            "animation_length": 1.10,
            "bones": {
                "left_arm": {"rotation": {"0.0": [0, 0, 0], "0.25": [-54, 0, 12], "0.62": [-42, 0, 8], "1.10": [0, 0, 0]}},
                "head": {"rotation": {"0.0": [0, 0, 0], "0.25": [-8, 0, 0], "1.10": [0, 0, 0]}},
            },
        },
        "social_horizon": {
            "animation_length": 1.00,
            "bones": {
                "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.25": [-58, 0, -15], "0.68": [-46, 0, -12], "1.00": [0, 0, 0]}},
                "head": {"rotation": {"0.0": [0, 0, 0], "0.25": [-6, 10, 0], "1.00": [0, 0, 0]}},
            },
        },
        "social_base": {
            "animation_length": 1.45,
            "bones": {
                "body": {"rotation": {"0.0": [0, 0, 0], "0.42": [-3, 0, 0], "0.92": [2, 0, 0], "1.45": [0, 0, 0]}},
                "head": {"rotation": {"0.0": [0, 0, 0], "0.52": [0, 6, 0], "1.45": [0, 0, 0]}},
            },
        },
        "social_work": {
            "animation_length": 1.08,
            "bones": {
                "left_arm": {"rotation": {"0.0": [0, 0, 0], "0.26": [-32, 0, 14], "0.70": [-24, 0, 8], "1.08": [0, 0, 0]}},
                "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.26": [-32, 0, -14], "0.70": [-24, 0, -8], "1.08": [0, 0, 0]}},
                "head": {"rotation": {"0.0": [0, 0, 0], "0.26": [12, 0, 0], "1.08": [0, 0, 0]}},
            },
        },
        "social_cave": {
            "animation_length": 1.18,
            "bones": {
                "head": {"rotation": {"0.0": [0, -8, 0], "0.34": [8, 8, 0], "0.82": [5, 3, 0], "1.18": [0, -8, 0]}},
                "body": {"rotation": {"0.0": [0, 0, 0], "0.34": [5, 0, 0], "1.18": [0, 0, 0]}},
            },
        },
        "social_village": {
            "animation_length": 1.08,
            "bones": {
                "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.28": [-24, 0, -12], "0.72": [-17, 0, -8], "1.08": [0, 0, 0]}},
                "head": {"rotation": {"0.0": [0, 0, 0], "0.28": [0, 6, 0], "1.08": [0, 0, 0]}},
            },
        },
        "social_travel": {
            "animation_length": 1.12,
            "bones": {
                "body": {"rotation": {"0.0": [0, 0, 0], "0.35": [-4, 0, 0], "0.76": [3, 0, 0], "1.12": [0, 0, 0]}},
                "head": {"rotation": {"0.0": [0, 0, 0], "0.35": [0, 9, 0], "1.12": [0, 0, 0]}},
            },
        },
        "social_calm": {
            "animation_length": 1.65,
            "bones": {
                "body": {"scale": {"0.0": [1, 1, 1], "0.62": [1.02, 1.035, 1.02], "1.65": [1, 1, 1]}},
                "head": {"rotation": {"0.0": [0, 0, 0], "0.62": [-3, 0, 0], "1.65": [0, 0, 0]}},
            },
        },
    }
    role_specific = {
        "seer": {
            "social_trace_signal": {"animation_length": 1.12, "bones": {"right_arm": {"rotation": {"0.0": [0, 0, 0], "0.30": [-34, 0, -20], "0.74": [-20, 0, -12], "1.12": [0, 0, 0]}}, "head": {"rotation": {"0.0": [0, 0, 0], "0.30": [0, 11, 0], "1.12": [0, 0, 0]}}}},
            "social_reflect": {"animation_length": 1.45, "bones": {"head": {"rotation": {"0.0": [0, 0, 0], "0.40": [15, -4, 0], "1.45": [0, 0, 0]}}, "body": {"rotation": {"0.0": [0, 0, 0], "0.40": [5, 0, 0], "1.45": [0, 0, 0]}}}},
        },
        "guardian": {
            "social_perimeter_scan": {"animation_length": 1.25, "bones": {"head": {"rotation": {"0.0": [0, -16, 0], "0.52": [0, 16, 0], "1.25": [0, -16, 0]}}, "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.52": [-18, 0, -10], "1.25": [0, 0, 0]}}}},
            "social_radio_check": {"animation_length": 0.92, "bones": {"left_arm": {"rotation": {"0.0": [0, 0, 0], "0.24": [-62, 0, 12], "0.60": [-54, 0, 9], "0.92": [0, 0, 0]}}, "head": {"rotation": {"0.0": [0, 0, 0], "0.24": [6, 0, 0], "0.92": [0, 0, 0]}}}},
        },
        "gifted": {
            "social_grounding_pose": {"animation_length": 1.30, "bones": {"left_arm": {"rotation": {"0.0": [0, 0, 0], "0.34": [-42, 0, 14], "0.88": [-30, 0, 10], "1.30": [0, 0, 0]}}, "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.34": [-42, 0, -14], "0.88": [-30, 0, -10], "1.30": [0, 0, 0]}}}},
            "social_breathe": {"animation_length": 1.45, "bones": {"body": {"scale": {"0.0": [1, 1, 1], "0.60": [1.015, 1.05, 1.015], "1.45": [1, 1, 1]}}, "head": {"rotation": {"0.0": [0, 0, 0], "0.60": [-5, 0, 0], "1.45": [0, 0, 0]}}}},
        },
        "scout": {
            "social_map_read": {"animation_length": 1.20, "bones": {"head": {"rotation": {"0.0": [0, 0, 0], "0.26": [18, 0, 0], "0.82": [12, 0, 0], "1.20": [0, 0, 0]}}, "left_arm": {"rotation": {"0.0": [0, 0, 0], "0.26": [-36, 0, 16], "1.20": [0, 0, 0]}}}},
            "social_route_confirm": {"animation_length": 0.86, "bones": {"right_arm": {"rotation": {"0.0": [0, 0, 0], "0.22": [-48, 0, -14], "0.55": [-34, 0, -10], "0.86": [0, 0, 0]}}, "head": {"rotation": {"0.0": [0, 0, 0], "0.22": [0, 10, 0], "0.86": [0, 0, 0]}}}},
        },
    }
    clips.update(role_specific[role])
    return {prefix + key: value for key, value in clips.items()}


def main() -> None:
    for role in ROLES:
        path = ANIMATIONS / f"{role}.animation.json"
        payload = json.loads(path.read_text(encoding="utf-8"))
        animations = payload.setdefault("animations", {})
        # Clean an invalid early-generator key shape before applying the canonical names.
        for key in list(animations):
            if f"animation.{role}.animation.{role}." in key:
                animations.pop(key)
        animations.update(social_clips(role))
        path.write_text(json.dumps(payload, indent=2) + "\n", encoding="utf-8")
        print(f"Expanded {path.name} with social/world interaction clips")


if __name__ == "__main__":
    main()
