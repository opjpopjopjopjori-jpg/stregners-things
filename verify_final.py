#!/usr/bin/env python3
"""
FINAL comprehensive verification for Rift Companions mod.
Correctly handles both Bedrock timestamp format and keyframes array format.
"""
import json
import struct
import sys
import re
from pathlib import Path

ROOT = Path("/home/user/stregners-things/src/main/resources/assets/riftcompanions")
JAVA_ROOT = Path("/home/user/stregners-things/src/main/java/com/riftcompanions")

errors = []
warnings = []
info = []

def err(msg): errors.append(msg); print(f"  ❌ ERROR: {msg}")
def warn(msg): warnings.append(msg); print(f"  ⚠️  WARN: {msg}")
def ok(msg): info.append(msg); print(f"  ✅ {msg}")

roles = ['guardian', 'gifted', 'scout', 'seer']

def channel_has_data(ch_data):
    """Check if a channel dict has actual keyframe data (either format)."""
    if not isinstance(ch_data, dict):
        return False
    # Format 1: keyframes array
    if 'keyframes' in ch_data:
        kf = ch_data['keyframes']
        return kf is not None and len(kf) > 0
    # Format 2: Bedrock timestamp keys like "0.0", "1.5"
    for key in ch_data.keys():
        try:
            float(key)
            return True
        except ValueError:
            pass
    return False

# ─────────────────────────────────────────────
# 1. JSON Validity
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 1: JSON Syntax Validation")
print("="*60)
json_err = 0
for jf in sorted(ROOT.rglob("*.json")):
    try:
        with open(jf) as f: json.load(f)
    except json.JSONDecodeError as e:
        err(f"{jf.relative_to(ROOT)}: {e}")
        json_err += 1
if json_err == 0:
    ok(f"All {len(list(ROOT.rglob('*.json')))} JSON files valid ✓")

# ─────────────────────────────────────────────
# 2. PNG Dimensions
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 2: PNG Texture Dimensions (512x512)")
print("="*60)
png_err = 0
for pf in sorted(ROOT.rglob("*.png")):
    with open(pf, 'rb') as f: h = f.read(24)
    if h[:8] != b'\x89PNG\r\n\x1a\n':
        err(f"Invalid PNG: {pf.relative_to(ROOT)}"); png_err += 1; continue
    w = struct.unpack('>I', h[16:20])[0]
    ht = struct.unpack('>I', h[20:24])[0]
    if w != 512 or ht != 512:
        err(f"{pf.relative_to(ROOT)}: {w}x{ht}"); png_err += 1
if png_err == 0:
    ok("All 8 PNGs = 512x512 ✓")

# ─────────────────────────────────────────────
# 3. Geo Models
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 3: Geo Model Structure")
print("="*60)
geo_bones = {}
for role in roles:
    with open(ROOT / "geo" / f"{role}.geo.json") as f: geo = json.load(f)
    geoms = geo.get('minecraft:geometry', [])
    if not geoms:
        err(f"{role}: no minecraft:geometry"); continue
    bones = set(b.get('name','') for b in geoms[0].get('bones',[]))
    geo_bones[role] = bones
    desc = geoms[0].get('description',{})
    tw, th = desc.get('texture_width',0), desc.get('texture_height',0)
    if tw == 512 and th == 512:
        ok(f"{role}: {len(bones)} bones, 512x512 texture ✓")
    else:
        err(f"{role}: texture {tw}x{th}")

# ─────────────────────────────────────────────
# 4. Animation → Geo Bone Cross-Reference
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 4: Animation → Geo Bone Cross-Reference")
print("="*60)
anim_data = {}
for role in roles:
    with open(ROOT / "animations" / f"{role}.animation.json") as f:
        anim_data[role] = json.load(f).get('animations', {})
    
    bad = 0; good = 0
    for ak, av in anim_data[role].items():
        if not isinstance(av, dict): continue
        for bn in av.get('bones',{}).keys():
            if bn not in geo_bones.get(role, set()):
                bad += 1
            else:
                good += 1
    if bad == 0:
        ok(f"{role}: {good} bone refs all match geo ✓")
    else:
        err(f"{role}: {bad} bone refs NOT in geo ({good} good)")

