package com.riftcompanions.animation;

import com.riftcompanions.entity.CompanionRole;

/**
 * Character-Specific Enhanced Animation Profiles — deepest possible animation
 * design per role: Seer (hive/power/observation), Guardian (defense/frontline),
 * Gifted (shield/push/rescue), Scout (route/signal/anchor).
 */
public final class CharacterAnimationProfiles {
    private CharacterAnimationProfiles() {}

    public static String seerAnimation(final String emotionalState, final boolean intense) {
        return "animation.seer." + emotionalState + (intense ? "_intense" : "_calm") +
               "_hive_sense_" + emotionalState + "_eye_glint_" + emotionalState;
    }

    public static String guardianAnimation(final String emotionalState, final boolean combat) {
        return "animation.guardian." + emotionalState + (combat ? "_combat_melee" : "_guard_defensive") +
               "_brace_" + emotionalState;
    }

    public static String giftedAnimation(final String emotionalState, final String ability) {
        return "animation.gifted." + emotionalState + "_" + ability +
               (emotionalState.contains("exhausted") ? "_recovery" : "_focus");
    }

    public static String scoutAnimation(final String emotionalState, final boolean signalActive) {
        return "animation.scout." + emotionalState + (signalActive ? "_signal_active" : "_lookout") +
               "_route_marked_" + emotionalState;
    }

    public static String buildCompleteProfile(final CompanionRole role, final String emotionalState, final boolean intense, final boolean combat) {
        return switch (role) {
            case SEER -> seerAnimation(emotionalState, intense);
            case GUARDIAN -> guardianAnimation(emotionalState, combat);
            case GIFTED -> giftedAnimation(emotionalState, intense ? "shield" : "push");
            case SCOUT -> scoutAnimation(emotionalState, intense);
            default -> "animation.unknown." + emotionalState;
        };
    }
}
