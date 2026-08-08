package com.riftcompanions.animation;

import com.riftcompanions.entity.CompanionRole;

/** Full integration: every chapter + every role + every emotional state = unique animation sequence. */
public final class FullAnimationStoryIntegration {
    private FullAnimationStoryIntegration() {}

    public static String getChapterRoleAnimation(final int chapterSequence, final CompanionRole role, final String emotionalState) {
        final String ch = String.format("CH%02d", chapterSequence);
        final String ro = role.id();
        return "animation.story." + ch + "." + ro + "." + emotionalState;
    }

    public static boolean isValidChapterAnimation(final int chapterSequence) {
        return chapterSequence >= 1 && chapterSequence <= 5;
    }
}
