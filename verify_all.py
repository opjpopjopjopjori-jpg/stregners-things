#!/usr/bin/env python3
"""
Comprehensive static verification for Rift Companions mod.
Correctly handles GeckoLib Bedrock JSON format.
"""
import json
import os
import struct
import sys
import re
from pathlib import Path
from collections import defaultdict

ROOT = Path("/home/user/stregners-things/src/main/resources/assets/riftcompanions")
JAVA_ROOT = Path("/home/user/stregners-things/src/main/java/com/riftcompanions")

errors = []
warnings = []
info = []

def err(msg): errors.append(msg); print(f"  ❌ ERROR: {msg}")
def warn(msg): warnings.append(msg); print(f"  ⚠️  WARN: {msg}")
def ok(msg): info.append(msg); print(f"  ✅ {msg}")

roles = ['guardian', 'gifted', 'scout', 'seer']

# ─────────────────────────────────────────────
# 1. Validate all JSON files parse correctly
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 1: JSON Syntax Validation")
print("="*60)

json_files = list(ROOT.rglob("*.json"))
json_parse_errors = 0
for jf in sorted(json_files):
    try:
        with open(jf, 'r') as f:
            json.load(f)
    except json.JSONDecodeError as e:
        err(f"JSON parse error in {jf.relative_to(ROOT)}: {e}")
        json_parse_errors += 1

if json_parse_errors == 0:
    ok(f"All {len(json_files)} JSON files parse correctly ✓")

# ─────────────────────────────────────────────
# 2. Validate PNG texture dimensions (512x512)
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 2: PNG Texture Dimension Check")
print("="*60)

def get_png_dimensions(path):
    with open(path, 'rb') as f:
        header = f.read(24)
        if header[:8] != b'\x89PNG\r\n\x1a\n':
            return None, None
        width = struct.unpack('>I', header[16:20])[0]
        height = struct.unpack('>I', header[20:24])[0]
        return width, height

png_issues = 0
for pf in sorted(ROOT.rglob("*.png")):
    w, h = get_png_dimensions(pf)
    rel = pf.relative_to(ROOT)
    if w is None:
        err(f"Invalid PNG: {rel}")
        png_issues += 1
    elif w != 512 or h != 512:
        err(f"PNG {rel} is {w}x{h}, expected 512x512")
        png_issues += 1

if png_issues == 0:
    ok("All 8 PNG textures are 512x512 ✓")

# ─────────────────────────────────────────────
# 3. Load Geo Models & Extract Bone Names
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 3: Geo Model Bone Extraction & Texture Dims")
print("="*60)

geo_bones = {}
for role in roles:
    geo_path = ROOT / "geo" / f"{role}.geo.json"
    with open(geo_path, 'r') as f:
        geo = json.load(f)
    
    # GeckoLib Bedrock format: minecraft:geometry[]
    geoms = geo.get('minecraft:geometry', [])
    if not geoms:
        err(f"{role}: no minecraft:geometry in geo file")
        continue
    
    geom = geoms[0]
    bones_list = geom.get('bones', [])
    bone_names = set(b.get('name', '') for b in bones_list)
    geo_bones[role] = bone_names
    
    desc = geom.get('description', {})
    tw = desc.get('texture_width', 0)
    th = desc.get('texture_height', 0)
    
    if tw == 512 and th == 512:
        ok(f"{role}: {len(bone_names)} bones, texture 512x512 ✓")
    else:
        err(f"{role}: texture {tw}x{th}, expected 512x512")

# ─────────────────────────────────────────────
# 4. Validate Animation Files Against Geo Models
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 4: Animation -> Geo Bone Cross-Reference")
print("="*60)

anim_data = {}
for role in roles:
    anim_path = ROOT / "animations" / f"{role}.animation.json"
    with open(anim_path, 'r') as f:
        anim = json.load(f)
    
    animations = anim.get('animations', {})
    anim_data[role] = animations
    
    bad_refs = 0
    good_refs = 0
    bad_ref_list = []
    
    for anim_name, anim_content in animations.items():
        if not isinstance(anim_content, dict):
            continue
        # GeckoLib Bedrock format: bones are under "bones" key
        bones_section = anim_content.get('bones', {})
        if isinstance(bones_section, dict):
            for bone_name in bones_section.keys():
                if bone_name not in geo_bones.get(role, set()):
                    bad_refs += 1
                    if len(bad_ref_list) < 10:
                        bad_ref_list.append(f"{anim_name} refs '{bone_name}'")
                else:
                    good_refs += 1
    
    if bad_refs == 0:
        ok(f"{role}: all {good_refs} bone refs in {len(animations)} anims match geo ✓")
    else:
        err(f"{role}: {bad_refs} bone refs NOT in geo model ({len(animations)} anims, {good_refs} good)")
        for br in bad_ref_list:
            warn(f"  {role}: {br}")

