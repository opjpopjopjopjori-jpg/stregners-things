package com.riftcompanions.dialogue;

import com.riftcompanions.entity.CompanionRole;

/**
 * Professional Intelligent Dialogue System — characters observe, explain,
 * discuss, and decide together. Not generic warnings. Real observations.
 */
public final class IntelligentDialogueSystem {
    private IntelligentDialogueSystem() {}

    public record Observation(String observer, String event, String reason, String suggestedAction) {}

    public static Observation observeAndExplain(final CompanionRole observer, final String worldState, final boolean isNight) {
        return new Observation(
            observer.personalName(),
            worldState,
            buildReason(worldState, isNight),
            buildAction(observer, worldState, isNight)
        );
    }

    private static String buildReason(String event, boolean night) {
        if (night) return "Night has fallen. Exposure is dangerous without shelter near vanilla structures or mod structures.";
        if (event.contains("zombie")) return "Large hostile group observed. Numbers exceed safe combat threshold with arrows and concealment.";
        if (event.contains("dragon")) return "Dragon entity detected. Extremely powerful. Immediate retreat is the only safe tactical decision.";
        if (event.contains("cave")) return "Nearby structure offers safe shelter and defense from weather and hostiles.";
        return "Environmental hazard detected. Safety requires immediate adjustment.";
    }

    private static String buildAction(CompanionRole role, String event, boolean night) {
        switch (role) {
            case SEER: return night ? "Search for nearby cave, village, or structure from vanilla or installed mods." : "Scan area for safe route and observe all hostile numbers.";
            case GUARDIAN: return "Form defensive position. Propose arrow and concealment plan if combat unavoidable.";
            case GIFTED: return "Prepare protective ability (Shield/Push) if protected-boundary checks allow.";
            case SCOUT: return "Mark safe route and signal nearby structures (caves, towers, villages) for team relocation.";
            default: return "Assess and respond.";
        }
    }

    public static String multiCharacterDialogue(final Observation obs, final CompanionRole responder) {
        return observerSpeaks(obs) + " " + responderAsks(responder, obs) + " " + observerExplains(obs);
    }

    private static String observerSpeaks(Observation obs) {
        return obs.observer() + ": We must retreat — I observe " + obs.event() + ".";
    }
    private static String responderAsks(CompanionRole r, Observation obs) {
        return r.personalName() + ": What is happening exactly?";
    }
    private static String observerExplains(Observation obs) {
        return obs.observer() + ": The reason is clear: " + obs.reason() + ". Suggested action: " + obs.suggestedAction() + ".";
    }
}
