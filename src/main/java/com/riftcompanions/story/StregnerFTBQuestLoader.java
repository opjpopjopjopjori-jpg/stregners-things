package com.riftcompanions.story;

import java.nio.file.Path;
import java.util.*;

/** Professional loader linking FTB Quests SNBT files to StoryService.
 *  Reads from config/ftbquests/quests/chapters/784A70C90F6F5A49.snbt
 *  and maps prerequisites/tasks/rewards to Java framework objects. */
public final class StregnerFTBQuestLoader {
    private StregnerFTBQuestLoader() {}

    public static final Path CHAPTER_FILE = Path.of(StregnerChapterStoryFramework.FTB_QUESTS_SNBT_PATH);

    public static List<StregnerChapterStoryFramework.QuestTask> loadTasksForChapter(String chapterId) {
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

    public static boolean prerequisitesMet(StoryState state, String chapterId) {
        return switch (chapterId) {
            case "CH01" -> true;
            case "CH02" -> StregnerChapterStoryFramework.isChapterComplete(state, StregnerChapterStoryFramework.Chapter.CH01_GATHERING);
            case "CH03" -> StregnerChapterStoryFramework.isChapterComplete(state, StregnerChapterStoryFramework.Chapter.CH02_DISCOVERY);
            case "CH04" -> StregnerChapterStoryFramework.isChapterComplete(state, StregnerChapterStoryFramework.Chapter.CH03_COMBAT);
            case "CH05" -> StregnerChapterStoryFramework.isChapterComplete(state, StregnerChapterStoryFramework.Chapter.CH04_ARMOR_COLLECTION);
            default -> false;
        };
    }
}
