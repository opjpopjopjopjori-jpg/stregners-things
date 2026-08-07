#!/usr/bin/env python3
"""Rebuild all original faceted companion presentation assets from source.

Order matters: geometry creates the shared UV contract; texture generation
replaces every personal/public PNG; animation rebuilding adds facial/hair and
role-motion layers; the source preview is regenerated last.
"""
from __future__ import annotations

from generate_gecko_assets import main as generate_geometry
from generate_high_res_textures import main as generate_textures
from rebuild_facial_motion import main as generate_motion
from render_modular_model_preview import main as generate_preview


def main() -> None:
    generate_geometry()
    generate_textures()
    generate_motion()
    generate_preview()
    print("Rebuilt the complete original faceted-character visual pipeline.")


if __name__ == "__main__":
    main()
