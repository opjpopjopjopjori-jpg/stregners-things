package com.riftcompanions.team;

/** Standard team vocabulary shared by UI, plans, dialogue, and diagnostics. */
public enum TeamCondition {
    CALM,
    CAUTION,
    DANGER,
    CRITICAL,
    COLLAPSE;

    public static TeamCondition fromDangerScore(final int score) {
        if (score >= 85) return COLLAPSE;
        if (score >= 65) return CRITICAL;
        if (score >= 40) return DANGER;
        if (score >= 20) return CAUTION;
        return CALM;
    }
}