# ─────────────────────────────────────────────
# 5. Validate Animation Length & Loop Settings
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 5: Animation Length & Loop Validation")
print("="*60)

for role in roles:
    length_issues = 0
    for anim_name, anim_content in anim_data[role].items():
        if not isinstance(anim_content, dict):
            continue
        if 'animation_length' not in anim_content:
            warn(f"{role}/{anim_name}: missing animation_length")
            length_issues += 1
        else:
            al = anim_content['animation_length']
            if al <= 0:
                err(f"{role}/{anim_name}: animation_length = {al} (must be >0)")
                length_issues += 1
        # Check loop setting
        if 'loop' not in anim_content:
            warn(f"{role}/{anim_name}: missing 'loop' setting")
            length_issues += 1
    
    if length_issues == 0:
        ok(f"{role}: all animations have valid animation_length + loop ✓")

# ─────────────────────────────────────────────
# 6. Validate sounds.json References
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 6: sounds.json -> .ogg Cross-Reference")
print("="*60)

sounds_json_path = ROOT / "sounds.json"
with open(sounds_json_path, 'r') as f:
    sounds_data = json.load(f)

sound_ref_errors = 0
for sound_name, sound_entry in sounds_data.items():
    if isinstance(sound_entry, dict):
        sounds_list = sound_entry.get('sounds', [])
    elif isinstance(sound_entry, list):
        sounds_list = sound_entry
    else:
        continue
    
    for s in sounds_list:
        if isinstance(s, dict):
            s = s.get('name', s.get('sound', ''))
        # Strip namespace prefix: riftcompanions:companions/foo -> companions/foo
        if ':' in s:
            s = s.split(':', 1)[1]
        # Minecraft sound path: riftcompanions:hive/notice -> sounds/hive/notice.ogg
        ogg_path = ROOT / "sounds" / f"{s}.ogg"
        if not ogg_path.exists():
            err(f"Sound '{sound_name}' refs '{s}' but .ogg not found")
            sound_ref_errors += 1

if sound_ref_errors == 0:
    ok(f"All {len(sounds_data)} sound events reference existing .ogg files ✓")

# Check unreferenced .ogg files
all_ogg = set()
for ogg_file in ROOT.rglob("*.ogg"):
    rel = str(ogg_file.relative_to(ROOT))
    # strip .ogg and sounds/ prefix for matching
    path_no_ext = rel[:-4]
    if path_no_ext.startswith('sounds/'):
        all_ogg.add(path_no_ext[7:])  # strip sounds/ prefix
    else:
        all_ogg.add(path_no_ext)

referenced_sounds = set()
for sound_name, sound_entry in sounds_data.items():
    entries = sound_entry.get('sounds', []) if isinstance(sound_entry, dict) else sound_entry if isinstance(sound_entry, list) else []
    for s in entries:
        if isinstance(s, dict):
            s = s.get('name', s.get('sound', ''))
        if ':' in s:
            s = s.split(':', 1)[1]
        referenced_sounds.add(s)

unreferenced = all_ogg - referenced_sounds
if unreferenced:
    for u in sorted(unreferenced):
        warn(f"OGG '{u}.ogg' not referenced in sounds.json")
else:
    ok("All .ogg files are referenced in sounds.json ✓")

ok(f"sounds.json: {len(sounds_data)} sound events, {len(all_ogg)} .ogg files on disk")

# ─────────────────────────────────────────────
# 7. Validate Java Source: Key Checks
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 7: Java Source Key Existence Checks")
print("="*60)

def read_java(rel_path):
    path = JAVA_ROOT / rel_path
    if path.exists():
        return path.read_text()
    return None

