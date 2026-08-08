#!/usr/bin/env python3
"""
Fix all 43 WEAK animations to make them impressive from a player's perspective.
"""
import json
from pathlib import Path

ROOT = Path('src/main/resources/assets/riftcompanions/animations')
fixes = []

def get_ch(anim, bone, channel):
    return anim.get('bones', {}).get(bone, {}).get(channel, {})

def set_ch(anim, bone, channel, data):
    if 'bones' not in anim:
        anim['bones'] = {}
    if bone not in anim['bones']:
        anim['bones'][bone] = {}
    anim['bones'][bone][channel] = data

def scale_ch(anim, bone, channel, factor):
    ch = get_ch(anim, bone, channel)
    for ts in ch:
        if isinstance(ch[ts], list):
            ch[ts] = [v * factor for v in ch[ts]]

def scale_all_bones(anim, factor):
    for bone_name in anim.get('bones', {}):
        for channel in ['rotation', 'position']:
            scale_ch(anim, bone_name, channel, factor)

for role in ['guardian', 'gifted', 'scout', 'seer']:
    path = ROOT / f'{role}.animation.json'
    with open(path) as f:
        data = json.load(f)
    anims = data['animations']

    # === FIX 1: idle — breathing too subtle ===
    key = f'animation.{role}.idle'
    if key in anims:
        a = anims[key]
        scale_ch(a, 'chest', 'position', 2.25)   # 0.08 → 0.18
        scale_ch(a, 'head', 'rotation', 2.5)      # 1° → 2.5°
        scale_ch(a, 'body', 'rotation', 2.0)      # body sway 1.5° → 3°
        scale_ch(a, 'left_arm', 'rotation', 1.8)
        scale_ch(a, 'right_arm', 'rotation', 1.8)
        # Add subtle body position bob for breathing
        if not get_ch(a, 'body', 'position'):
            set_ch(a, 'body', 'position', {"0.0": [0,0,0], "1.5": [0,-0.12,0], "3.0": [0,0,0]})
        fixes.append(f'{role}/idle: breathing 2.25x, head 2.5x, body 2x, arm 1.8x')

    # === FIX 2: idle_alert — barely visible ===
    key = f'animation.{role}.idle_alert'
    if key in anims:
        a = anims[key]
        scale_ch(a, 'chest', 'rotation', 2.0)
        set_ch(a, 'head', 'rotation', {"0.0":[0,0,0],"0.15":[0,8,0],"0.3":[0,-6,0],"0.45":[0,0,0]})
        set_ch(a, 'left_arm', 'rotation', {"0.0":[0,0,0],"0.1":[-8,0,-4],"0.45":[0,0,0]})
        set_ch(a, 'right_arm', 'rotation', {"0.0":[0,0,0],"0.1":[-8,0,4],"0.45":[0,0,0]})
        fixes.append(f'{role}/idle_alert: chest 2x, +head scan 8°, +arms tense 8°')

    # === FIX 3: emotion_determined — invisible ===
    key = f'animation.{role}.emotion_determined'
    if key in anims:
        a = anims[key]
        scale_ch(a, 'chest', 'rotation', 2.5)
        scale_ch(a, 'head', 'rotation', 3.0)
        scale_ch(a, 'left_arm', 'rotation', 2.5)
        scale_ch(a, 'right_arm', 'rotation', 2.5)
        # Add body position for chest puff
        set_ch(a, 'body', 'position', {"0.0":[0,0,0],"0.2":[0,0.15,0],"0.62":[0,0,0]})
        fixes.append(f'{role}/emotion_determined: 2.5-3x all + chest puff')

    # === FIX 4: emotion_relief — barely visible sigh ===
    key = f'animation.{role}.emotion_relief'
    if key in anims:
        a = anims[key]
        for bone in ['head', 'chest', 'left_arm', 'right_arm']:
            scale_ch(a, bone, 'rotation', 2.0)
        # Visible body sigh
        set_ch(a, 'body', 'position', {"0.0":[0,0,0],"0.36":[0,-0.25,0],"0.72":[0,0,0]})
        fixes.append(f'{role}/emotion_relief: 2x all + body sigh drop')

    # === FIX 5: spawn_appear — first impression! ===
    key = f'animation.{role}.spawn_appear'
    if key in anims:
        a = anims[key]
        scale_all_bones(a, 2.0)
        fixes.append(f'{role}/spawn_appear: 2x all for dramatic entrance')

    # === FIX 6: combat_dodge — barely visible dodge ===
    for dodge in ['combat_dodge_left', 'combat_dodge_right', 'combat_dodge_back']:
        key = f'animation.{role}.{dodge}'
        if key in anims:
            a = anims[key]
            scale_all_bones(a, 2.1)  # 12° → 25°
            fixes.append(f'{role}/{dodge}: 2.1x (dodge now 25°)')

    # === FIX 7: combat_hit_react — weak recoil ===
    key = f'animation.{role}.combat_hit_react'
    if key in anims:
        a = anims[key]
        scale_all_bones(a, 1.85)  # 15° → 28°
        fixes.append(f'{role}/combat_hit_react: 1.85x (28° recoil)')

    # === FIX 8: face_combat — no head movement! ===
    key = f'animation.{role}.face_combat'
    if key in anims:
        a = anims[key]
        # Add head combat scanning
        set_ch(a, 'head', 'rotation', {
            "0.0": [0, 0, 0],
            "0.5": [0, 0, -3],
            "1.0": [0, 5, 0],
            "1.5": [0, 0, 0]
        })
        fixes.append(f'{role}/face_combat: +head scan (3° tilt, 5° yaw)')

    # === FIX 9: social_radio_check/breathe/map_read — too subtle ===
    for social in ['social_radio_check', 'social_breathe', 'social_map_read']:
        key = f'animation.{role}.{social}'
        if key in anims:
            a = anims[key]
            scale_all_bones(a, 2.0)
            fixes.append(f'{role}/{social}: 2x enhancement')

    # Save
    with open(path, 'w') as f:
        json.dump(data, f, indent=2)
        f.write('\n')

print(f'\n✅ {len(fixes)} animation fixes applied!')
for fix in fixes:
    print(f'  🔧 {fix}')
