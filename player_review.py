#!/usr/bin/env python3
"""
Player-perspective animation review.
Evaluates each animation as a real player would see it in-game.
Identifies which animations are impressive vs which feel basic.
"""
import json
from pathlib import Path

ROOT = Path('src/main/resources/assets/riftcompanions/animations')

# Categories for player impression
IMPRESSIVE = []  # Would wow the player
DECENT = []      # Looks good, not amazing
BASIC = []       # Feels simple, needs improvement
WEAK = []        # Barely visible, definitely needs fixing

for role in ['guardian', 'gifted', 'scout', 'seer']:
    with open(ROOT / f'{role}.animation.json') as f:
        data = json.load(f)

    for ak in sorted(data['animations'].keys()):
        anim = data['animations'][ak]
        if not isinstance(anim, dict):
            continue

        name = ak.replace(f'animation.{role}.', '')
        length = anim.get('animation_length', 0)
        bones = anim.get('bones', {})
        if not isinstance(bones, dict):
            continue

        bone_summary = {}
        for bn, bd in bones.items():
            if not isinstance(bd, dict):
                continue
            for ch, cd in bd.items():
                if not isinstance(cd, dict):
                    continue
                max_val = 0
                for k in cd.keys():
                    try:
                        float(k)
                        v = cd[k]
                        if isinstance(v, list):
                            max_val = max(max_val, max(abs(x) for x in v))
                        elif isinstance(v, dict):
                            vec = v.get('vector', v.get('post', [0,0,0]))
                            if isinstance(vec, list):
                                max_val = max(max_val, max(abs(x) for x in vec))
                    except ValueError:
                        pass
                if max_val > 0:
                    bone_summary[f'{bn}.{ch}'] = max_val

        total_bones = len(bone_summary)
        max_amp = max(bone_summary.values()) if bone_summary else 0

        has_head = any('head' in k for k in bone_summary)
        has_chest = any('chest' in k for k in bone_summary)
        has_body = any('body' in k for k in bone_summary)
        has_arms = any('arm' in k for k in bone_summary)
        has_legs = any('leg' in k or 'foot' in k for k in bone_summary)
        has_eye = any('eye' in k or 'pupil' in k or 'lid' in k or 'brow' in k for k in bone_summary)
        has_hair = any('hair' in k for k in bone_summary)
        has_jaw = any('jaw' in k for k in bone_summary)

        # Player impression scoring
        score = 0
        issues = []

        # === BODY ANIMATIONS ===
        if name in ['idle']:
            # Idle should have visible breathing (chest position > 0.1)
            chest_pos = max((v for k, v in bone_summary.items() if 'chest' in k and 'position' in k), default=0)
            head_rot = max((v for k, v in bone_summary.items() if 'head' in k and 'rotation' in k), default=0)
            if chest_pos < 0.12:
                issues.append(f'تنفس الصدر ضعيف ({chest_pos:.2f} بدل ≥0.15)')
                score -= 3
            if head_rot < 2:
                issues.append(f'حركة الرأس ضعيفة ({head_rot:.1f}° بدل ≥2°)')
                score -= 1
            if total_bones >= 5:
                score += 1

        elif name in ['walk', 'run']:
            if max_amp >= 25:
                score += 3
            elif max_amp >= 15:
                score += 1
            else:
                issues.append('خطوات ضعيفة')
                score -= 2
            if has_head and has_arms and has_legs:
                score += 2
            if not has_head:
                issues.append('ماكو حركة رأس')

        elif name in ['combat_ready']:
            arm_rot = max((v for k, v in bone_summary.items() if 'arm' in k and 'rotation' in k), default=0)
            forearm = max((v for k, v in bone_summary.items() if 'arm_lower' in k or 'arm_upper' in k), default=0)
            if arm_rot >= 25 and forearm >= 35:
                score += 3  # Fists raised = impressive
            if has_head and max((v for k, v in bone_summary.items() if 'head' in k), default=0) >= 3:
                score += 1  # Head scanning

        elif 'melee_attack' in name:
            if max_amp >= 65:
                score += 3  # Strong strike
            elif max_amp >= 40:
                score += 1
            chest_twist = max((v for k, v in bone_summary.items() if 'chest' in k and 'rotation' in k), default=0)
            if chest_twist >= 10:
                score += 2  # Body twist = realistic
            if not has_head:
                issues.append('ماكو حركة رأس بالضربة')

        elif name == 'melee_block':
            if max_amp >= 60:
                score += 3  # Arms up to block
            elif max_amp >= 30:
                score += 1

        elif name in ['combat_dodge_left', 'combat_dodge_right', 'combat_dodge_back']:
            if max_amp >= 20:
                score += 2
            else:
                issues.append(f' dodge ضعيف ({max_amp:.0f}° بدل ≥20°)')
                score -= 2
            if total_bones >= 6:
                score += 1

        elif name == 'combat_hit_react':
            if max_amp >= 20:
                score += 2
            else:
                issues.append(f' رد فعل الضربة ضعيف ({max_amp:.0f}°)')
                score -= 2

        # === EMOTION ANIMATIONS ===
        elif name == 'emotion_fear':
            if max_amp >= 25:
                score += 2
            else:
                issues.append('خوف ما يبان كافي')
                score -= 1
            # Fear should have body trembling
            if not (has_body or has_chest):
                issues.append('ماكو ارتعاش بالجسم')

        elif name == 'emotion_determined':
            if max_amp >= 15:
                score += 2
            else:
                issues.append(f'العزيمة ما تبان ({max_amp:.0f}°) — لازم صدر منتفخ + رأس مرفوع')
                score -= 3

        elif name == 'emotion_relief':
            if max_amp >= 12:
                score += 1
            else:
                issues.append(f'الارتياح ضعيف ({max_amp:.0f}°) — لازم تنهيدة واضحة')
                score -= 2

        elif name == 'emotion_exhausted':
            if max_amp >= 15:
                score += 2
            else:
                issues.append('الارهاق ما يبان')

        # === FACE ANIMATIONS ===
        elif name == 'face_idle':
            # Blink should be visible
            lid = max((v for k, v in bone_summary.items() if 'lid' in k), default=0)
            pupil = max((v for k, v in bone_summary.items() if 'pupil' in k), default=0)
            if lid >= 0.5 and pupil >= 0.2:
                score += 2
            # 72 keyframes = detailed blink cycle

        elif name == 'face_combat':
            if has_head:
                score += 2
            else:
                issues.append('ماكو حركة رأس بالقتال — العيون بس ما تكفي')
                score -= 2
            brow = max((v for k, v in bone_summary.items() if 'brow' in k), default=0)
            if brow >= 0.2:
                score += 1  # Brow furrowed = combat face

        elif name == 'face_talk':
            if has_jaw:
                score += 2  # Lip sync!
            if has_eye:
                score += 1

        # === HIVE/POWER ANIMATIONS ===
        elif 'hive_' in name:
            if max_amp >= 80:
                score += 3  # Very dramatic = impressive
            elif max_amp >= 40:
                score += 2
            if total_bones >= 6:
                score += 1

        # === SOCIAL ANIMATIONS ===
        elif name.startswith('social_'):
            if max_amp >= 30:
                score += 2
            elif max_amp >= 15:
                score += 1
            else:
                if name not in ['social_listen', 'social_observe', 'social_calm', 'social_travel', 'social_weather', 'social_base', 'social_cave', 'social_village', 'social_reflect']:
                    issues.append(f' اجتماعي ضعيف ({max_amp:.0f}°)')
                    score -= 1

        # === OTHER ===
        elif name == 'downed' or name == 'downed_hold':
            if max_amp >= 60:
                score += 3  # Dramatic fall

        elif name == 'recover':
            if max_amp >= 40:
                score += 2

        elif name == 'retreat_signal':
            if max_amp >= 70:
                score += 3  # Arm raised high

        elif name == 'spawn_appear':
            if max_amp >= 20:
                score += 2
            else:
                issues.append(f'ظهور اول مرة ضعيف ({max_amp:.0f}°) — اول انطباع مهم!')
                score -= 2

        elif name == 'interact_point':
            if max_amp >= 70:
                score += 2  # Clear pointing

        elif name == 'interact_talk':
            if has_jaw and max_amp >= 25:
                score += 2

        elif name == 'protect' or name == 'guard_stance':
            if max_amp >= 50:
                score += 2

        elif name.startswith('secondary_'):
            hair_max = max((v for k, v in bone_summary.items() if 'hair' in k), default=0)
            if 'power' in name and hair_max >= 5:
                score += 2  # Hair surge
            elif 'run' in name and hair_max >= 5:
                score += 1

        elif name == 'calm_look_around':
            if max_amp >= 15 and total_bones >= 4:
                score += 2
            else:
                issues.append('نظرة حول ضعيفة — لازم رأس + صدر + ايد')

        elif name == 'idle_alert':
            if max_amp >= 10 and total_bones >= 3:
                score += 2
            else:
                issues.append(f'انتباه ضعيف ({max_amp:.0f}°, {total_bones} عظام فقط) — لازم يكون واضح')
                score -= 3

        elif name == 'walk_crouched':
            if has_arms:
                score += 1
            else:
                issues.append('ماكو حركة ايد بالانحناء — لازم ايد مقلوبة لقدام')

        # Classify
        entry = f'{role}/{name}'
        if score >= 4:
            IMPRESSIVE.append((entry, score, issues))
        elif score >= 2:
            DECENT.append((entry, score, issues))
        elif score >= 0:
            BASIC.append((entry, score, issues))
        else:
            WEAK.append((entry, score, issues))