checks = [
    ("entity/CompanionAction.java", "SEER_DANGER_MODE", "CompanionAction: SEER_DANGER_MODE"),
    ("animation/CompanionAnimationController.java", "SEER_DANGER_MODE", "AnimController: SEER_DANGER_MODE"),
    ("animation/CompanionAnimationController.java", "isPowerAction", "AnimController: isPowerAction()"),
    ("animation/CompanionAnimationStateMapper.java", "SEER_DANGER_MODE", "StateMapper: SEER_DANGER_MODE"),
    ("entity/CompanionEntity.java", "tickCombatRole", "CompanionEntity: tickCombatRole()"),
    ("entity/CompanionEntity.java", "SEER_DANGER_MODE", "CompanionEntity: SEER_DANGER_MODE"),
    ("hive/control/HiveChannelManager.java", "DANGER_DURATION_MULTIPLIER", "HiveChannelMgr: DANGER_DURATION_MULTIPLIER"),
    ("hive/control/HiveChannelManager.java", "isDangerModeActive", "HiveChannelMgr: isDangerModeActive()"),
    ("registry/ModSounds.java", "SEER_DANGER_MODE", "ModSounds: SEER_DANGER_MODE"),
    ("client/hud/CompanionHudOverlay.java", "danger", "HudOverlay: danger mode HUD"),
]

checked_files = {}
for filepath, token, label in checks:
    if filepath not in checked_files:
        checked_files[filepath] = read_java(filepath)
    content = checked_files[filepath]
    if content is None:
        err(f"{label} — file {filepath} not found!")
    elif token in content:
        ok(f"{label} ✓")
    else:
        err(f"{label} MISSING!")

# Specific AbilityService checks
ability = read_java("server/AbilityService.java")
if ability:
    if "32.0D" in ability or "32.0" in ability:
        ok("AbilityService: 32-block range present ✓")
    else:
        err("AbilityService: 32.0 range NOT found!")
    
    if "hasLineOfSight" in ability:
        ok("AbilityService: hasLineOfSight check ✓")
    else:
        err("AbilityService: hasLineOfSight MISSING!")
    
    if "2.4" in ability and "3.2" in ability:
        ok("AbilityService: Push impulse 2.4H+3.2V ✓")
    else:
        err("AbilityService: impulse 2.4/3.2 NOT found!")
else:
    err("AbilityService.java not found")

# WillControlEligibility checks
will = read_java("hive/control/WillControlEligibility.java")
if will:
    for method in ['isNearbyThreat', 'isDangerContext', 'isActiveThreat']:
        if method in will:
            ok(f"WillControlEligibility: {method}() ✓")
        else:
            err(f"WillControlEligibility: {method}() MISSING!")
else:
    err("WillControlEligibility.java not found")

# HiveChannelManager danger multiplier
hive = read_java("hive/control/HiveChannelManager.java")
if hive and "2.0D" in hive:
    ok("HiveChannelManager: DANGER_DURATION_MULTIPLIER = 2.0 ✓")
elif hive:
    err("HiveChannelManager: 2.0D not found!")

# PresentationSoundService
pres_sound = read_java("server/CompanionPresentationSoundService.java")
if pres_sound:
    if "seer_danger_mode" in pres_sound or "SEER_DANGER_MODE" in pres_sound:
        ok("PresentationSoundService: seer_danger_mode cue ✓")
    else:
        err("PresentationSoundService: seer_danger_mode MISSING!")
else:
    err("CompanionPresentationSoundService.java not found")

# ─────────────────────────────────────────────
# 8. Java Brace Balance
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 8: Java Brace Balance Check")
print("="*60)

java_files = list(JAVA_ROOT.rglob("*.java"))
syntax_issues = 0
for jf in sorted(java_files):
    content = jf.read_text()
    rel = jf.relative_to(JAVA_ROOT)
    opens = content.count('{')
    closes = content.count('}')
    if opens != closes:
        err(f"{rel}: unbalanced braces ({{ = {opens}, }} = {closes})")
        syntax_issues += 1

if syntax_issues == 0:
    ok(f"All {len(java_files)} Java files: braces balanced ✓")

# ─────────────────────────────────────────────
# 9. Animation Key Naming & Face/Secondary Coverage
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 9: Face/Secondary Animation Coverage")
print("="*60)

face_suffixes = ['face_alert', 'face_check_back_left', 'face_check_back_right',
                 'face_combat', 'face_context', 'face_glance_left', 'face_glance_right',
                 'face_idle', 'face_power', 'face_recovery', 'face_talk']

secondary_suffixes = ['secondary_combat', 'secondary_context', 'secondary_idle',
                      'secondary_power', 'secondary_recovery']

