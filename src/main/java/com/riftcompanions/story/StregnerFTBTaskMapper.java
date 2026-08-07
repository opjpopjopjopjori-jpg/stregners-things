package com.riftcompanions.story;

/**
 * Deeper FTB Quests integration — maps SNBT tasks to framework actions.
 * Every task type in FTB Quests (item, kill, location, structure, checkmark)
 * has a direct mapping here for AI story generation and server validation.
 */
public final class StregnerFTBTaskMapper {
    private StregnerFTBTaskMapper() {}

    public static String mapTaskToDialogueHook(String taskType, String label) {
        return switch (taskType) {
            case "item" -> "Gather required resource: " + label + ". Dialogue should reference collecting materials safely.";
            case "kill" -> "Combat objective: defeat target. Dialogue must include tactical planning (arrows, concealment, roles).";
            case "location" -> "Discovery event: locate place. Dialogue should reflect exploration, scouting, and village discovery via Seer power.";
            case "structure" -> "Structure discovery: search for specific biome/building. Dialogue references searching again with new target if not found.";
            case "checkmark" -> "Manual confirmation / story activation. Dialogue should confirm agreement or command activation.";
            default -> "Generic task: " + label;
        };
    }

    public static boolean requiresPrerequisite(String chapterId) {
        return !"CH01".equals(chapterId);
    }
}