# Print results
print("=" * 70)
print("🎮 تقييم الأنيميشن من نظرة لاعب حقيقي")
print("=" * 70)

print(f"\n🔥 IMPRESSIVE ({len(IMPRESSIVE)}) — يبهور اللاعب:")
for entry, score, issues in sorted(IMPRESSIVE, key=lambda x: -x[1]):
    print(f"   ✅ {entry} (score={score})")

print(f"\n👍 DECENT ({len(DECENT)}) — زين بس مو مذهل:")
for entry, score, issues in sorted(DECENT, key=lambda x: -x[1]):
    iss = ' | '.join(issues) if issues else ''
    print(f"   ✅ {entry} (score={score}) {iss}")

print(f"\n😐 BASIC ({len(BASIC)}) — تحسه بسيط:")
for entry, score, issues in sorted(BASIC, key=lambda x: x[1]):
    iss = ' | '.join(issues) if issues else ''
    print(f"   ⚠️  {entry} (score={score}) {iss}")

print(f"\n❌ WEAK ({len(WEAK)}) — ضعيف، لازم يتحسن:")
for entry, score, issues in sorted(WEAK, key=lambda x: x[1]):
    iss = ' | '.join(issues) if issues else ''
    print(f"   ❌ {entry} (score={score}) {iss}")

print(f"\n{'='*70}")
print(f"الخلاصة: {len(IMPRESSIVE)} مبهور | {len(DECENT)} زين | {len(BASIC)} بسيط | {len(WEAK)} ضعيف")
print(f"{'='*70}")
