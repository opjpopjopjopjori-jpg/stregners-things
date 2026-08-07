package com.riftcompanions.duo;

/** Tactical clarity signals, never damage multipliers or an automatic combo attack. */
public enum DuoSynergy {
    GUARD_SHIELD_WINDOW(TeamPair.GUARDIAN_GIFTED, "Guardian created a safe Shield window."),
    DISRUPT_ROUTE_WINDOW(TeamPair.SEER_SCOUT, "Seer disruption opened a short route window."),
    MARKED_RETREAT_PATH(TeamPair.GUARDIAN_SCOUT, "Scout route marker supports Guardian retreat spacing."),
    CLUE_PROTECTED_ENTRY(TeamPair.GUARDIAN_SEER, "Seer evidence supports a guarded entry decision."),
    HIVE_PROTECTION_WINDOW(TeamPair.SEER_GIFTED, "Seer evidence supports a limited protective response."),
    ROUTE_RESCUE_WINDOW(TeamPair.GIFTED_SCOUT, "Scout route and Gifted rescue remain separate, bounded options.");

    private final TeamPair pair;
    private final String summary;

    DuoSynergy(final TeamPair pair, final String summary) {
        this.pair = pair;
        this.summary = summary;
    }

    public TeamPair pair() { return pair; }
    public String summary() { return summary; }
}