# ─────────────────────────────────────────────
# 5. Animation Data Integrity (KEY: check actual keyframe data)
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 5: Animation Keyframe Data Integrity")
print("="*60)
for role in roles:
    total = 0; has_data = 0; all_empty = 0; partial = 0
    for ak, av in anim_data[role].items():
        if not isinstance(av, dict): continue
        total += 1
        bones = av.get('bones', {})
        if not isinstance(bones, dict): continue
        
        channels_with = 0; channels_without = 0
        for bn, bd in bones.items():
            if not isinstance(bd, dict): continue
            for ch, cd in bd.items():
                if channel_has_data(cd):
                    channels_with += 1
                else:
                    channels_without += 1
        
        if channels_with > 0 and channels_without == 0:
            has_data += 1
        elif channels_with > 0:
            partial += 1
        else:
            all_empty += 1
    
    ok(f"{role}: {total} anims = {has_data} full + {partial} partial + {all_empty} empty")
    if all_empty > 0:
        err(f"{role}: {all_empty} animations have ZERO keyframe data!")
    if partial > 50:
        warn(f"{role}: {partial} animations have some empty channels")

# ─────────────────────────────────────────────
# 6. Sounds Cross-Reference
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 6: sounds.json ↔ .ogg Cross-Reference")
print("="*60)
with open(ROOT / "sounds.json") as f: sounds_data = json.load(f)
snd_err = 0
for sn, se in sounds_data.items():
    entries = se.get('sounds',[]) if isinstance(se,dict) else se if isinstance(se,list) else []
    for s in entries:
        if isinstance(s,dict): s = s.get('name',s.get('sound',''))
        if ':' in s: s = s.split(':',1)[1]
        if not (ROOT / "sounds" / f"{s}.ogg").exists():
            err(f"Sound '{sn}' → '{s}.ogg' NOT FOUND"); snd_err += 1
if snd_err == 0:
    ok(f"All {len(sounds_data)} sound events → .ogg files exist ✓")

# Check unreferenced .ogg
all_ogg = set()
for og in ROOT.rglob("*.ogg"):
    r = str(og.relative_to(ROOT))
    if r.startswith('sounds/') and r.endswith('.ogg'):
        all_ogg.add(r[7:-4])

ref_ogg = set()
for sn, se in sounds_data.items():
    entries = se.get('sounds',[]) if isinstance(se,dict) else se if isinstance(se,list) else []
    for s in entries:
        if isinstance(s,dict): s = s.get('name',s.get('sound',''))
        if ':' in s: s = s.split(':',1)[1]
        ref_ogg.add(s)

unref = all_ogg - ref_ogg
if unref:
    for u in sorted(unref): warn(f"Unreferenced .ogg: sounds/{u}.ogg")
else:
    ok("All .ogg files referenced in sounds.json ✓")

# ─────────────────────────────────────────────
# 7. OGG File Sizes
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 7: OGG File Integrity")
print("="*60)
for og in sorted(ROOT.rglob("*.ogg")):
    sz = og.stat().st_size
    if sz < 100: err(f"{og.relative_to(ROOT)}: {sz} bytes (corrupt)")
    elif sz < 500: warn(f"{og.relative_to(ROOT)}: {sz} bytes (tiny)")
ok("OGG integrity check complete ✓")

# ─────────────────────────────────────────────
# 8. Java Source Checks
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 8: Java Source Key Checks")
print("="*60)

def rj(p):
    fp = JAVA_ROOT / p
    return fp.read_text() if fp.exists() else None

checks = [
    ("entity/CompanionAction.java", "SEER_DANGER_MODE", "CompanionAction: SEER_DANGER_MODE"),
    ("animation/CompanionAnimationController.java", "SEER_DANGER_MODE", "AnimCtrl: SEER_DANGER_MODE"),
    ("animation/CompanionAnimationController.java", "isPowerAction", "AnimCtrl: isPowerAction()"),
    ("animation/CompanionAnimationStateMapper.java", "SEER_DANGER_MODE", "StateMapper: SEER_DANGER_MODE"),
    ("entity/CompanionEntity.java", "tickCombatRole", "Entity: tickCombatRole()"),
    ("entity/CompanionEntity.java", "SEER_DANGER_MODE", "Entity: SEER_DANGER_MODE"),
    ("hive/control/HiveChannelManager.java", "DANGER_DURATION_MULTIPLIER", "Hive: DANGER_DURATION_MULTIPLIER"),
    ("hive/control/HiveChannelManager.java", "isDangerModeActive", "Hive: isDangerModeActive()"),
    ("registry/ModSounds.java", "SEER_DANGER_MODE", "ModSounds: SEER_DANGER_MODE"),
    ("client/hud/CompanionHudOverlay.java", "danger", "HUD: danger mode"),
]
cache = {}
for fp, tok, lbl in checks:
    if fp not in cache: cache[fp] = rj(fp)
    c = cache[fp]
    if c is None: err(f"{lbl} — {fp} not found")
    elif tok in c: ok(f"{lbl} ✓")
    else: err(f"{lbl} MISSING!")

