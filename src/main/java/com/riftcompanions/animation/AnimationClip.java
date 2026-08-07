package com.riftcompanions.animation;

/**
 * Immutable playback selection generated from authoritative state. The clip
 * key is an asset identifier, not an instruction to apply gameplay effects.
 */
public record AnimationClip(
        String key,
        AnimationPlaybackMode playback,
        AnimationPriority priority,
        float preferredBlendSeconds,
        boolean interruptible
) {
    public AnimationClip {
        key = key == null ? "animation.guardian.idle" : key;
        playback = playback == null ? AnimationPlaybackMode.LOOP : playback;
        priority = priority == null ? AnimationPriority.AMBIENT : priority;
        preferredBlendSeconds = Math.max(0.0F, Math.min(0.5F, preferredBlendSeconds));
    }

    public static AnimationClip loop(final String key, final AnimationPriority priority, final float blendSeconds) {
        return new AnimationClip(key, AnimationPlaybackMode.LOOP, priority, blendSeconds, true);
    }

    public static AnimationClip oneShot(final String key, final AnimationPriority priority, final float blendSeconds,
                                        final boolean interruptible) {
        return new AnimationClip(key, AnimationPlaybackMode.ONE_SHOT, priority, blendSeconds, interruptible);
    }
}
