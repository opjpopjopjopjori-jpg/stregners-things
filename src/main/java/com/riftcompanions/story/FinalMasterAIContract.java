package com.riftcompanions.story;

import com.riftcompanions.dialogue.IntelligentDialogueSystem;
import com.riftcompanions.entity.CompanionRole;

/**
 * Final Master AI Integration Contract vFINAL.
 * Connects intelligent dialogue directly to 5-chapter story events,
 * ensuring every chapter produces professional multi-character dialogue
 * with observations, explanations, and role-specific actions.
 */
public final class FinalMasterAIContract {
    private FinalMasterAIContract() {}

    public static String generateChapterDialogue(final StregnerChapterStoryFramework.Chapter chapter, final boolean night, final String worldEvent) {
        CompanionRole observer = getObserverForChapter(chapter);
        CompanionRole responder = getResponderForChapter(chapter);
        IntelligentDialogueSystem.Observation obs = IntelligentDialogueSystem.observeAndExplain(observer, worldEvent, night);
        return IntelligentDialogueSystem.multiCharacterDialogue(obs, responder);
    }

    private static CompanionRole getObserverForChapter(StregnerChapterStoryFramework.Chapter ch) {
        return switch (ch) {
            case CH01_GATHERING, CH02_DISCOVERY -> CompanionRole.SEER;
            case CH03_COMBAT -> CompanionRole.GUARDIAN;
            case CH04_ARMOR_COLLECTION -> CompanionRole.SCOUT;
            case CH05_CONCLUSION -> CompanionRole.SEER;
        };
    }

    private static CompanionRole getResponderForChapter(StregnerChapterStoryFramework.Chapter ch) {
        return switch (ch) {
            case CH01_GATHERING -> CompanionRole.GUARDIAN;
            case CH02_DISCOVERY -> CompanionRole.SCOUT;
            case CH03_COMBAT -> CompanionRole.SEER;
            case CH04_ARMOR_COLLECTION -> CompanionRole.SEER;
            case CH05_CONCLUSION -> CompanionRole.GIFTED;
        };
    }
}
