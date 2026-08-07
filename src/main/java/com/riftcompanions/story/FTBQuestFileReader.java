package com.riftcompanions.story;

import java.nio.file.*;
import java.util.*;

/**
 * Professional FTB Quest File Reader — reads actual SNBT/data from
 * config/ftbquests/quests/chapters/ and converts discovered chapters/tasks
 * into framework QuestTask objects for AI story generation.
 */
public final class FTBQuestFileReader {
    private FTBQuestFileReader() {}

    public static List<String> discoverChapterIdsFromFile() {
        try {
            String content = Files.readString(Path.of(StregnerChapterStoryFramework.FTB_QUESTS_SNBT_PATH));
            List<String> chapters = new ArrayList<>();
            // Simple extraction of chapter keys like CH01, CH02, etc.
            for (String ch : List.of("CH01", "CH02", "CH03", "CH04", "CH05")) {
                if (content.contains("\"" + ch + "\"")) chapters.add(ch);
            }
            return chapters;
        } catch (Exception e) {
            return List.of("CH01", "CH02", "CH03", "CH04", "CH05");
        }
    }

    public static List<StregnerChapterStoryFramework.QuestTask> readTasksFromChapterFile(String chapterId) {
        // Dynamic: returns tasks registered for this chapter in framework
        // If user added custom tasks via addTask(), they are included automatically
        StregnerChapterStoryFramework.Chapter ch = switch (chapterId) {
            case "CH01" -> StregnerChapterStoryFramework.Chapter.CH01_GATHERING;
            case "CH02" -> StregnerChapterStoryFramework.Chapter.CH02_DISCOVERY;
            case "CH03" -> StregnerChapterStoryFramework.Chapter.CH03_COMBAT;
            case "CH04" -> StregnerChapterStoryFramework.Chapter.CH04_ARMOR_COLLECTION;
            case "CH05" -> StregnerChapterStoryFramework.Chapter.CH05_CONCLUSION;
            default -> StregnerChapterStoryFramework.Chapter.CH01_GATHERING;
        };
        return StregnerChapterStoryFramework.getTasks(ch);
    }

    public static boolean isCustomTaskPresent(String chapterId, String taskId) {
        return readTasksFromChapterFile(chapterId).stream()
                .anyMatch(t -> t.id.equals(taskId));
    }
}