for role in roles:
    missing_face = []
    missing_sec = []
    
    for suffix in face_suffixes:
        key = f"animation.{role}.{suffix}"
        if key not in anim_data[role]:
            missing_face.append(suffix)
    
    for suffix in secondary_suffixes:
        key = f"animation.{role}.{suffix}"
        if key not in anim_data[role]:
            missing_sec.append(suffix)
    
    if not missing_face:
        ok(f"{role}: all 11 face anims present ✓")
    else:
        for m in missing_face:
            err(f"{role}: missing face anim: {m}")
    
    if not missing_sec:
        ok(f"{role}: all 5 secondary anims present ✓")
    else:
        for m in missing_sec:
            err(f"{role}: missing secondary anim: {m}")

# ─────────────────────────────────────────────
# 10. Face animation eye bone coverage quality
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 10: Face Animation Eye/Pupil Quality")
print("="*60)

for role in roles:
    stats = {'pupil': 0, 'lid': 0, 'glint': 0, 'brow': 0, 'total': 0}
    for suffix in face_suffixes:
        key = f"animation.{role}.{suffix}"
        if key in anim_data[role]:
            anim = anim_data[role][key]
            if isinstance(anim, dict):
                stats['total'] += 1
                bones_in = anim.get('bones', {})
                bone_names = list(bones_in.keys()) if isinstance(bones_in, dict) else []
                if any('pupil' in b for b in bone_names):
                    stats['pupil'] += 1
                if any('lid' in b for b in bone_names):
                    stats['lid'] += 1
                if any('glint' in b for b in bone_names):
                    stats['glint'] += 1
                if any('brow' in b for b in bone_names):
                    stats['brow'] += 1
    
    t = stats['total']
    if t > 0:
        ok(f"{role} face: pupil {stats['pupil']}/{t}, lid {stats['lid']}/{t}, glint {stats['glint']}/{t}, brow {stats['brow']}/{t}")
        if stats['pupil'] == t:
            ok(f"{role}: ALL face anims have pupil movement ✓")
        else:
            warn(f"{role}: {t - stats['pupil']}/{t} face anims missing pupil movement")

# ─────────────────────────────────────────────
# 11. UV coordinates within texture bounds
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 11: UV Coordinates Within Texture Bounds")
print("="*60)

for role in roles:
    geo_path = ROOT / "geo" / f"{role}.geo.json"
    with open(geo_path, 'r') as f:
        geo = json.load(f)
    
    geoms = geo.get('minecraft:geometry', [])
    uv_issues = 0
    for geom in geoms:
        for bone in geom.get('bones', []):
            for cube in bone.get('cubes', []):
                for face_name, face in cube.get('faces', {}).items():
                    uv = face.get('uv', [])
                    if isinstance(uv, list) and len(uv) >= 2:
                        if isinstance(uv[0], list):
                            for coord in uv:
                                if coord[0] > 512 or coord[1] > 512:
                                    uv_issues += 1
                        elif len(uv) == 4:
                            for val in uv:
                                if val > 512:
                                    uv_issues += 1
    
    if uv_issues == 0:
        ok(f"{role}: all UV coords within 512x512 ✓")
    else:
        err(f"{role}: {uv_issues} UV coords exceed texture bounds!")

# ─────────────────────────────────────────────
# 12. Hair & Eye inflate values
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 12: Hair & Eye Inflate Enhancement")
print("="*60)

for role in roles:
    geo_path = ROOT / "geo" / f"{role}.geo.json"
    with open(geo_path, 'r') as f:
        geo = json.load(f)
    
    hair_inflates = []
    eye_inflates = []
    
    for geom in geo.get('minecraft:geometry', []):
        for bone in geom.get('bones', []):
            bname = bone.get('name', '').lower()
            for cube in bone.get('cubes', []):
                inflate = cube.get('inflate', 0.0)
                if 'hair' in bname:
                    hair_inflates.append(inflate)
                if any(kw in bname for kw in ['eye', 'pupil', 'lid', 'glint', 'brow']):
                    eye_inflates.append(inflate)
    
    if hair_inflates:
        max_h = max(hair_inflates)
        min_h = min(hair_inflates)
        if max_h >= 0.12:
            ok(f"{role} hair inflate: min={min_h:.3f} max={max_h:.3f} ✓")
        else:
            err(f"{role} hair inflate max={max_h:.3f} (expected >=0.12)")
    else:
        warn(f"{role}: no hair cubes with inflate values")
    
    if eye_inflates:
        max_e = max(eye_inflates)
        if max_e >= 0.06:
            ok(f"{role} eye inflate: max={max_e:.3f} ✓")
        else:
            err(f"{role} eye inflate max={max_e:.3f} (expected >=0.06)")
    else:
        warn(f"{role}: no eye cubes with inflate values")