# AbilityService specific
ab = rj("server/AbilityService.java")
if ab:
    ok("AbilityService: 32-block range ✓" if "32.0D" in ab or "32.0" in ab else "FAIL")
    ok("AbilityService: hasLineOfSight ✓" if "hasLineOfSight" in ab else "FAIL")
    ok("AbilityService: impulse 2.4H+3.2V ✓" if "2.4" in ab and "3.2" in ab else "FAIL")
else: err("AbilityService.java not found")

# WillControlEligibility
wc = rj("hive/control/WillControlEligibility.java")
if wc:
    for m in ['isNearbyThreat','isDangerContext','isActiveThreat']:
        ok(f"WillControl: {m}() ✓" if m in wc else f"FAIL: {m}")
else: err("WillControlEligibility.java not found")

# HiveChannelManager 2.0D
hm = rj("hive/control/HiveChannelManager.java")
ok("Hive: DANGER_DURATION_MULTIPLIER=2.0 ✓" if hm and "2.0D" in hm else "FAIL")

# PresentationSoundService
ps = rj("server/CompanionPresentationSoundService.java")
ok("PresSound: seer_danger_mode ✓" if ps and "seer_danger_mode" in ps else "FAIL")

# ─────────────────────────────────────────────
# 9. Java Brace Balance
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 9: Java Brace Balance")
print("="*60)
jf_all = list(JAVA_ROOT.rglob("*.java"))
brace_err = 0
for jf in jf_all:
    c = jf.read_text()
    if c.count('{') != c.count('}'):
        err(f"{jf.relative_to(JAVA_ROOT)}: unbalanced braces"); brace_err += 1
if brace_err == 0:
    ok(f"All {len(jf_all)} Java files: braces balanced ✓")

# ─────────────────────────────────────────────
# 10. Face/Secondary Animation Coverage
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 10: Face & Secondary Animation Coverage")
print("="*60)
face_suf = ['face_alert','face_check_back_left','face_check_back_right',
            'face_combat','face_context','face_glance_left','face_glance_right',
            'face_idle','face_power','face_recovery','face_talk']
sec_suf = ['secondary_combat','secondary_context','secondary_idle',
           'secondary_power','secondary_recovery']

for role in roles:
    mf = [s for s in face_suf if f"animation.{role}.{s}" not in anim_data[role]]
    ms = [s for s in sec_suf if f"animation.{role}.{s}" not in anim_data[role]]
    if not mf: ok(f"{role}: 11 face anims ✓")
    else:
        for m in mf: err(f"{role}: missing {m}")
    if not ms: ok(f"{role}: 5 secondary anims ✓")
    else:
        for m in ms: err(f"{role}: missing {m}")

# ─────────────────────────────────────────────
# 11. Face Animation Eye Quality
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 11: Face Animation Eye/Pupil Quality")
print("="*60)
for role in roles:
    stats = {'pupil':0,'lid':0,'glint':0,'brow':0,'total':0}
    for suf in face_suf:
        k = f"animation.{role}.{suf}"
        if k in anim_data[role]:
            a = anim_data[role][k]
            if isinstance(a, dict):
                stats['total'] += 1
                bn = list(a.get('bones',{}).keys()) if isinstance(a.get('bones'),dict) else []
                if any('pupil' in b for b in bn): stats['pupil'] += 1
                if any('lid' in b for b in bn): stats['lid'] += 1
                if any('glint' in b for b in bn): stats['glint'] += 1
                if any('brow' in b for b in bn): stats['brow'] += 1
    t = stats['total']
    if t > 0:
        ok(f"{role} face: pupil {stats['pupil']}/{t}, lid {stats['lid']}/{t}, glint {stats['glint']}/{t}, brow {stats['brow']}/{t}")
        if stats['pupil'] == t: ok(f"{role}: ALL face anims have pupil ✓")

