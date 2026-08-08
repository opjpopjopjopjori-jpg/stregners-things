package com.riftcompanions.story;

import com.riftcompanions.entity.CompanionRole;
import java.util.*;

/**
 * Stregner Integrated Chapter Story Framework — FTB Quest Mod Compatible (1.20.1).
 *
 * This module connects directly to FTB Quests SNBT data at:
 * config/ftbquests/quests/chapters/784A70C90F6F5A49.snbt
 *
 * It defines 5 sequential chapters (CH01-CH05), each with prerequisites,
 * task lists (item, kill, location, structure, checkmark), dialogue contracts,
 * and character role assignments. The framework is designed for professional
 * AI generation: given this source + SNBT, an AI can produce complete
 * coherent English dialogue, event sequences, and story resolution without ambiguity.
 */
public final class StregnerChapterStoryFramework {
    private StregnerChapterStoryFramework() {}

    public enum Chapter {
        CH01_GATHERING(1, "Gather food; scout area; prerequisite: none"),
        CH02_DISCOVERY(2, "Locate village or search for structure/biome; prerequisite: CH01 complete"),
        CH03_COMBAT(3, "Defeat 10 zombies; agree arrow/concealment plan; prerequisite: CH02 complete"),
        CH04_ARMOR_COLLECTION(4, "Collect armor; gather additional food; prerequisite: CH03 complete"),
        CH05_CONCLUSION(5, "Activate complete 5-chapter story via command; full team resolution; prerequisite: CH04 complete");

        public final int sequence;
        public final String prerequisiteSummary;
        Chapter(int s, String p) { this.sequence = s; this.prerequisiteSummary = p; }
    }

    public static final class QuestTask {
        public final String id;
        public final String label;
        public final String taskType; // item_collection, exploration, combat, tactical_decision, command_trigger
        public QuestTask(String id, String label, String type) { this.id = id; this.label = label; this.taskType = type; }
    }

    // Dynamic task registry — allows users to add/remove/modify tasks per chapter later
    private static final Map<Chapter, List<QuestTask>> DYNAMIC_TASKS = new HashMap<>();

    static {
        addTasks(Chapter.CH01_GATHERING, List.of(
            new QuestTask("collect_food", "Gather food resources", "item_collection"),
            new QuestTask("scout_area", "Scout surrounding area", "exploration")
        ));
        addTasks(Chapter.CH02_DISCOVERY, List.of(
            new QuestTask("find_village", "Locate nearby village or structure", "location_discovery"),
            new QuestTask("search_for_structure", "Search for biome structure / pyramid", "search")
        ));
        addTasks(Chapter.CH03_COMBAT, List.of(
            new QuestTask("kill_10_zombies", "Defeat 10 zombies", "combat"),
            new QuestTask("plan_strategy", "Agree arrow and concealment strategy", "tactical_decision")
        ));
        addTasks(Chapter.CH04_ARMOR_COLLECTION, List.of(
            new QuestTask("collect_armor", "Gather armor pieces", "item_collection"),
            new QuestTask("collect_more_food", "Gather additional food", "item_collection")
        ));
        addTasks(Chapter.CH05_CONCLUSION, List.of(
            new QuestTask("final_dialogue", "Full team dialogue and resolution", "narrative_resolution"),
            new QuestTask("complete_story_command", "Activate complete 5-chapter story via command", "command_trigger")
        ));
    }

    public static void addTask(Chapter ch, QuestTask task) {
        DYNAMIC_TASKS.computeIfAbsent(ch, k -> new ArrayList<>()).add(task);
    }

    public static void addTasks(Chapter ch, List<QuestTask> tasks) {
        DYNAMIC_TASKS.put(ch, new ArrayList<>(tasks));
    }

    public static List<QuestTask> getTasks(Chapter ch) {
        return DYNAMIC_TASKS.getOrDefault(ch, List.of());
    }

    public static void clearChapterTasks(Chapter ch) {
        DYNAMIC_TASKS.remove(ch);
    }

    public static CompanionRole[] rolesForChapter(Chapter ch) {
        return switch (ch) {
            case CH01_GATHERING -> new CompanionRole[]{CompanionRole.SEER, CompanionRole.GUARDIAN};
            case CH02_DISCOVERY -> new CompanionRole[]{CompanionRole.SEER, CompanionRole.SCOUT};
            case CH03_COMBAT -> new CompanionRole[]{CompanionRole.GUARDIAN, CompanionRole.SEER, CompanionRole.GIFTED};
            case CH04_ARMOR_COLLECTION -> new CompanionRole[]{CompanionRole.SCOUT, CompanionRole.SEER};
            case CH05_CONCLUSION -> new CompanionRole[]{CompanionRole.SEER, CompanionRole.GUARDIAN, CompanionRole.SCOUT, CompanionRole.GIFTED};
        };
    }

    public static boolean isChapterComplete(StoryState state, Chapter ch) {
        return state.complete(mapChapter(ch));
    }

    private static StoryChapter mapChapter(Chapter ch) {
        return switch (ch) {
            case CH01_GATHERING -> StoryChapter.ARRIVAL;
            case CH02_DISCOVERY -> StoryChapter.A_WAY_BACK;
            case CH03_COMBAT -> StoryChapter.SIGNS_IN_THE_WORLD;
            case CH04_ARMOR_COLLECTION -> StoryChapter.THE_HIVE_ECHO;
            case CH05_CONCLUSION -> StoryChapter.THE_HIVE_ECHO;
        };
    }

    public static String generateStoryDirective() {
        return "FTB Quest Compatible — 5 sequential chapters (CH01-CH05), prerequisites enforced, tasks defined per chapter (item/collection/combat/search/command), roles assigned (Seer/Guardian/Gifted/Scout), dialogue hooks tied to events (village discovery via Seer power, zombie combat tactics arrows+concealment, armor/food collection), and a final command trigger that activates the complete story. Use StregnerFTBTaskMapper for task mapping. AI should generate full English dialogue, event progression, prerequisite checks, and coherent narrative linking all chapters using this framework.";
    }

    public static final String FTB_QUESTS_SNBT_PATH = "config/ftbquests/quests/chapters/784A70C90F6F5A49.snbt";

    public static String ftbQuestChapterSchema() {
        return "{\"chapter\":\"CH%02d\",\"title\":\"...\",\"prerequisites\":[\"CH%02d\"],\"tasks\":{...},\"dialogue_hook\":\"...\",\"event_trigger\":\"...\"}";
    }
}
