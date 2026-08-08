package com.riftcompanions.team.events;

/** Explicit events only; no vague "something happened" trigger exists. */
public enum TeamEventType {
    PLAYER_HEALTH_CRITICAL(TeamEventPriority.P0_FATAL_EMERGENCY, 80L),
    PLAYER_FALL_RISK(TeamEventPriority.P0_FATAL_EMERGENCY, 40L),
    COMPANION_DOWNED(TeamEventPriority.P0_FATAL_EMERGENCY, 200L),
    EXPLOSIVE_NEAR_PROTECTED_AREA(TeamEventPriority.P1_IMMEDIATE_COMBAT, 100L),
    HOSTILE_CROWD(TeamEventPriority.P1_IMMEDIATE_COMBAT, 100L),
    ROUTE_BLOCKED(TeamEventPriority.P2_PLAN_STATE, 160L),
    PLAN_CANCELLED(TeamEventPriority.P2_PLAN_STATE, 80L),
    POWER_UNAVAILABLE(TeamEventPriority.P2_PLAN_STATE, 120L),
    UNKNOWN_MOB_FIRST_SEEN(TeamEventPriority.P3_DISCOVERY, 180L),
    HIVE_LINKED_TARGET_DETECTED(TeamEventPriority.P2_PLAN_STATE, 100L),
    STRUCTURE_REQUESTED(TeamEventPriority.P3_DISCOVERY, 220L),
    NIGHT_APPROACHING(TeamEventPriority.P3_DISCOVERY, 300L),
    BIOME_RISK_CHANGED(TeamEventPriority.P3_DISCOVERY, 300L),
    BASE_RETURN_AFTER_CRISIS(TeamEventPriority.P4_MEMORY_AMBIENT, 400L),
    MEMORY_LANDMARK_REVISITED(TeamEventPriority.P4_MEMORY_AMBIENT, 600L);

    private final TeamEventPriority priority;
    private final long defaultLifetime;

    TeamEventType(final TeamEventPriority priority, final long defaultLifetime) {
        this.priority = priority;
        this.defaultLifetime = defaultLifetime;
    }

    public TeamEventPriority priority() {
        return priority;
    }

    public long defaultLifetime() {
        return defaultLifetime;
    }
}
