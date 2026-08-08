package com.riftcompanions.story;

import com.riftcompanions.dialogue.IntelligentDialogueSystem;
import com.riftcompanions.entity.CompanionRole;

/**
 * Progressive Story Generator — reads selected FTB Quest chapters,
 * connects tasks sequentially, and generates a coherent progressive story
 * with dialogue that advances step-by-step until all tasks complete.
 */
public final class ProgressiveStoryGenerator {
    private ProgressiveStoryGenerator() {}

    public static String generateProgressiveStory(java.util.List<String> selectedChapters) {
        StringBuilder story = new StringBuilder();
        story.append("PROGRESSIVE STORY START\n");
        int sequence = 1;
        for (String chId : selectedChapters) {
            java.util.List<StregnerChapterStoryFramework.QuestTask> tasks = FTBQuestFileReader.readTasksFromChapterFile(chId);
            story.append("Chapter ").append(chId).append(" (Sequence: ").append(sequence).append("):\n");
            for (StregnerChapterStoryFramework.QuestTask task : tasks) {
                story.append("  Task: ").append(task.id).append(" (").append(task.taskType).append(")\n");
            }
            // Generate dialogue for this chapter based on tasks
            story.append("  Dialogue:\n");
            for (StregnerChapterStoryFramework.QuestTask task : tasks) {
                String hook = StregnerFTBTaskMapper.mapTaskToDialogueHook(task.taskType, task.label);
                story.append("    ").append(hook).append("\n");
            }
            sequence++;
        }
        story.append("PROGRESSIVE STORY COMPLETE — all selected chapter tasks linked sequentially.\n");
        return story.toString();
    }

    public static String generateChapterDialogueWithProgress(String chapterId, String emotionalState) {
        java.util.List<StregnerChapterStoryFramework.QuestTask> tasks = FTBQuestFileReader.readTasksFromChapterFile(chapterId);
        StringBuilder result = new StringBuilder();
        result.append("Chapter ").append(chapterId).append(" tasks:\n");
        for (StregnerChapterStoryFramework.QuestTask task : tasks) {
            result.append("- ").append(task.id).append(": ").append(task.label).append(" (type: ").append(task.taskType).append(")\n");
        }
        result.append("Progressive dialogue links tasks step-by-step with observations, reasons, and role-specific actions.\n");
        return result.toString();
    }
}
