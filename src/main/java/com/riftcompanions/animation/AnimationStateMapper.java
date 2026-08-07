package com.riftcompanions.animation;

import com.riftcompanions.entity.CompanionEntity;

/**
 * Stable public mapper façade for animation integrations. It delegates to the
 * role-aware mapper and has no side effects.
 */
public final class AnimationStateMapper {
    private AnimationStateMapper() {}

    public static AnimationClip select(final CompanionEntity companion, final boolean moving) {
        return CompanionAnimationStateMapper.select(companion, moving);
    }
}
