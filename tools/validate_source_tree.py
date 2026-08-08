#!/usr/bin/env python3
"""Source/resource validation for the approved Forge build workspace.

This does not download Forge, GeckoLib, Gradle, or any dependency. It catches
asset/schema regressions before the owner later attaches a real Forge workspace.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

from PIL import Image
from verify_animations_comprehensive import main as audit_animations

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/riftcompanions"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> int:
    json_files = list((ROOT / "src/main/resources").glob("**/*.json"))
    for file in json_files:
        json.loads(file.read_text(encoding="utf-8"))

    roles = {
        "seer": "will_seer.png",
        "guardian": "hopper_sheriff.png",
        "gifted": "eleven_gifted.png",
        "scout": "max_scout.png",
    }
    public_textures = {
        "seer": "seer_public.png",
        "guardian": "guardian_public.png",
        "gifted": "gifted_public.png",
        "scout": "scout_public.png",
    }
    texture_size = 64
    for role, texture_name in roles.items():
        texture = ASSETS / "textures/entity/personal" / texture_name
        image = Image.open(texture)
        require(image.size == (texture_size, texture_size),
                f"{texture_name} must remain {texture_size}x{texture_size}; found {image.size}")
        require(image.mode == "RGBA", f"{texture_name} must remain RGBA; found {image.mode}")
        public_image = Image.open(ASSETS / "textures/entity/public" / public_textures[role])
        require(public_image.size == (texture_size, texture_size) and public_image.mode == "RGBA",
                f"public {role} texture must match the faceted 64px atlas contract")
        geometry = json.loads((ASSETS / "geo" / f"{role}.geo.json").read_text(encoding="utf-8"))
        geometry_root = geometry["minecraft:geometry"][0]
        description = geometry_root["description"]
        require(description["texture_width"] == texture_size and description["texture_height"] == texture_size,
                f"{role}.geo.json must map against the modular {texture_size}px texture atlas")
        canonical_rig = {"root", "body", "chest", "head", "jaw", "left_arm_upper", "left_arm_lower", "left_hand",
                         "right_arm_upper", "right_arm_lower", "right_hand", "left_leg_upper", "left_leg_lower",
                         "left_foot", "right_leg_upper", "right_leg_lower", "right_foot", "accessory_1", "accessory_2"}
        bones = geometry_root["bones"]
        bone_names = {bone["name"] for bone in bones}
        require(len(bone_names) == len(bones), f"{role} contains duplicate Geo bone names")
        for source_bone in bones:
            parent = source_bone.get("parent")
            require(parent is None or parent in bone_names,
                    f"{role} Geo bone {source_bone['name']} has an unresolved parent")
            for source_cube in source_bone.get("cubes", []):
                require(len(source_cube.get("origin", [])) == 3 and len(source_cube.get("size", [])) == 3
                        and all(value > 0 for value in source_cube["size"]),
                        f"{role} contains an invalid faceted Geo cube")
        parent_by_name = {source_bone["name"]: source_bone.get("parent") for source_bone in bones}
        for bone_name in bone_names:
            ancestry = set()
            current = bone_name
            while current is not None:
                require(current not in ancestry, f"{role} contains a Geo bone parent cycle at {bone_name}")
                ancestry.add(current)
                current = parent_by_name.get(current)
        require(description.get("riftcompanions_rig_contract") == "shared_humanoid_v2", f"{role} must use shared_humanoid_v2")
        require(description.get("riftcompanions_visual_contract") == "faceted_character_64_v2",
                f"{role} must use the faceted character visual contract")
        require(canonical_rig.issubset(bone_names), f"{role} is missing a canonical shared animation rig bone")
        pivots = {bone["name"]: bone["pivot"] for bone in bones}
        for bone_name, pivot in {"head": [0, 24, 0], "jaw": [0, 24, -4], "left_arm": [5, 22, 0],
                                 "right_arm": [-5, 22, 0], "left_leg": [2, 12, 0], "right_leg": [-2, 12, 0]}.items():
            require(pivots.get(bone_name) == pivot, f"{role} moved canonical animation pivot {bone_name}")
        require(len(bones) >= 68 and sum(len(bone.get("cubes", [])) for bone in bones) >= 57,
                f"{role} faceted character model unexpectedly lost visual geometry detail")
        require(sum(name.startswith(role + "_") for name in bone_names) >= 19,
                f"{role} is missing role-specific facial or hair silhouette bones")
        facial_bones = {"eye_left_white", "eye_right_white", "eye_left_pupil", "eye_right_pupil",
                        "eye_left_upper_lid", "eye_right_upper_lid", "brow_left", "brow_right",
                        "mouth_neutral", "face_core", "face_chin"}
        require(facial_bones.issubset(bone_names), f"{role} is missing faceted face components")
        hair_floor = 2 if role == "guardian" else 5
        require(sum("hair" in name for name in bone_names) >= hair_floor,
                f"{role} is missing independently animated hair-layer bones")
        require(description.get("visible_bounds_width", 0) >= 4.0 and description.get("visible_bounds_height", 0) >= 4.4,
                f"{role} modular silhouette bounds are too small")
        for source_bone in bones:
            for source_cube in source_bone.get("cubes", []):
                for face in source_cube.get("uv", {}).values():
                    origin = face.get("uv", [])
                    size = face.get("uv_size", [])
                    require(len(origin) == 2 and len(size) == 2 and size[0] > 0 and size[1] > 0,
                            f"{role} contains an invalid modular UV face")
                    require(0 <= origin[0] and 0 <= origin[1]
                            and origin[0] + size[0] <= texture_size and origin[1] + size[1] <= texture_size,
                            f"{role} contains an out-of-bounds modular UV face")
        animations = json.loads((ASSETS / "animations" / f"{role}.animation.json").read_text(encoding="utf-8"))["animations"]
        for clip_name, animation in animations.items():
            animation_bones = set(animation.get("bones", {}))
            require(animation_bones.issubset(bone_names),
                    f"{role} animation {clip_name} references a missing Geo bone: " + ", ".join(sorted(animation_bones - bone_names)))
        for name in ("idle", "walk", "downed", "downed_hold", "recover", "idle_alert", "run", "melee_attack_1",
                     "melee_attack_2", "melee_attack_3", "melee_block", "combat_hit_react", "combat_dodge_left",
                     "combat_dodge_right", "combat_dodge_back", "interact_talk", "interact_point", "interact_pickup",
                     "interact_use", "stuck_struggle", "spawn_appear", "dismiss_fade", "emotion_fear",
                     "emotion_relief", "emotion_determined", "emotion_exhausted", "calm_look_around", "calm_sit_rest",
                     "social_listen", "social_point", "social_reassure", "social_gear_check", "social_observe",
                     "social_campfire", "social_weather", "social_horizon", "social_base", "social_work",
                     "social_cave", "social_village", "social_travel", "social_calm"):
            require(f"animation.{role}.{name}" in animations, f"missing expanded {role} animation: {name}")
        contextual_runtime = {"context_animal_greet", "context_animal_observe", "context_field_note", "context_threat_brief", "context_loot_note", "context_biome_brief", "context_structure_brief", "context_rest_request", "context_route_note"}
        role_runtime = {
            "seer": {"anomaly_notice", "hive_focus", "hive_release_suspend", "hive_release_redirect", "hive_release_shatter", "hive_resist", "hive_recovery", "social_trace_signal", "social_reflect"},
            "guardian": {"guard_stance", "protect", "social_perimeter_scan", "social_radio_check"},
            "gifted": {"power_notice", "power_focus", "push_release", "shield_hold", "rescue_pull", "exhausted_recovery", "social_grounding_pose", "social_breathe"},
            "scout": {"route_point", "lookout", "ranged_ready", "anchor_call", "ranged_draw", "ranged_aim", "ranged_release", "mind_anchor_call", "mind_anchor_hold", "social_map_read", "social_route_confirm"},
        }
        require(all(f"animation.{role}.{name}" in animations for name in role_runtime[role] | contextual_runtime),
                f"{role} is missing a state-mapper/runtime-selected animation clip")
        layer_keys = ("face_idle", "face_alert", "face_talk", "face_combat", "face_power", "face_context", "face_recovery",
                      "face_glance_left", "face_glance_right", "face_check_back_left", "face_check_back_right",
                      "secondary_idle", "secondary_walk", "secondary_run", "secondary_combat", "secondary_power", "secondary_context", "secondary_recovery")
        require(all(f"animation.{role}.{layer}" in animations for layer in layer_keys),
                f"{role} is missing layered face or secondary-motion clips")
        for gesture in ("face_glance_left", "face_glance_right", "face_check_back_left", "face_check_back_right"):
            require(animations[f"animation.{role}.{gesture}"].get("loop") is False,
                    f"{role} {gesture} must be a finite look-and-return gesture, not a static loop")
        alert_head = animations[f"animation.{role}.face_alert"].get("bones", {}).get("head", {}).get("rotation", {})
        require(alert_head and all(abs(frame[1]) < 0.001 for frame in alert_head.values()),
                f"{role} face alert must not perform a periodic left/right head scan")
        idle_bones = set(animations[f"animation.{role}.face_idle"].get("bones", {}))
        require({"eye_left_pupil", "eye_right_pupil", "eye_left_upper_lid", "eye_right_upper_lid", "brow_left", "brow_right"}.issubset(idle_bones),
                f"{role} face idle is missing authored blink, gaze, or brow motion")
        for movement in ("walk", "run"):
            movement_bones = set(animations[f"animation.{role}.secondary_{movement}"].get("bones", {}))
            require(any("hair" in name for name in movement_bones),
                    f"{role} {movement} is missing secondary hair motion")
        if role == "seer":
            focus_bones = set(animations["animation.seer.face_power"].get("bones", {}))
            secondary_bones = set(animations["animation.seer.secondary_power"].get("bones", {}))
            require({"eye_left_pupil", "eye_right_pupil", "brow_left", "brow_right"}.issubset(focus_bones)
                    and any("hair" in name for name in secondary_bones),
                    "Seer precision focus is missing layered faceted face or hair presentation")
        if role == "gifted":
            focus_bones = set(animations["animation.gifted.face_power"].get("bones", {}))
            secondary_bones = set(animations["animation.gifted.secondary_power"].get("bones", {}))
            require({"eye_left_pupil", "eye_right_pupil", "brow_left", "brow_right"}.issubset(focus_bones)
                    and any("hair" in name for name in secondary_bones),
                    "Gifted precision focus is missing layered faceted face or hair presentation")

    gifted = json.loads((ASSETS / "animations/gifted.animation.json").read_text(encoding="utf-8"))["animations"]
    require("animation.gifted.rescue_pull" in gifted, "missing gifted rescue_pull animation")
    seer = json.loads((ASSETS / "animations/seer.animation.json").read_text(encoding="utf-8"))["animations"]
    scout = json.loads((ASSETS / "animations/scout.animation.json").read_text(encoding="utf-8"))["animations"]
    require({"animation.seer.hive_release_suspend", "animation.seer.hive_release_redirect", "animation.seer.hive_release_shatter"}.issubset(seer),
            "missing Seer release variant clips")
    require({"animation.scout.ranged_draw", "animation.scout.ranged_aim", "animation.scout.ranged_release", "animation.scout.mind_anchor_hold"}.issubset(scout),
            "missing Scout ranged or Mind Anchor clips")
    rig_contract = json.loads((ASSETS / "animation_contracts/shared_humanoid_rig.json").read_text(encoding="utf-8"))
    state_contract = json.loads((ASSETS / "animation_contracts/companion_animation_states.json").read_text(encoding="utf-8"))
    marker_contract = json.loads((ASSETS / "animation_contracts/core_animation_timeline.json").read_text(encoding="utf-8"))
    require(rig_contract.get("rig_id") == "shared_humanoid_v2"
            and rig_contract.get("visual_extension_contract") == "faceted_character_64_v2",
            "shared modular rig contract is invalid")
    require(state_contract.get("state_authority") == "server_derived_visual_state", "animation state authority contract is invalid")
    forbidden_markers = {"MODIFY_INVENTORY", "PLACE_BLOCK", "DAMAGE_RANDOM_ENTITY", "START_PLAN", "WRITE_MEMORY", "CHANGE_AI_STATE"}
    require(not any(entry.get("marker") in forbidden_markers for entry in marker_contract.get("clip_markers", [])),
            "forbidden gameplay marker found in animation timeline")

    companion_audio = list((ASSETS / "sounds/companions").glob("*.ogg"))
    ambience_audio = list((ASSETS / "sounds/ambience").glob("*.ogg"))
    require(len(companion_audio) >= 17 and len(ambience_audio) >= 4,
            "original companion action and ambience audio library unexpectedly shrank")
    sounds_json = json.loads((ASSETS / "sounds.json").read_text(encoding="utf-8"))
    for key in ("guardian_guard_signal", "seer_focus", "gifted_push", "scout_anchor", "ui_safe_mode"):
        require(key in sounds_json, "missing registered original sound cue: " + key)

    dialogue = json.loads((ROOT / "src/main/resources/data/riftcompanions/companions_dialogue/core_en_us.json").read_text(encoding="utf-8"))
    require(dialogue.get("locale") == "en_us", "default dialogue pack must remain English-only")
    require(not any(any("\u0600" <= char <= "\u06ff" for char in entry["text"]) for entry in dialogue["entries"]), "Arabic text found in default dialogue pack")
    entries = dialogue["entries"]
    require(len(entries) >= 500, "expanded dialogue library unexpectedly shrank below the social interaction floor")
    require(len({entry["id"] for entry in entries}) == len(entries), "duplicate dialogue IDs detected")
    social_cues = ("campfire", "weather", "horizon", "base", "work", "cave", "village", "travel", "calm")
    for cue in social_cues:
        for role in roles:
            require(any(entry["role"] == role and entry["trigger"] == f"social_{cue}_lead" for entry in entries),
                    f"missing social lead dialogue for {role}:{cue}")
            require(any(entry["role"] == role and entry["trigger"] == f"social_{cue}_reply" for entry in entries),
                    f"missing social reply dialogue for {role}:{cue}")

    context_data = json.loads((ROOT / "src/main/resources/data/riftcompanions/contextual_interactions/vanilla_field_guide.json").read_text(encoding="utf-8"))
    require(context_data.get("schema_version") == 2 and len(context_data.get("entries", [])) >= 198,
            "multi-stage contextual interaction coverage unexpectedly shrank")
    action_source = (ROOT / "src/main/java/com/riftcompanions/entity/CompanionAction.java").read_text(encoding="utf-8")
    context_ids = set()
    context_stages = {}
    for definition in context_data["entries"]:
        require({"id", "kind", "match", "group", "stage", "lead_trigger", "reply_trigger", "action", "reply_action", "priority", "cooldown_ticks", "requires_focused_monster"}.issubset(definition),
                "invalid multi-stage contextual interaction definition")
        require(definition["id"] not in context_ids, "duplicate contextual interaction id")
        context_ids.add(definition["id"])
        context_stages.setdefault(definition["group"], set()).add(definition["stage"])
        require(definition["action"] in action_source and definition["reply_action"] in action_source
                and 0 <= definition["priority"] <= 4 and 100 <= definition["cooldown_ticks"] <= 24000,
                "contextual interaction action/priority/cooldown is invalid")
        for role in roles:
            require(any(entry["role"] == role and entry["trigger"] == definition["lead_trigger"] for entry in entries),
                    f"missing contextual lead dialogue for {role}:{definition['lead_trigger']}")
            require(any(entry["role"] == role and entry["trigger"] == definition["reply_trigger"] for entry in entries),
                    f"missing contextual reply dialogue for {role}:{definition['reply_trigger']}")
    require(all({"DISCOVERY", "REVISIT"}.issubset(stages) for stages in context_stages.values()),
            "every contextual group must provide Discovery and Revisit scenes")

    intentions = json.loads((ROOT / "src/main/resources/data/riftcompanions/companion_intentions/core_intentions.json").read_text(encoding="utf-8"))
    require(intentions.get("schema_version") == 1, "intention content schema must remain version 1")
    require(len(intentions.get("entries", [])) >= 4, "expected four base optional intention definitions")
    required_intention = {"id", "role", "context", "optional_action", "completion_key", "reward_summary", "dialogue_trigger"}
    for entry in intentions["entries"]:
        require(required_intention.issubset(entry), "invalid intention entry schema")
        require(not any("\u0600" <= char <= "\u06ff" for value in entry.values() if isinstance(value, str) for char in value), "Arabic text found in intention content")

    classifications = json.loads((ROOT / "src/main/resources/data/riftcompanions/item_classifications/vanilla_baseline.json").read_text(encoding="utf-8"))
    require(classifications.get("schema_version") == 1 and len(classifications.get("entries", [])) >= 8,
            "safe item classification baseline unexpectedly shrank")

    threat_profiles = json.loads((ROOT / "src/main/resources/data/riftcompanions/threat_profiles/vanilla_baseline.json").read_text(encoding="utf-8"))
    require(threat_profiles.get("schema_version") == 1 and len(threat_profiles.get("entries", [])) >= 4,
            "advisory threat profile baseline unexpectedly shrank")
    for entry in threat_profiles["entries"]:
        require({"id", "entity_type", "archetype", "caution_bias"}.issubset(entry), "invalid threat profile schema")
        require(0 <= entry["caution_bias"] <= 30, "threat caution bias must remain bounded")

    structure_overrides = json.loads((ROOT / "src/main/resources/data/riftcompanions/structure_overrides/vanilla_baseline.json").read_text(encoding="utf-8"))
    allowed_profiles = {"village", "dungeon", "mineshaft", "ruined_portal", "stronghold", "unknown"}
    require(structure_overrides.get("schema_version") == 1 and len(structure_overrides.get("entries", [])) >= 5,
            "structure override baseline unexpectedly shrank")
    for entry in structure_overrides["entries"]:
        require({"block", "profile_id", "caution_bias"}.issubset(entry), "invalid structure override schema")
        require(entry["profile_id"] in allowed_profiles and 0 <= entry["caution_bias"] <= 30,
                "structure override must remain advisory and bounded")

    visual_state = ROOT / "src/main/java/com/riftcompanions/entity/CompanionVisualState.java"
    require(visual_state.exists(), "missing authoritative CompanionVisualState contract")
    visual_source = visual_state.read_text(encoding="utf-8")
    for name in ("IDLE_CALM", "GUARD", "DOWNED", "POWER_FOCUS", "POINT_ROUTE", "INTERACT_ANCHOR"):
        require(name in visual_source, "missing visual state " + name)
    animation_controller_source = (ROOT / "src/main/java/com/riftcompanions/animation/CompanionAnimationController.java").read_text(encoding="utf-8")
    companion_entity_source = (ROOT / "src/main/java/com/riftcompanions/entity/CompanionEntity.java").read_text(encoding="utf-8")
    require("applyFace" in animation_controller_source and "applySecondary" in animation_controller_source
            and "companion_face" in companion_entity_source and "companion_secondary" in companion_entity_source,
            "layered body/face/secondary GeckoLib controller contract is missing")
    gait_source_path = ROOT / "src/main/java/com/riftcompanions/presentation/CompanionGaitPresentationService.java"
    gait_source = gait_source_path.read_text(encoding="utf-8") if gait_source_path.exists() else ""
    look_intent_path = ROOT / "src/main/java/com/riftcompanions/entity/CompanionLookIntent.java"
    require(look_intent_path.exists() and "CompanionGaitPresentationService.tick" in companion_entity_source
            and "new LookAtPlayerGoal" not in companion_entity_source and "new RandomLookAroundGoal" not in companion_entity_source
            and "CHECK_BACK_LEFT" in gait_source and "stabilizeBodyToTravel" in gait_source
            and "face_check_back_left" in animation_controller_source,
            "natural body-heading/head-only glance contract is missing")

    resting_contract = ROOT / "src/main/java/com/riftcompanions/server/CompanionLifecycleService.java"
    require(resting_contract.exists() and "restAtBase" in resting_contract.read_text(encoding="utf-8"),
            "missing safe-base resting snapshot lifecycle contract")
    fault_contract = ROOT / "src/main/java/com/riftcompanions/safety/CompanionFaultService.java"
    require(fault_contract.exists(), "missing repeated companion fault circuit breaker")
    policy_contract = ROOT / "src/main/java/com/riftcompanions/policy/PlayerPolicyService.java"
    require(policy_contract.exists(), "missing per-owner policy service")
    navigation_contract = ROOT / "src/main/java/com/riftcompanions/navigation/CompanionNavigationService.java"
    require(navigation_contract.exists(), "missing bounded companion navigation safety layer")
    navigation_source = navigation_contract.read_text(encoding="utf-8")
    for phrase in ("pathIsSafe", "NAVIGATION_NO_PROGRESS", "FormationSlotReservationService", "NAVIGATION_MAX_SAFE_DROP"):
        require(phrase in navigation_source, "navigation contract unexpectedly missing " + phrase)
    safe_teleport_source = (ROOT / "src/main/java/com/riftcompanions/server/SafeTeleport.java").read_text(encoding="utf-8")
    recall_reservations = ROOT / "src/main/java/com/riftcompanions/server/RecallPlacementReservationService.java"
    lifecycle_source = resting_contract.read_text(encoding="utf-8")
    behavior_catalog = (ROOT / "src/main/java/com/riftcompanions/playtest/BehaviorTestCatalog.java").read_text(encoding="utf-8")
    require(recall_reservations.exists() and "hasLivingEntityClearance" in safe_teleport_source
            and "reserveCompanionSpawnSpot" in safe_teleport_source and "SAFE_RECALL_DESTINATION_REASSIGN" in safe_teleport_source,
            "Safe Recall anti-stacking placement/reservation contract is missing")
    require("reserveCompanionSpawnSpot" in lifecycle_source and "releaseCompanionReservation" in lifecycle_source
            and "safe_recall_distinct_destinations" in behavior_catalog,
            "lifecycle spawn/recall anti-stacking regression contract is missing")
    threat_response_path = ROOT / "src/main/java/com/riftcompanions/team/ThreatResponseDirector.java"
    threat_response_source = threat_response_path.read_text(encoding="utf-8") if threat_response_path.exists() else ""
    melee_goal_source = (ROOT / "src/main/java/com/riftcompanions/entity/ai/CompanionMeleeGoal.java").read_text(encoding="utf-8")
    require(threat_response_path.exists() and "ThreatResponseDirector.tick" in (ROOT / "src/main/java/com/riftcompanions/team/TeamDirector.java").read_text(encoding="utf-8")
            and "VISIBLE_HOSTILE_RESPONSE" in threat_response_source and "hostile_contact" in threat_response_source
            and "visible_zombie_response" in behavior_catalog and "getLookControl().setLookAt" not in melee_goal_source,
            "visible hostile anti-freeze/response contract is missing")
    context_director = ROOT / "src/main/java/com/riftcompanions/context/ContextualInteractionDirector.java"
    context_listener = ROOT / "src/main/java/com/riftcompanions/context/ContextInteractionReloadListener.java"
    context_director_source = context_director.read_text(encoding="utf-8") if context_director.exists() else ""
    require(context_director.exists() and context_listener.exists() and "ContextualInteractionDirector.tick" in (ROOT / "src/main/java/com/riftcompanions/team/TeamDirector.java").read_text(encoding="utf-8")
            and "CONTEXTUAL_INTERACTIONS" in context_director_source and "safeAmbientContext" in context_director_source
            and "observePlayerSelectedStructure" in context_director_source,
            "data-driven contextual interaction director contract is missing")
    for trigger in ("hostile_contact", "hostile_crowd"):
        for role in roles:
            require(any(entry["role"] == role and entry["trigger"] == trigger for entry in entries),
                    f"missing hostile response dialogue for {role}:{trigger}")
    save_versions = (ROOT / "src/main/java/com/riftcompanions/persistence/SaveVersions.java").read_text(encoding="utf-8")
    require("TEAM_DATA = 23" in save_versions and "BLACKBOARD_DATA = 19" in save_versions
            and "CONFIG_VERSION = 16" in save_versions,
            "contextual interaction/config version markers are missing")
    network_source = (ROOT / "src/main/java/com/riftcompanions/network/ModNetwork.java").read_text(encoding="utf-8")
    require('PROTOCOL = "33"' in network_source, "network protocol must include contextual interaction action vocabulary")
    will_control_budget = (ROOT / "src/main/java/com/riftcompanions/hive/control/WillControlBudget.java").read_text(encoding="utf-8")
    will_control_eligibility = (ROOT / "src/main/java/com/riftcompanions/hive/control/WillControlEligibility.java").read_text(encoding="utf-8")
    channel_source = (ROOT / "src/main/java/com/riftcompanions/hive/control/HiveChannelManager.java").read_text(encoding="utf-8")
    require("WILL_CONTROL_HEART_BUDGET" in will_control_budget and "effectiveHeartLoad" in will_control_budget
            and "WILL_CONTROL_OVERLOAD_DURATION_PERCENT" in will_control_budget,
            "Will control heart-budget model is incomplete")
    require("target instanceof Enemy" in will_control_eligibility and "PROTECTED_FROM_COMPANIONS" in will_control_eligibility
            and "isActiveThreat" in will_control_eligibility,
            "Will control hostile eligibility/anti-farm boundary is incomplete")
    require("WillControlBudget.select" in channel_source and "ModTags.HIVE_LINKED" not in channel_source,
            "Will control channel must use the heart budget rather than a Hive-only tag gate")
    entities_source = (ROOT / "src/main/java/com/riftcompanions/registry/ModEntities.java").read_text(encoding="utf-8")
    operation_dir = ROOT / "src/main/java/com/riftcompanions/operation"
    hostile_assets = list((ASSETS / "textures/entity").glob("hive/**/*.png")) + list((ASSETS / "geo").glob("hive_*.geo.json"))
    require("MobCategory.MONSTER" not in entities_source and "HIVE_" not in entities_source
            and not operation_dir.exists() and not hostile_assets,
            "custom hostile entity, operation, or hostile asset content must remain absent")
    guardian_brace = (ROOT / "src/main/java/com/riftcompanions/server/GuardianBraceService.java").read_text(encoding="utf-8")
    ability_source = (ROOT / "src/main/java/com/riftcompanions/server/AbilityService.java").read_text(encoding="utf-8")
    social_source = (ROOT / "src/main/java/com/riftcompanions/social/CompanionSocialDirector.java").read_text(encoding="utf-8")
    dialogue_source = (ROOT / "src/main/java/com/riftcompanions/dialogue/DialogueService.java").read_text(encoding="utf-8")
    require("GUARDIAN_BRACE" in guardian_brace and "SCOUT_SIGNAL" in ability_source,
            "natural-world Guardian Brace or Scout Signal capability is missing")
    require("speakPairedReply" in dialogue_source and "observePlayerInteraction" in social_source
            and "PENDING_REPLIES" in social_source and "BlockTags.BEDS" in social_source,
            "bounded Social Director interaction contract is incomplete")
    roster_source = (ROOT / "src/main/java/com/riftcompanions/server/TeamSavedData.java").read_text(encoding="utf-8")
    require("CompanionLifecycle.DOWNED" in roster_source and "activeEntityUuid" in roster_source,
            "downed companion UUIDs must remain addressable for rescue and duplicate prevention")
    config_source = (ROOT / "src/main/java/com/riftcompanions/config/CompanionConfig.java").read_text(encoding="utf-8")
    require("TUTORIAL_ENABLED" in config_source and "RIFT_OPERATIONS_ENABLED" not in config_source
            and "ENABLE_COMPANION_SOCIAL" in config_source
            and "CRITICAL_ONLY" in (ROOT / "src/main/java/com/riftcompanions/dialogue/ChatProfile.java").read_text(encoding="utf-8"),
            "social/tutorial or critical-alert accessibility contract is missing")
    journal_snapshot = (ROOT / "src/main/java/com/riftcompanions/network/TeamStatusSnapshot.java").read_text(encoding="utf-8")
    require("StoryJournalView" in journal_snapshot and "Undiscovered Chapter" in journal_snapshot
            and "clue.summary()" in journal_snapshot and "clue.id()" not in journal_snapshot,
            "bounded story journal must avoid hidden chapter titles and raw clue IDs")
    require("TeamSupplyView" in journal_snapshot and "withdrawnTodayReadOnly" in journal_snapshot
            and "supply.position()" not in journal_snapshot and "supply.dimension()" not in journal_snapshot,
            "Team Supply UI projection must not expose container coordinates")
    require("SocialView" in journal_snapshot and "socialView" in journal_snapshot
            and "DialogueLine" not in journal_snapshot.split("public record SocialView", 1)[1],
            "Social Director projection must remain bounded and exclude dialogue body")
    for contract in (
        ROOT / "src/main/java/com/riftcompanions/consequence/ConsequenceService.java",
        ROOT / "src/main/java/com/riftcompanions/perception/CompanionPerceptionService.java",
        ROOT / "src/main/java/com/riftcompanions/interrupt/ActionInterruptService.java",
        ROOT / "src/main/java/com/riftcompanions/spatial/SpatialEtiquetteService.java",
        ROOT / "src/main/java/com/riftcompanions/scene/SetPieceService.java",
        ROOT / "src/main/java/com/riftcompanions/social/CompanionSocialDirector.java",
        ROOT / "src/main/java/com/riftcompanions/resource/WorldFreePickupService.java",
        ROOT / "src/main/java/com/riftcompanions/resource/TeamSupplyService.java",
        ROOT / "src/main/java/com/riftcompanions/mental/MindAnchorService.java",
        ROOT / "src/main/java/com/riftcompanions/mental/MentalEffectReloadListener.java",
        ROOT / "src/main/java/com/riftcompanions/story/MysteryBoardState.java",
        ROOT / "src/main/java/com/riftcompanions/story/StoryPromiseState.java",
        ROOT / "src/main/java/com/riftcompanions/encounter/EncounterContextService.java",
        ROOT / "src/main/java/com/riftcompanions/encounter/EncounterDialogueReloadListener.java",
        ROOT / "src/main/java/com/riftcompanions/behavior/RoleBehaviorService.java",
        ROOT / "src/main/java/com/riftcompanions/behavior/BehaviorProfileReloadListener.java",
        ROOT / "src/main/java/com/riftcompanions/behavior/GiftedBehaviorService.java",
        ROOT / "src/main/java/com/riftcompanions/behavior/GuardianBehaviorService.java",
        ROOT / "src/main/java/com/riftcompanions/server/TeamSafetyControlService.java",
        ROOT / "src/main/java/com/riftcompanions/performance/CompanionPerformanceMonitor.java",
        ROOT / "src/main/java/com/riftcompanions/navigation/NavigationDebugSnapshot.java",
        ROOT / "src/main/java/com/riftcompanions/mode/GameModePolicyService.java",
        ROOT / "src/main/java/com/riftcompanions/mode/VanillaRulePolicyService.java",
        ROOT / "src/main/java/com/riftcompanions/onboarding/OnboardingService.java",
        ROOT / "src/main/java/com/riftcompanions/onboarding/OnboardingState.java",
        ROOT / "src/main/java/com/riftcompanions/animation/CompanionAnimationController.java",
        ROOT / "src/main/java/com/riftcompanions/animation/CompanionAnimationHandler.java",
        ROOT / "src/main/java/com/riftcompanions/animation/AnimationStateMapper.java",
        ROOT / "src/main/java/com/riftcompanions/animation/CompanionAnimationStateMapper.java",
        ROOT / "src/main/java/com/riftcompanions/animation/AnimationMarkerPlan.java",
        ROOT / "src/main/java/com/riftcompanions/decision/TeamDecisionService.java",
        ROOT / "src/main/java/com/riftcompanions/playtest/BehaviorTestCatalog.java",
        ROOT / "src/main/java/com/riftcompanions/server/CompanionPresentationSoundService.java",
    ):
        require(contract.exists(), "missing consequence/perception/scene contract: " + str(contract.relative_to(ROOT)))

    build_scaffold = (
        ROOT / "build.gradle",
        ROOT / "settings.gradle",
        ROOT / "gradle.properties",
        ROOT / "gradlew",
        ROOT / "gradlew.bat",
        ROOT / "gradle/wrapper/gradle-wrapper.jar",
        ROOT / "gradle/wrapper/gradle-wrapper.properties",
    )
    require(all(path.exists() for path in build_scaffold), "approved Forge build scaffold is incomplete")
    build_source = (ROOT / "build.gradle").read_text(encoding="utf-8")
    require("net.minecraftforge.gradle" in build_source and "geckolib-forge-" in build_source
            and "geckolib_version" in build_source,
            "ForgeGradle or GeckoLib build dependency is missing")

    # A reload listener receives its Gson instance in the superclass constructor.
    # Initializing its singleton before Gson would silently pass null and crash
    # the first integrated-server data reload (for example, world creation).
    reload_listeners = [
        path for path in (ROOT / "src/main/java").glob("**/*ReloadListener.java")
        if "extends SimpleJsonResourceReloadListener" in path.read_text(encoding="utf-8")
    ]
    require(reload_listeners, "expected SimpleJsonResourceReloadListener implementations")
    for listener in reload_listeners:
        listener_source = listener.read_text(encoding="utf-8")
        gson_match = re.search(r"private static final Gson GSON = new Gson\(\);", listener_source)
        instance_match = re.search(
            r"public static final [A-Za-z0-9_]+ INSTANCE = new [A-Za-z0-9_]+\(\);", listener_source)
        require(gson_match is not None and instance_match is not None,
                f"reload listener lacks explicit Gson/singleton initialization: {listener.relative_to(ROOT)}")
        require(gson_match.start() < instance_match.start() and "super(GSON," in listener_source,
                f"Gson must initialize before singleton construction: {listener.relative_to(ROOT)}")

    for java in (ROOT / "src/main/java").glob("**/*.java"):
        source = java.read_text(encoding="utf-8")
        stripped = re.sub(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"', "", source, flags=re.S)
        for opening, closing in (("(", ")"), ("{", "}"), ("[", "]")):
            require(stripped.count(opening) == stripped.count(closing), f"unbalanced {opening}{closing} in {java.relative_to(ROOT)}")

    print(f"PASS: {len(json_files)} JSON resources, {len(entries)} dialogue lines, {len(list((ROOT / 'src/main/java').glob('**/*.java')))} Java source files")
    audit_animations()
    print("PASS: 64px faceted-character texture/Geo/animation contracts, reliability/profile schemas, and approved Forge build scaffold")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except AssertionError as error:
        print(f"FAIL: {error}", file=sys.stderr)
        raise SystemExit(1)
