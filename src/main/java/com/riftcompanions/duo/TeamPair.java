package com.riftcompanions.duo;

import com.riftcompanions.entity.CompanionRole;

import java.util.Arrays;
import java.util.Optional;

/** The six two-role configurations. Pair order never changes its identity. */
public enum TeamPair {
    GUARDIAN_SEER(CompanionRole.GUARDIAN, CompanionRole.SEER, "Safety and Mystery", "Caves, unknown structures, careful investigation", "Less direct damage and fewer route alternatives"),
    GUARDIAN_GIFTED(CompanionRole.GUARDIAN, CompanionRole.GIFTED, "Protection and Rescue", "Defence, emergencies, controlled retreats", "Less anomaly evidence and fewer route alternatives"),
    GUARDIAN_SCOUT(CompanionRole.GUARDIAN, CompanionRole.SCOUT, "Guardian and Scout", "Travel, mountains, forests, practical survival", "No Hive sensing or power rescue"),
    SEER_GIFTED(CompanionRole.SEER, CompanionRole.GIFTED, "Anomaly and Power", "Supported Hive and mystery content", "No Guardian frontline"),
    SEER_SCOUT(CompanionRole.SEER, CompanionRole.SCOUT, "Observation and Escape", "Unknown biomes, clues, route testing", "Less direct protection and no power rescue"),
    GIFTED_SCOUT(CompanionRole.GIFTED, CompanionRole.SCOUT, "Mobility and Recovery", "Routes, flexible exploration, emergency exits", "No Guardian hold line or deep anomaly expertise");

    private final CompanionRole first;
    private final CompanionRole second;
    private final String identity;
    private final String recommendedContexts;
    private final String blindSpot;

    TeamPair(final CompanionRole first, final CompanionRole second, final String identity,
             final String recommendedContexts, final String blindSpot) {
        this.first = first;
        this.second = second;
        this.identity = identity;
        this.recommendedContexts = recommendedContexts;
        this.blindSpot = blindSpot;
    }

    public CompanionRole first() { return first; }
    public CompanionRole second() { return second; }
    public String identity() { return identity; }
    public String recommendedContexts() { return recommendedContexts; }
    public String blindSpot() { return blindSpot; }
    public boolean includes(final CompanionRole role) { return role == first || role == second; }
    public String dialogueTrigger() { return "duo_" + name().toLowerCase(java.util.Locale.ROOT); }

    public static Optional<TeamPair> of(final CompanionRole one, final CompanionRole two) {
        if (one == null || two == null || one == two) return Optional.empty();
        return Arrays.stream(values()).filter(pair -> pair.includes(one) && pair.includes(two)).findFirst();
    }
}
