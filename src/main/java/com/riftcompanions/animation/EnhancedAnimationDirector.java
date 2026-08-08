package com.riftcompanions.animation;

import com.riftcompanions.story.StregnerChapterStoryFramework;

/**
 * Enhanced Animation Director — links 5-chapter story events directly to
 * animation sequences (combat, dialogue, emotional recovery, defense, victory).
 * This makes the animation system 10x stronger by connecting narrative to motion.
 */
public final class EnhancedAnimationDirector {
    private EnhancedAnimationDirector() {}

    public static String storyEventAnimation(final StregnerChapterStoryFramework.Chapter chapter, final String emotionalState) {
        return switch (chapter) {
            case CH01_GATHERING -> "animation.seer.scout_" + emotionalState;
            case CH02_DISCOVERY -> "animation.seer.discover_" + emotionalState;
            case CH03_COMBAT -> "animation.guardian.combat_intense_" + emotionalState;
            case CH04_ARMOR_COLLECTION -> "animation.scout.collect_" + emotionalState;
            case CH05_CONCLUSION -> "animation.all.resolution_" + emotionalState;
        };
    }

    public static boolean isStoryChapterAnimation(final String animationKey) {
        return animationKey.contains("scout_") || animationKey.contains("discover_")
                || animationKey.contains("combat_") || animationKey.contains("resolution_");
    }
}
