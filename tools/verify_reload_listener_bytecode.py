#!/usr/bin/env python3
"""Verify that packaged JSON reload listeners initialize Gson before INSTANCE.

Usage:
    JAVAP=/path/to/javap python3 tools/verify_reload_listener_bytecode.py path/to/mod.jar

This is intentionally a bytecode check, not merely a source-order check. It
protects the integrated-server world-creation hotfix from compiler or source
regressions that could again pass a null Gson to SimpleJsonResourceReloadListener.
"""
from __future__ import annotations

import os
import subprocess
import sys
from pathlib import Path


LISTENERS = (
    "com.riftcompanions.behavior.BehaviorProfileReloadListener",
    "com.riftcompanions.compat.CompatibilityPackReloadListener",
    "com.riftcompanions.context.ContextInteractionReloadListener",
    "com.riftcompanions.dialogue.DialogueReloadListener",
    "com.riftcompanions.encounter.EncounterDialogueReloadListener",
    "com.riftcompanions.encounter.ThreatProfileReloadListener",
    "com.riftcompanions.intention.IntentionReloadListener",
    "com.riftcompanions.mental.MentalEffectReloadListener",
    "com.riftcompanions.resource.ItemClassificationReloadListener",
    "com.riftcompanions.world.assessment.StructureProfileOverrideReloadListener",
)


def fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)


def static_initializer(output: str, class_name: str) -> str:
    marker = "  static {};"
    start = output.find(marker)
    if start < 0:
        fail(f"missing static initializer: {class_name}")
    return output[start:]


def main() -> int:
    if len(sys.argv) != 2:
        fail("usage: verify_reload_listener_bytecode.py path/to/mod.jar")
    jar = Path(sys.argv[1]).resolve()
    if not jar.is_file():
        fail(f"JAR not found: {jar}")
    javap = os.environ.get("JAVAP", "javap")

    for class_name in LISTENERS:
        result = subprocess.run(
            [javap, "-classpath", str(jar), "-c", "-p", class_name],
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )
        if result.returncode != 0:
            fail(f"javap failed for {class_name}: {result.stderr.strip()}")
        initializer = static_initializer(result.stdout, class_name)
        gson_at = initializer.find("Field GSON:")
        instance_at = initializer.find("Field INSTANCE:")
        if gson_at < 0 or instance_at < 0:
            fail(f"missing Gson or singleton bytecode field write: {class_name}")
        if gson_at > instance_at:
            fail(f"INSTANCE writes before GSON: {class_name}")
        print(f"PASS: {class_name} initializes Gson before INSTANCE")

    print(f"PASS: {len(LISTENERS)} packaged JSON reload listeners pass static initialization order")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