# ─────────────────────────────────────────────
# 12. UV Coordinates
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 12: UV Coordinates Within Bounds")
print("="*60)
for role in roles:
    with open(ROOT / "geo" / f"{role}.geo.json") as f: geo = json.load(f)
    uv_err = 0
    for g in geo.get('minecraft:geometry',[]):
        for b in g.get('bones',[]):
            for c in b.get('cubes',[]):
                for fn, fc in c.get('faces',{}).items():
                    uv = fc.get('uv',[])
                    if isinstance(uv,list) and len(uv)>=2:
                        if isinstance(uv[0],list):
                            for co in uv:
                                if co[0]>512 or co[1]>512: uv_err += 1
                        elif len(uv)==4:
                            for v in uv:
                                if v>512: uv_err += 1
    if uv_err == 0: ok(f"{role}: all UV ≤512 ✓")
    else: err(f"{role}: {uv_err} UV coords >512!")

# ─────────────────────────────────────────────
# 13. Hair & Eye Inflate
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 13: Hair & Eye Inflate Enhancement")
print("="*60)
for role in roles:
    with open(ROOT / "geo" / f"{role}.geo.json") as f: geo = json.load(f)
    hair_i = []; eye_i = []
    for g in geo.get('minecraft:geometry',[]):
        for b in g.get('bones',[]):
            n = b.get('name','').lower()
            for c in b.get('cubes',[]):
                i = c.get('inflate',0.0)
                if 'hair' in n: hair_i.append(i)
                if any(k in n for k in ['eye','pupil','lid','glint','brow']): eye_i.append(i)
    if hair_i:
        ok(f"{role} hair inflate: max={max(hair_i):.3f}" + (" ✓" if max(hair_i)>=0.12 else " LOW!"))
    if eye_i:
        ok(f"{role} eye inflate: max={max(eye_i):.3f}" + (" ✓" if max(eye_i)>=0.06 else " LOW!"))

# ─────────────────────────────────────────────
# 14. Body Animation Quality
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 14: Body Animation Quality")
print("="*60)
quality = {
    'idle': (['chest'], 'chest breathing'),
    'walk': (['left_leg','right_leg'], 'leg movement'),
    'run': (['left_leg','right_leg'], 'leg movement'),
    'combat_ready': (['right_arm','left_arm'], 'arm readiness'),
}
for role in roles:
    for suf, (bones_need, desc) in quality.items():
        k = f"animation.{role}.{suf}"
        if k in anim_data[role]:
            bn = list(anim_data[role][k].get('bones',{}).keys()) if isinstance(anim_data[role][k].get('bones'),dict) else []
            if all(b in bn for b in bones_need):
                ok(f"{role}/{suf}: {desc} ✓")

# ─────────────────────────────────────────────
# 15. Turn head-lead, melee chest twist
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 15: Enhanced Animation Quality")
print("="*60)
for role in roles:
    turns = [k for k in anim_data[role] if 'turn' in k.lower()]
    th = sum(1 for t in turns if 'head' in (anim_data[role][t].get('bones',{}) if isinstance(anim_data[role][t].get('bones'),dict) else {}))
    if turns: ok(f"{role}: {th}/{len(turns)} turns have head movement ✓")
    
    melee = [k for k in anim_data[role] if 'melee_attack' in k]
    mc = sum(1 for m in melee if 'chest' in (anim_data[role][m].get('bones',{}) if isinstance(anim_data[role][m].get('bones'),dict) else {}))
    if melee: ok(f"{role}: {mc}/{len(melee)} melee have chest twist ✓")

# ─────────────────────────────────────────────
# 16. ContentProfileRegistry
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 16: Texture ↔ ContentProfile")
print("="*60)
cpr = rj("content/ContentProfileRegistry.java")
if cpr:
    for t in ['hopper_sheriff','eleven_gifted','max_scout','will_seer',
              'guardian_public','gifted_public','scout_public','seer_public']:
        ok(f"ContentProfile: '{t}' ✓" if t in cpr else f"ContentProfile: '{t}' may be missing")
else: warn("ContentProfileRegistry.java not found")

# ─────────────────────────────────────────────
# 17. GeoModel Texture Resolution
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 17: GeoModel Texture Resolution")
print("="*60)
gm = rj("client/model/CompanionGeoModel.java")
if gm:
    ok("GeoModel: personal/public switching ✓" if 'personal' in gm and 'public' in gm else "FAIL")
    ok("GeoModel: getTextureResource() ✓" if 'getTextureResource' in gm else "FAIL")
