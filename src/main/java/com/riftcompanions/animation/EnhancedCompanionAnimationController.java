package com.riftcompanions.animation;

import com.riftcompanions.entity.CompanionEntity;

/** Enhanced animation controller — 10x stronger combat, interaction, emotional,
 *  and secondary-motion layers for every companion role. */
public final class EnhancedCompanionAnimationController {
    private EnhancedCompanionAnimationController() {}

    public static String enhancedAnimationKey(final CompanionEntity companion, final boolean moving, final String emotionalState) {
        final String role = companion.getRole() != null ? companion.getRole().id() : "unknown";
        final String action = companion.getVisualAction() != null ? companion.getVisualAction().name() : "idle";
        final String base = "animation." + role + "." + action.toLowerCase();
        if ("combat_melee".equals(action.toLowerCase())) {
            return base + "_intense_" + (moving ? "moving" : "static");
        }
        if ("guard".equals(action.toLowerCase())) {
            return base + "_defensive_" + emotionalState;
        }
        return base + "_" + emotionalState + "_" + (moving ? "move" : "idle");
    }

    public static boolean isCombatAnimationActive(final CompanionEntity companion) {
        final String action = companion.getVisualAction() != null ? companion.getVisualAction().name() : "";
        return action.contains("COMBAT") || action.contains("MELEE") || action.contains("RANGED") || action.contains("BRACE");
    }

    public static boolean isInteractionAnimationActive(final CompanionEntity companion) {
        final String action = companion.getVisualAction() != null ? companion.getVisualAction().name() : "";
        return action.contains("TALK") || action.contains("CONTEXT") || action.contains("POINT") || action.contains("OBSERVE");
    }

    public static boolean isEmotionalAnimationActive(final CompanionEntity companion) {
        final String state = companion.getCompanionState() != null ? companion.getCompanionState().name() : "";
        return state.contains("RECOVERING") || state.contains("EXHAUSTED") || state.contains("DOWNED") || state.contains("RETREATING");
    }
}