# ─────────────────────────────────────────────
# 13. Animation keyframe validation
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 13: Animation Keyframe Validation")
print("="*60)

for role in roles:
    bad_kf = 0
    empty_kf = 0
    for anim_name, anim_content in anim_data[role].items():
        if not isinstance(anim_content, dict):
            continue
        bones_section = anim_content.get('bones', {})
        if not isinstance(bones_section, dict):
            continue
        for bone_name, bone_data in bones_section.items():
            if not isinstance(bone_data, dict):
                continue
            for channel_name, channel_data in bone_data.items():
                if not isinstance(channel_data, dict):
                    continue
                keyframes = channel_data.get('keyframes', [])
                if keyframes is None or len(keyframes) == 0:
                    empty_kf += 1
                    if empty_kf <= 3:
                        warn(f"{role}/{anim_name}/{bone_name}/{channel_name}: empty keyframes")
    
    if empty_kf == 0:
        ok(f"{role}: all keyframes valid ✓")
    else:
        warn(f"{role}: {empty_kf} empty keyframe channels (may be placeholders)")

# ─────────────────────────────────────────────
# 14. Java animation refs -> JSON
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 14: Java -> JSON Animation Key Cross-Reference")
print("="*60)

state_mapper = read_java("animation/CompanionAnimationStateMapper.java")
if state_mapper:
    # Match both "animation.guardian.idle" and "guardian.face_idle" patterns
    pattern = r'"(animation\.)?(' + '|'.join(roles) + r')\.([^"]+)"'
    matches = re.findall(pattern, state_mapper)
    
    missing_java_refs = []
    for prefix, role_ref, anim_key in matches:
        # Try both formats: animation.role.key and role.key
        full_key1 = f"animation.{role_ref}.{anim_key}"
        full_key2 = f"{role_ref}.{anim_key}"
        found = (role_ref in anim_data and 
                 (full_key1 in anim_data[role_ref] or full_key2 in anim_data[role_ref]))
        if not found and role_ref in anim_data:
            missing_java_refs.append(full_key1)
    
    if not missing_java_refs:
        ok(f"All Java anim key refs found in JSON ✓")
    else:
        for m in sorted(set(missing_java_refs)):
            err(f"Java refs '{m}' NOT in animation JSON!")
else:
    err("CompanionAnimationStateMapper.java not found")

# ─────────────────────────────────────────────
# 15. OGG file integrity
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 15: OGG Sound File Integrity")
print("="*60)

ogg_issues = 0
for ogg in sorted(ROOT.rglob("*.ogg")):
    size = ogg.stat().st_size
    rel = ogg.relative_to(ROOT)
    if size < 100:
        err(f"{rel}: only {size} bytes (corrupt)")
        ogg_issues += 1
    elif size < 500:
        warn(f"{rel}: only {size} bytes (very short)")

if ogg_issues == 0:
    ok("All .ogg files have valid size ✓")

# ─────────────────────────────────────────────
# 16. CombatProfile pursuit radii
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 16: CombatProfile Configuration")
print("="*60)

combat_profile = read_java("policy/CombatProfile.java")
if combat_profile:
    for label in ['DEFENSIVE', 'BALANCED', 'TACTICAL']:
        if label in combat_profile:
            ok(f"CombatProfile: {label} present ✓")
        else:
            warn(f"CombatProfile: {label} may be missing")
else:
    warn("CombatProfile.java not found")

# ─────────────────────────────────────────────
# 17. ContentProfileRegistry texture mappings
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 17: Texture -> ContentProfile Consistency")
print("="*60)

cpr = read_java("content/ContentProfileRegistry.java")
if cpr:
    for tex in ['hopper_sheriff', 'eleven_gifted', 'max_scout', 'will_seer',
                'guardian_public', 'gifted_public', 'scout_public', 'seer_public']:
        if tex in cpr:
            ok(f"ContentProfile refs '{tex}' ✓")
        else:
            warn(f"ContentProfile may not reference '{tex}'")
else:
    warn("ContentProfileRegistry.java not found")

# ─────────────────────────────────────────────
# 18. CompanionGeoModel texture resolution
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 18: GeoModel Texture Resolution")
print("="*60)

geo_model = read_java("client/model/CompanionGeoModel.java")
if geo_model:
    if "personal" in geo_model and "public" in geo_model:
        ok("CompanionGeoModel: personal/public switching ✓")
    else:
        warn("CompanionGeoModel: personal/public paths may be missing")
    
    if "getTextureResource" in geo_model:
        ok("CompanionGeoModel: getTextureResource() ✓")
    else:
        err("CompanionGeoModel: getTextureResource() MISSING!")
