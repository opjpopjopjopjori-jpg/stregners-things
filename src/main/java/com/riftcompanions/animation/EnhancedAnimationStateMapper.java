package com.riftcompanions.animation;

import com.riftcompanions.entity.CompanionEntity;

public final class EnhancedAnimationStateMapper {
    private EnhancedAnimationStateMapper() {}

    public static AnimationClip enhancedSelect(final CompanionEntity companion, final boolean moving, final String emotionalState) {
        final String roleId = companion.getRole() != null ? companion.getRole().id() : "unknown";
        final String action = companion.getVisualAction() != null ? companion.getVisualAction().name() : "idle";
        final String key = EnhancedCompanionAnimationController.enhancedAnimationKey(companion, moving, emotionalState);
        final boolean loop = !action.contains("COMBAT") && !action.contains("POWER") && !action.contains("CONTEXT");
        return new AnimationClip(key, loop ? AnimationPlaybackMode.LOOP : AnimationPlaybackMode.ONCE, emotionalState);
    }
}
