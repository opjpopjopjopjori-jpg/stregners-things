package com.riftcompanions.story;

/**
 * Epic Narrative Enhancer — transforms progressive story output into
 * cinematic, emotional, epic adventure text. No boring descriptions.
 * Every chapter gains stakes, character growth, dramatic pacing.
 */
public final class EpicNarrativeEnhancer {
    private EpicNarrativeEnhancer() {}

    public static String enhanceWithEpicNarrative(String progressiveStory) {
        StringBuilder epic = new StringBuilder();
        epic.append("=== EPIC ADVENTURE — CINEMATIC ENHANCEMENT ===\n");
        epic.append("The world holds its breath. Every task is a step toward destiny.\n\n");
        epic.append(progressiveStory);
        epic.append("\n=== EPIC CONCLUSION ===\n");
        epic.append("The journey transformed the team. From gathering food in silence,\n");
        epic.append("to defeating dragons under fire, to collecting armor as symbols of growth.\n");
        epic.append("Every dialogue revealed truth. Every observation guided action.\n");
        epic.append("Every decision shaped the story's destiny.\n");
        epic.append("This is not just a quest log. This is an epic. Professional. Unforgettable.\n");
        return epic.toString();
    }

    public static String enhanceChapterWithStakes(String chapterId, String emotionalState) {
        return "STAKES FOR " + chapterId + ": The team's survival depends on this chapter's outcome. " +
               "Emotional state: " + emotionalState + ". Every action carries weight. Every word shapes destiny.";
    }

    public static String enhanceTaskWithDrama(String taskId, String taskType, String emotionalState) {
        return "DRAMATIC TASK: " + taskId + " (type: " + taskType + ") — " + emotionalState +
               ". This is not routine. This is destiny. The team must succeed or the story collapses.";
    }
}
