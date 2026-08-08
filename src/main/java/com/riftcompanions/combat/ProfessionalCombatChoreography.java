package com.riftcompanions.combat;

import com.riftcompanions.animation.EnhancedAnimationDirector;
import com.riftcompanions.animation.EnhancedCompanionAnimationController;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;

/**
 * Professional Combat Choreography System — coordinates multi-role combat
 * animations with formation, escalation, and resolution phases.
 * Every combat task from FTB Quests triggers coordinated role animations.
 */
public final class ProfessionalCombatChoreography {
    private ProfessionalCombatChoreography() {}

    public enum CombatPhase { INITIATION, ESCALATION, PEAK, RESOLUTION, RETREAT }

    public static String coordinateCombatAnimation(final CompanionRole role,
                                                     final CombatPhase phase,
                                                     final String emotionalState) {
        String base = EnhancedAnimationDirector.storyEventAnimation(
                com.riftcompanions.story.StregnerChapterStoryFramework.Chapter.CH03_COMBAT,
                emotionalState);
        return switch (role) {
            case GUARDIAN -> base + "_guardian_" + phase.name().toLowerCase();
            case SEER -> base + "_seer_" + phase.name().toLowerCase();
            case GIFTED -> base + "_gifted_" + phase.name().toLowerCase();
            case SCOUT -> base + "_scout_" + phase.name().toLowerCase();
        };
    }

    public static boolean isCombatPhaseActive(final CombatPhase phase) {
        return phase != CombatPhase.RESOLUTION && phase != CombatPhase.RETREAT;
    }

    public static String buildCombatSequence(final CompanionRole[] activeRoles, final String emotionalState) {
        StringBuilder sequence = new StringBuilder();
        sequence.append("COMBAT CHOREOGRAPHY SEQUENCE (Phase: ").append(emotionalState).append("):\n");
        for (CompanionRole role : activeRoles) {
            for (CombatPhase phase : CombatPhase.values()) {
                sequence.append("  ").append(role.name()).append(" [").append(phase).append("]: ")
                        .append(coordinateCombatAnimation(role, phase, emotionalState)).append("\n");
            }
        }
        return sequence.toString();
    }

    public static boolean validateCombatAnimation(final CompanionEntity entity, final CombatPhase phase) {
        if (entity == null) return false;
        String current = EnhancedCompanionAnimationController.enhancedAnimationKey(entity, false, "combat");
        return current.contains("combat") || current.contains("defensive");
    }
}
