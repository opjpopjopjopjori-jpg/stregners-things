package com.riftcompanions.entity;

/**
 * Professional Character Intelligence Engine — gives each of the 4 roles
 * unique cognitive patterns: Seer (analysis, observation, prediction),
 * Guardian (defense, tactical assessment, group protection),
 * Gifted (magical awareness, boundary assessment, emotional sensing),
 * Scout (navigation, pattern recognition, route memory).
 */
public final class CharacterIntelligenceEngine {
    private CharacterIntelligenceEngine() {}

    public static String seerThink(final String event, final String emotionalState) {
        return "Seer analysis: " + event + " — pattern identified — emotional state: " + emotionalState +
               " — prediction: requires observation before action — hive sense confirms anomaly.";
    }

    public static String guardianThink(final String event, final String emotionalState) {
        return "Guardian assessment: " + event + " — tactical evaluation: defensive priority — emotional: " + emotionalState +
               " — recommendation: protect group first, then respond — brace if needed.";
    }

    public static String giftedThink(final String event, final String emotionalState) {
        return "Gifted awareness: " + event + " — magical reading: energy level " + emotionalState +
               " — boundary assessment: protected zone status — ability readiness: conditional.";
    }

    public static String scoutThink(final String event, final String emotionalState) {
        return "Scout navigation: " + event + " — route analysis: safe/unsafe — emotional: " + emotionalState +
               " — pattern memory: previous similar events — signal placement: strategic.";
    }

    public static String buildIntelligenceProfile(final CompanionRole role) {
        return switch (role) {
            case SEER -> "Analysis + Observation + Prediction + Hive Sense";
            case GUARDIAN -> "Defense + Tactical Assessment + Group Protection + Brace";
            case GIFTED -> "Magical Awareness + Boundary Assessment + Emotional Sensing + Shield/Push";
            case SCOUT -> "Navigation + Pattern Recognition + Route Memory + Signal/Anchor";
        };
    }
}