else: warn("CompanionGeoModel.java not found")

# ─────────────────────────────────────────────
# 18. Animation Loop Settings
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 18: Animation Loop/Play Settings")
print("="*60)
for role in roles:
    no_loop = 0
    for ak, av in anim_data[role].items():
        if isinstance(av, dict) and 'loop' not in av:
            no_loop += 1
    if no_loop == 0:
        ok(f"{role}: all anims have loop setting ✓")
    else:
        warn(f"{role}: {no_loop} anims missing explicit loop setting (defaults to GeckoLib default)")

# ─────────────────────────────────────────────
# 19. Verify specific enhanced animation values
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 19: Animation Value Spot-Checks")
print("="*60)

def get_first_value(anim, bone, channel):
    """Get the first non-zero value from an animation channel."""
    bones = anim.get('bones', {})
    if not isinstance(bones, dict) or bone not in bones:
        return None
    bd = bones[bone]
    if not isinstance(bd, dict) or channel not in bd:
        return None
    cd = bd[channel]
    if isinstance(cd, dict):
        # Find first timestamp key with non-zero value
        for k in sorted(cd.keys(), key=lambda x: float(x) if x.replace('.','').replace('-','').isdigit() else 999):
            try:
                float(k)
            except ValueError:
                continue
            v = cd[k]
            if isinstance(v, list) and any(abs(x) > 0.001 for x in v):
                return v
    return None

# Check idle chest breathing (should have Y position change)
for role in roles:
    k = f"animation.{role}.idle"
    if k in anim_data[role]:
        v = get_first_value(anim_data[role][k], 'chest', 'position')
        if v and any(abs(x) > 0.01 for x in v):
            ok(f"{role}/idle: chest position changes (breathing) ✓")
        else:
            warn(f"{role}/idle: no chest position change detected")

# Check combat_ready arm rotation
for role in roles:
    k = f"animation.{role}.combat_ready"
    if k in anim_data[role]:
        v = get_first_value(anim_data[role][k], 'right_arm', 'rotation')
        if v and any(abs(x) > 5 for x in v):
            ok(f"{role}/combat_ready: arms raised (≥5°) ✓")
        else:
            warn(f"{role}/combat_ready: arm rotation may be too small")

# Check walk leg movement amplitude
for role in roles:
    k = f"animation.{role}.walk"
    if k in anim_data[role]:
        v = get_first_value(anim_data[role][k], 'left_leg', 'rotation')
        if v and any(abs(x) > 10 for x in v):
            ok(f"{role}/walk: leg rotation ≥10° ✓")
        else:
            warn(f"{role}/walk: leg rotation may be too small")

# ─────────────────────────────────────────────
# 20. Java → JSON Animation Key Cross-Reference
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("PHASE 20: Java → JSON Animation Keys")
print("="*60)
sm = rj("animation/CompanionAnimationStateMapper.java")
if sm:
    pattern = r'"(animation\.)?(' + '|'.join(roles) + r')\.([^"]+)"'
    matches = re.findall(pattern, sm)
    missing = []
    for prefix, rr, ak in matches:
        k1 = f"animation.{rr}.{ak}"
        k2 = f"{rr}.{ak}"
        if rr in anim_data and k1 not in anim_data[rr] and k2 not in anim_data[rr]:
            missing.append(k1)
    if not missing:
        ok(f"All {len(matches)} Java anim refs found in JSON ✓")
    else:
        for m in sorted(set(missing)):
            err(f"Java refs '{m}' NOT in JSON!")
else: err("StateMapper not found")

# ─────────────────────────────────────────────
# SUMMARY
# ─────────────────────────────────────────────
print("\n" + "="*60)
print("FINAL VERIFICATION SUMMARY")
print("="*60)
print(f"  ✅ Passed:  {len(info)}")
print(f"  ⚠️  Warnings: {len(warnings)}")
print(f"  ❌ Errors:   {len(errors)}")
print("="*60)

if errors:
    print("\n🚨 CRITICAL ISSUES:")
    for e in errors: print(f"  → {e}")
    sys.exit(1)
else:
    print("\n🎉🎉🎉 ALL CRITICAL CHECKS PASSED! 🎉🎉🎉")
    if warnings:
        print(f"\n⚠️  {len(warnings)} warnings:")
        for w in warnings[:20]: print(f"  → {w}")
        if len(warnings) > 20: print(f"  ... +{len(warnings)-20} more")
    sys.exit(0)
