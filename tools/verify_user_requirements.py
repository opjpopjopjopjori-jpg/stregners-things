#!/usr/bin/env python3
"""Offline compliance audit for the owner's explicit project requirements.

This script performs no download, build, dependency resolution, or network I/O.
"""
from __future__ import annotations

import json
import sys
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RUNTIME = ROOT / "src/main"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def contains_arabic(text: str) -> bool:
    return any("\u0600" <= char <= "\u06ff" for char in text)


def main() -> int:
    # The owner explicitly approved a Forge build workspace after the source-only phase.
    required_build = {
        "build.gradle", "settings.gradle", "gradle.properties", "gradlew", "gradlew.bat",
        "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties",
    }
    missing_build = [path for path in required_build if not (ROOT / path).exists()]
    require(not missing_build, "approved build scaffold is incomplete: " + ", ".join(sorted(missing_build)))
    build_gradle = (ROOT / "build.gradle").read_text(encoding="utf-8")
    require("net.minecraftforge.gradle" in build_gradle and "geckolib-forge-" in build_gradle
            and "geckolib_version" in build_gradle,
            "ForgeGradle or GeckoLib dependency setup is missing")

    # Minecraft + Forge metadata requirement.
    mods_toml = (RUNTIME / "resources/META-INF/mods.toml").read_text(encoding="utf-8")
    require('modId="minecraft"' in mods_toml and '[1.20.1,1.21)' in mods_toml, "mods.toml is not pinned to Minecraft 1.20.1")
    require('modId="forge"' in mods_toml and 'loaderVersion="[47,)"' in mods_toml, "mods.toml is not configured for Forge 47+")
    require('modId="geckolib"' in mods_toml, "GeckoLib dependency declaration missing")

    # High-resolution companion textures requirement.
    textures = {
        "will_seer.png", "hopper_sheriff.png", "eleven_gifted.png", "max_scout.png",
    }
    texture_dir = RUNTIME / "resources/assets/riftcompanions/textures/entity/personal"
    for name in textures:
        image = Image.open(texture_dir / name)
        require(image.size == (512, 512), f"{name} is not 512x512")
        require(image.mode == "RGBA", f"{name} is not RGBA")
    public_dir = RUNTIME / "resources/assets/riftcompanions/textures/entity/public"
    for name in ("seer_public.png", "guardian_public.png", "gifted_public.png", "scout_public.png"):
        image = Image.open(public_dir / name)
        require(image.size == (512, 512) and image.mode == "RGBA", f"public profile texture invalid: {name}")

    hive_sound_dir = RUNTIME / "resources/assets/riftcompanions/sounds/hive"
    for name in ("notice.ogg", "focus.ogg", "release.ogg", "resist.ogg", "recovery.ogg"):
        require((hive_sound_dir / name).exists(), f"missing original Hive sound cue: {name}")

    # English-only project requirement: source, packaged resources, and documentation.
    language_files = list((RUNTIME / "resources/assets/riftcompanions/lang").glob("*.json"))
    require([path.name for path in language_files] == ["en_us.json"], "only en_us.json may be packaged")
    dialogue = json.loads((RUNTIME / "resources/data/riftcompanions/companions_dialogue/core_en_us.json").read_text(encoding="utf-8"))
    require(dialogue.get("locale") == "en_us", "default dialogue locale must be en_us")
    require(len(dialogue.get("entries", [])) >= 280, "English dialogue coverage unexpectedly fell below the per-role content floor")
    counts = {}
    for entry in dialogue["entries"]:
        counts[entry["role"]] = counts.get(entry["role"], 0) + 1
    require(all(counts.get(role, 0) >= 70 for role in ("guardian", "seer", "gifted", "scout")), "each core role needs at least 70 default English lines")

    skipped_suffixes = {".png", ".jpg", ".jpeg", ".zip", ".sha256"}
    non_english = []
    for path in ROOT.rglob("*"):
        if not path.is_file() or path.suffix.lower() in skipped_suffixes:
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        if contains_arabic(text):
            non_english.append(path.relative_to(ROOT))
    require(not non_english, "Arabic text found in project: " + ", ".join(map(str, non_english)))

    # Core feature source presence requirement.
    expected = [
        "formation/FormationCoordinator.java",
        "team/TeamDirector.java",
        "team/ThreatResponseDirector.java",
        "context/ContextualInteractionDirector.java",
        "context/ContextInteractionReloadListener.java",
        "context/ContextInteractionRegistry.java",
        "context/ContextInteractionDefinition.java",
        "context/ContextInteractionKind.java",
        "team/events/TeamEventQueue.java",
        "server/RescueService.java",
        "world/BaseLifeCoordinator.java",
        "resource/CompanionInventory.java",
        "conversation/ConversationService.java",
        "social/SocialCue.java",
        "social/CompanionSocialDirector.java",
        "story/StoryService.java",
        "content/ContentProfileRegistry.java",
        "transaction/ActionLedger.java",
        "transaction/ReservationBook.java",
        "persistence/SaveMigrationService.java",
        "persistence/SaveRecoveryService.java",
        "safety/SafeModeService.java",
        "duo/DuoDynamicsService.java",
        "intention/IntentionService.java",
        "arc/ArcService.java",
        "entity/CompanionVisualState.java",
        "entity/CompanionLookIntent.java",
        "presentation/CompanionGaitPresentationService.java",
        "server/BaseMilestoneService.java",
        "server/MilestoneTransactionService.java",
        "safety/CompanionFaultService.java",
        "safety/CompanionFaultRecord.java",
        "policy/PlayerPolicyService.java",
        "policy/PlayerPolicyState.java",
        "encounter/ThreatProfileReloadListener.java",
        "world/assessment/StructureProfileOverrideReloadListener.java",
        "team/PlanTarget.java",
        "navigation/CompanionNavigationService.java",
        "navigation/FormationSlotReservationService.java",
        "navigation/NavigationIntent.java",
        "consequence/ConsequenceService.java",
        "identity/PlayerIdentityService.java",
        "perception/CompanionPerceptionService.java",
        "interrupt/ActionInterruptService.java",
        "spatial/SpatialEtiquetteService.java",
        "scene/SetPieceService.java",
        "playtest/PlaytestTelemetryService.java",
        "playtest/BehaviorTestCatalog.java",
        "hive/control/HiveChannelManager.java",
        "server/GuardianBraceService.java",
        "server/RecallPlacementReservationService.java",
        "resource/WorldFreePickupService.java",
        "resource/TeamSupplyService.java",
        "resource/TeamSupplyState.java",
        "mental/MindAnchorService.java",
        "mental/MentalEffectReloadListener.java",
        "story/MysteryBoardState.java",
        "story/StoryPromiseState.java",
        "encounter/EncounterContextService.java",
        "encounter/EncounterDialogueReloadListener.java",
        "behavior/RoleBehaviorService.java",
        "behavior/BehaviorProfileReloadListener.java",
        "behavior/GiftedBehaviorService.java",
        "behavior/GuardianBehaviorService.java",
        "server/TeamSafetyControlService.java",
        "server/CompanionPresentationSoundService.java",
        "performance/CompanionPerformanceMonitor.java",
        "navigation/NavigationDebugSnapshot.java",
        "mode/GameModePolicyService.java",
        "mode/VanillaRulePolicyService.java",
        "mode/VanillaRuleSnapshot.java",
        "onboarding/OnboardingService.java",
        "onboarding/OnboardingState.java",
        "animation/CompanionAnimationController.java",
        "animation/CompanionAnimationHandler.java",
        "animation/AnimationStateMapper.java",
        "animation/CompanionAnimationStateMapper.java",
        "animation/AnimationMarkerPlan.java",
        "decision/TeamDecisionService.java",
    ]
    missing = [item for item in expected if not (RUNTIME / "java/com/riftcompanions" / item).exists()]
    require(not missing, "expected core source files missing: " + ", ".join(missing))

    required_docs = {
        "docs/RELIABILITY_AND_RECOVERY.md", "docs/FEATURE_FLAGS_AND_DATA_PACKS.md",
        "docs/CHAOS_AND_ACCEPTANCE_TESTS.md", "docs/BEHAVIOR_ACCEPTANCE_CATALOG.md", "docs/DUO_ARCS_INTENTIONS.md",
        "docs/VISUAL_HUD_AUDIO_QA.md", "docs/ASSET_REGISTRY.md", "docs/NAVIGATION_AND_STUCK_RECOVERY.md",
        "docs/CONSEQUENCE_PERCEPTION_SCENES.md", "docs/PLAYTEST_CAMPAIGN.md", "docs/HIVE_SURGE_CONTRACT.md", "docs/WILL_GIFTED_PRECISION_POWER_CONTRACT.md",
        "docs/RESOURCE_OWNERSHIP_AND_TEAM_SUPPLY.md", "docs/TEAM_SUPPLY_STATUS_UI_CONTRACT.md", "docs/MIND_ANCHOR_CONTRACT.md",
        "docs/STORY_MYSTERY_PROMISE_CONTRACT.md", "docs/STORY_JOURNAL_PRESENTATION_CONTRACT.md", "docs/ENCOUNTER_INTELLIGENCE_CONTRACT.md",
        "docs/ROLE_BEHAVIOR_AND_CAPABILITY_CONTRACT.md", "docs/ROLE_READINESS_AND_AFTER_ACTION_CONTRACT.md",
        "docs/MODE_PERFORMANCE_AND_DECISION_CONTRACT.md", "docs/WORLD_RULES_ONBOARDING_ACCESSIBILITY_CONTRACT.md",
        "docs/ANIMATION_DIRECTOR_SYSTEM.md", "docs/ANIMATION_REVIEW_AUDIT.md", "docs/TEXTURE_ART_DIRECTION_CONTRACT.md", "docs/FACETED_CHARACTER_RIG_CONTRACT.md", "docs/SOCIAL_DIALOGUE_AND_INTERACTION_DIRECTOR.md", "docs/ORIGINAL_AUDIO_CUE_LIBRARY.md",
        "docs/OPERATIONAL_SAFETY_AND_OBSERVABILITY.md", "docs/RECALL_PLACEMENT_AND_ANTI_STACKING.md", "docs/NATURAL_GAIT_AND_HEAD_LOOK_CONTRACT.md", "docs/VISIBLE_HOSTILE_RESPONSE_CONTRACT.md", "docs/CONTEXTUAL_INTERACTION_DIRECTOR.md", "docs/SCOPE_GOVERNANCE_AND_PUBLICATION_POLICY.md",
    }
    absent_docs = [item for item in required_docs if not (ROOT / item).exists()]
    require(not absent_docs, "reliability/duo documentation missing: " + ", ".join(absent_docs))
    require((RUNTIME / "resources/data/riftcompanions/companion_intentions/core_intentions.json").exists(), "intention data pack missing")
    require((RUNTIME / "resources/data/riftcompanions/item_classifications/vanilla_baseline.json").exists(), "item classification data pack missing")
    require((RUNTIME / "resources/data/riftcompanions/threat_profiles/vanilla_baseline.json").exists(), "threat profile data pack missing")
    require((RUNTIME / "resources/data/riftcompanions/structure_overrides/vanilla_baseline.json").exists(), "structure override data pack missing")

    print("PASS: Forge 1.20.1 metadata")
    print("PASS: approved Forge build scaffold")
    print("PASS: eight 512x512 faceted companion textures")
    print(f"PASS: English-only project and {len(dialogue['entries'])} English dialogue entries")
    print("PASS: expected team, rescue, base, inventory, conversation, natural-world, social dialogue, reliability, navigation, consequence, perception, scene, policy, profile, duo, arc, and visual-state source modules")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as error:
        print(f"FAIL: {error}", file=sys.stderr)
        raise SystemExit(1)
