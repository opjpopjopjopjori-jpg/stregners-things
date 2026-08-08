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
        boolean interruptible,
        String emotionalState,
        String roleContext
) {
    public AnimationClip {
        key = key == null ? "animation.guardian.idle" : key;
        playback = playback == null ? AnimationPlaybackMode.LOOP : playback;
        priority = priority == null ? AnimationPriority.AMBIENT : priority;
        preferredBlendSeconds = Math.max(0.0F, Math.min(0.5F, preferredBlendSeconds));
        emotionalState = emotionalState == null ? "neutral" : emotionalState;
        roleContext = roleContext == null ? "general" : roleContext;
    }

    public static AnimationClip loop(final String key, final AnimationPriority priority, final float blendSeconds,
                                     final String emotionalState, final String roleContext) {
        return new AnimationClip(key, AnimationPlaybackMode.LOOP, priority, blendSeconds, true, emotionalState, roleContext);
    }

    public static AnimationClip oneShot(final String key, final AnimationPriority priority, final float blendSeconds,
                                        final boolean interruptible, final String emotionalState, final String roleContext) {
        return new AnimationClip(key, AnimationPlaybackMode.ONE_SHOT, priority, blendSeconds, interruptible, emotionalState, roleContext);
    }
}
