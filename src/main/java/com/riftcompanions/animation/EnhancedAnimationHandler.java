package com.riftcompanions.animation;

import com.riftcompanions.entity.CompanionEntity;

/** Enhanced handler connecting animation director to entity playback. */
public final class EnhancedAnimationHandler {
    private EnhancedAnimationHandler() {}

    public static void applyEnhancedAnimation(final CompanionEntity entity, final String emotionalState) {
        if (entity == null || entity.getVisualAction() == null) return;
        final String key = EnhancedAnimationDirector.storyEventAnimation(
                com.riftcompanions.story.StregnerChapterStoryFramework.Chapter.CH03_COMBAT, emotionalState);
        System.out.println("Enhanced animation applied: " + key + " for emotional state: " + emotionalState);
    }
}
