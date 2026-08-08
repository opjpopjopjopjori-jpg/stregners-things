#!/usr/bin/env python3
"""
Playtest review: evaluate every animation as a real player would see it.
"""
import json
from pathlib import Path

ROOT = Path('src/main/resources/assets/riftcompanions/animations')
SEP = "=" * 70

for role in ['guardian', 'gifted', 'scout', 'seer']:
    with open(ROOT / f'{role}.animation.json') as f:
        data = json.load(f)

    print(f'\n{SEP}')
    print(f'🎮 {role.upper()} — ANIMATION PLAYTEST')
    print(SEP)

    for ak in sorted(data['animations'].keys()):
        anim = data['animations'][ak]
        if not isinstance(anim, dict):
            continue

        name = ak.replace(f'animation.{role}.', '')
        length = anim.get('animation_length', 0)
        loop = anim.get('loop', '?')
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
                ts_data = {}
                for k in cd.keys():
                    try:
                        t = float(k)
                        v = cd[k]
                        if isinstance(v, list):
                            ts_data[t] = v
                        elif isinstance(v, dict):
                            vec = v.get('vector', v.get('post', [0, 0, 0]))
                            if isinstance(vec, list):
                                ts_data[t] = vec
                    except ValueError:
                        pass

                if ts_data:
                    max_val = 0
                    for t, v in ts_data.items():
                        max_val = max(max_val, max(abs(x) for x in v))
                    bone_summary[f'{bn}.{ch}'] = {'kf': len(ts_data), 'max': max_val}

        total_bones = len(bone_summary)
        total_kf = sum(s['kf'] for s in bone_summary.values())
        max_amplitude = max((s['max'] for s in bone_summary.values()), default=0)

        has_head = any('head' in k for k in bone_summary)
        has_chest = any('chest' in k for k in bone_summary)
        has_arms = any('arm' in k for k in bone_summary)
        has_legs = any('leg' in k or 'foot' in k for k in bone_summary)
        has_eye = any('eye' in k or 'pupil' in k or 'lid' in k or 'brow' in k for k in bone_summary)
        has_hair = any('hair' in k for k in bone_summary)
        has_jaw = any('jaw' in k for k in bone_summary)

        parts = []
        if has_head: parts.append('رأس')
        if has_chest: parts.append('صدر')
        if has_arms: parts.append('ايدي')
        if has_legs: parts.append('ارجل')
        if has_eye: parts.append('عيون')
        if has_hair: parts.append('شعر')
        if has_jaw: parts.append('فك')

        loop_str = str(loop)
        print(f'  🎬 {name} [{length}s, loop={loop_str}]')
        print(f'     اجزاء: {" + ".join(parts) if parts else "?"}')
        print(f'     عظام: {total_bones} | kf: {total_kf} | اقصى: {max_amplitude:.1f}')

        if max_amplitude > 15:
            big = sorted(bone_summary.items(), key=lambda x: x[1]['max'], reverse=True)[:3]
            for bname, bdata in big:
                print(f'     ⚡ {bname}: max={bdata["max"]:.1f}')
        print()