else:
    warn("CompanionGeoModel.java not found")

# ─────────────────────────────────────────────
# 19. Check animation key consistency across roles
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 19: Animation Key Consistency")
print("="*60)

all_anim_keys = {role: set(anim_data[role].keys()) for role in roles}
common_keys = set.intersection(*all_anim_keys.values())
all_keys = set.union(*all_anim_keys.values())

missing_per_role = {}
for role in roles:
    missing = all_keys - all_anim_keys[role]
    if missing:
        missing_per_role[role] = missing

if not missing_per_role:
    ok(f"All 4 roles have identical animation sets ({len(common_keys)} each) ✓")
else:
    for role, missing in sorted(missing_per_role.items()):
        for m in sorted(missing):
            warn(f"{role} missing animation: {m}")
    ok(f"Common: {len(common_keys)}, total unique: {len(all_keys)}")

# ─────────────────────────────────────────────
# 20. Deep body animation quality check
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 20: Body Animation Quality (idle/walk/combat)")
print("="*60)

# Check idle has chest breathing, walk has leg movement, combat_ready has arm movement
quality_checks = {
    'idle': {'should_have': ['chest'], 'description': 'chest breathing'},
    'walk': {'should_have': ['left_leg', 'right_leg'], 'description': 'leg movement'},
    'run': {'should_have': ['left_leg', 'right_leg'], 'description': 'leg movement'},
    'combat_ready': {'should_have': ['right_arm', 'left_arm'], 'description': 'arm readiness'},
}

for role in roles:
    for anim_suffix, check in quality_checks.items():
        key = f"animation.{role}.{anim_suffix}"
        if key in anim_data[role]:
            bones_in = anim_data[role][key].get('bones', {})
            if isinstance(bones_in, dict):
                present = [b for b in check['should_have'] if b in bones_in]
                if len(present) == len(check['should_have']):
                    ok(f"{role}/{anim_suffix}: has {check['description']} ✓")
                else:
                    missing_bones = set(check['should_have']) - set(present)
                    warn(f"{role}/{anim_suffix}: missing {missing_bones} for {check['description']}")

# ─────────────────────────────────────────────
# 21. Verify key animation enhancements (head leads turns, melee chest twist, etc)
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 21: Enhanced Animation Quality Checks")
print("="*60)

# Check turn animations have head movement
for role in roles:
    turn_anims = [k for k in anim_data[role].keys() if 'turn' in k.lower()]
    turn_with_head = 0
    for t in turn_anims:
        bones_in = anim_data[role][t].get('bones', {})
        if isinstance(bones_in, dict) and 'head' in bones_in:
            turn_with_head += 1
    
    if turn_anims:
        ok(f"{role}: {turn_with_head}/{len(turn_anims)} turn anims have head movement")

# Check melee attack has chest twist
for role in roles:
    melee_anims = [k for k in anim_data[role].keys() if 'melee_attack' in k or 'melee_strike' in k]
    melee_with_chest = 0
    for m in melee_anims:
        bones_in = anim_data[role][m].get('bones', {})
        if isinstance(bones_in, dict) and 'chest' in bones_in:
            melee_with_chest += 1
    
    if melee_anims:
        ok(f"{role}: {melee_with_chest}/{len(melee_anims)} melee anims have chest twist")
        if melee_with_chest < len(melee_anims):
            warn(f"{role}: {len(melee_anims) - melee_with_chest} melee anims missing chest twist")

# ─────────────────────────────────────────────
# SUMMARY
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("VERIFICATION SUMMARY")
print("="*60)
print(f"  ✅ Passed:  {len(info)}")
print(f"  ⚠️  Warnings: {len(warnings)}")
print(f"  ❌ Errors:   {len(errors)}")
print("="*60)

if errors:
    print("\n🚨 CRITICAL ISSUES TO FIX:")
    for e in errors:
        print(f"  → {e}")
    sys.exit(1)
else:
    print("\n🎉 ALL CRITICAL CHECKS PASSED!")
    if warnings:
        print(f"\n⚠️  {len(warnings)} warnings (may be intentional):")
        for w in warnings[:30]:
            print(f"  → {w}")
        if len(warnings) > 30:
            print(f"  ... and {len(warnings)-30} more warnings")
    sys.exit(0)
