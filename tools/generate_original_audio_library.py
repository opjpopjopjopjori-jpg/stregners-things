#!/usr/bin/env python3
"""Generate deterministic original companion SFX and ambience as OGG Vorbis.

The library uses synthesized tones, filtered noise, envelopes, and delays only.
It contains no actor voice, show music, sampled dialogue, or third-party audio.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/assets/riftcompanions/sounds"
SAMPLE_RATE = 48_000
RNG = np.random.default_rng(0x52494654)


def envelope(length: int, attack: float, release: float) -> np.ndarray:
    attack_samples = max(1, min(length, int(SAMPLE_RATE * attack)))
    release_samples = max(1, min(length, int(SAMPLE_RATE * release)))
    result = np.ones(length, dtype=np.float64)
    result[:attack_samples] = np.linspace(0.0, 1.0, attack_samples, endpoint=False)
    result[-release_samples:] *= np.linspace(1.0, 0.0, release_samples, endpoint=True)
    return result


def tone(time: np.ndarray, frequency: float, amplitude: float = 1.0, phase: float = 0.0) -> np.ndarray:
    return amplitude * np.sin(2.0 * np.pi * frequency * time + phase)


def chirp(time: np.ndarray, start: float, end: float, amplitude: float = 1.0) -> np.ndarray:
    duration = max(time[-1], 1.0 / SAMPLE_RATE)
    phase = 2.0 * np.pi * (start * time + 0.5 * (end - start) * time * time / duration)
    return amplitude * np.sin(phase)


def smooth_noise(length: int, smoothing: int, amplitude: float) -> np.ndarray:
    raw = RNG.normal(0.0, 1.0, length)
    kernel = np.ones(max(1, smoothing), dtype=np.float64)
    kernel /= kernel.sum()
    return amplitude * np.convolve(raw, kernel, mode="same")


def delay(signal: np.ndarray, seconds: float, feedback: float) -> np.ndarray:
    offset = int(seconds * SAMPLE_RATE)
    result = signal.copy()
    if offset <= 0:
        return result
    for start in range(offset, len(result), offset):
        result[start:] += signal[:-start] * feedback ** (start // offset)
    return result


def stereo(mono: np.ndarray, width: float = 0.18) -> np.ndarray:
    shift = max(1, int(0.004 * SAMPLE_RATE))
    left = mono
    right = np.roll(mono, shift) * (1.0 - width) + smooth_noise(len(mono), 120, width * 0.02)
    return np.stack((left, right), axis=1)


def render(kind: str, seconds: float) -> np.ndarray:
    length = int(seconds * SAMPLE_RATE)
    time = np.arange(length, dtype=np.float64) / SAMPLE_RATE
    env = envelope(length, 0.05, min(0.8, seconds * 0.32))
    base = np.zeros(length, dtype=np.float64)

    if kind == "guardian_guard_signal":
        base = 0.18 * tone(time, 92) + 0.09 * tone(time, 184) + 0.045 * smooth_noise(length, 180, 1.0)
    elif kind == "guardian_melee_swing":
        base = 0.24 * chirp(time, 180, 920) + 0.11 * smooth_noise(length, 22, 1.0)
    elif kind == "guardian_impact":
        base = 0.32 * tone(time, 72) * np.exp(-time * 8.0) + 0.18 * smooth_noise(length, 12, 1.0)
    elif kind == "guardian_retreat":
        base = 0.16 * chirp(time, 110, 310) + 0.08 * tone(time, 156) + 0.035 * smooth_noise(length, 160, 1.0)
    elif kind == "seer_notice":
        base = 0.12 * chirp(time, 310, 470) + 0.07 * tone(time, 235) + 0.04 * smooth_noise(length, 260, 1.0)
    elif kind == "seer_focus":
        modulation = 0.55 + 0.45 * np.sin(2.0 * np.pi * 0.7 * time)
        base = modulation * (0.09 * tone(time, 198) + 0.07 * tone(time, 297) + 0.03 * smooth_noise(length, 300, 1.0))
    elif kind == "seer_release":
        base = 0.20 * chirp(time, 170, 980) + 0.07 * chirp(time, 620, 240) + 0.05 * smooth_noise(length, 35, 1.0)
    elif kind == "seer_shatter":
        base = 0.20 * chirp(time, 95, 760) * np.exp(-time * 1.2) + 0.12 * smooth_noise(length, 15, 1.0) + 0.05 * tone(time, 64)
    elif kind == "seer_recovery":
        base = 0.08 * tone(time, 174) + 0.05 * tone(time, 261) + 0.025 * smooth_noise(length, 420, 1.0)
    elif kind == "gifted_notice":
        base = 0.11 * chirp(time, 240, 420) + 0.06 * tone(time, 315) + 0.025 * smooth_noise(length, 210, 1.0)
    elif kind == "gifted_focus":
        pulse = 0.50 + 0.50 * np.sin(2.0 * np.pi * 1.05 * time)
        base = pulse * (0.08 * tone(time, 220) + 0.06 * tone(time, 330) + 0.04 * tone(time, 440))
    elif kind == "gifted_push":
        base = 0.23 * chirp(time, 120, 1300) + 0.10 * smooth_noise(length, 18, 1.0)
    elif kind == "gifted_shield":
        pulse = 0.65 + 0.35 * np.sin(2.0 * np.pi * 0.85 * time)
        base = pulse * (0.07 * tone(time, 172) + 0.05 * tone(time, 258) + 0.025 * smooth_noise(length, 350, 1.0))
    elif kind == "gifted_rescue":
        base = 0.15 * chirp(time, 210, 720) + 0.10 * chirp(time, 720, 260) + 0.04 * smooth_noise(length, 50, 1.0)
    elif kind == "gifted_exhausted":
        base = 0.06 * tone(time, 132) + 0.03 * smooth_noise(length, 400, 1.0)
    elif kind == "scout_route":
        base = 0.09 * tone(time, 392) + 0.07 * tone(time, 523) + 0.035 * chirp(time, 280, 470)
    elif kind == "scout_anchor":
        pulse = 0.55 + 0.45 * np.sin(2.0 * np.pi * 0.9 * time)
        base = pulse * (0.06 * tone(time, 196) + 0.06 * tone(time, 294) + 0.035 * smooth_noise(length, 280, 1.0))
    elif kind == "scout_dodge":
        base = 0.18 * chirp(time, 750, 190) + 0.08 * smooth_noise(length, 20, 1.0)
    elif kind == "ui_plan_accept":
        base = 0.09 * tone(time, 330) + 0.08 * tone(time, 495) + 0.05 * tone(time, 660)
    elif kind == "ui_safe_mode":
        base = 0.10 * tone(time, 104) + 0.07 * tone(time, 156) + 0.03 * smooth_noise(length, 250, 1.0)
    elif kind == "ambience_guardian_base":
        base = 0.035 * tone(time, 73) + 0.02 * tone(time, 109) + 0.06 * smooth_noise(length, 600, 1.0)
    elif kind == "ambience_seer_signal":
        base = 0.035 * tone(time, 175) + 0.025 * tone(time, 262) + 0.055 * smooth_noise(length, 520, 1.0)
    elif kind == "ambience_gifted_calm":
        base = 0.035 * tone(time, 146) + 0.025 * tone(time, 219) + 0.05 * smooth_noise(length, 700, 1.0)
    elif kind == "ambience_scout_lookout":
        base = 0.025 * tone(time, 247) + 0.02 * tone(time, 370) + 0.06 * smooth_noise(length, 450, 1.0)
    else:
        raise ValueError(kind)

    base *= env
    base = delay(base, 0.14, 0.22)
    peak = max(0.001, float(np.max(np.abs(base))))
    return stereo(np.clip(base / peak * 0.70, -0.98, 0.98))


CUES = {
    "companions/guardian_guard_signal": 2.8,
    "companions/guardian_melee_swing": 1.0,
    "companions/guardian_impact": 1.2,
    "companions/guardian_retreat": 2.8,
    "companions/seer_notice": 3.2,
    "companions/seer_focus": 7.0,
    "companions/seer_release": 2.4,
    "companions/seer_shatter": 2.5,
    "companions/seer_recovery": 3.4,
    "companions/gifted_notice": 2.6,
    "companions/gifted_focus": 7.4,
    "companions/gifted_push": 1.8,
    "companions/gifted_shield": 8.0,
    "companions/gifted_rescue": 3.0,
    "companions/gifted_exhausted": 3.6,
    "companions/scout_route": 2.2,
    "companions/scout_anchor": 6.8,
    "companions/scout_dodge": 1.0,
    "ui/plan_accept": 1.7,
    "ui/safe_mode": 2.2,
    "ambience/guardian_base": 22.0,
    "ambience/seer_signal": 22.0,
    "ambience/gifted_calm": 22.0,
    "ambience/scout_lookout": 22.0,
}


def main() -> None:
    for path, seconds in CUES.items():
        output = OUTPUT / f"{path}.ogg"
        output.parent.mkdir(parents=True, exist_ok=True)
        folder, name = path.split("/", 1)
        kind = name if folder == "companions" else f"{folder}_{name}"
        signal = render(kind, seconds)
        sf.write(output, signal, SAMPLE_RATE, format="OGG", subtype="VORBIS")
    print(f"Generated {len(CUES)} original companion audio cues")


if __name__ == "__main__":
    main()
