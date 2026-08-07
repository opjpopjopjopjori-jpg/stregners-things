package com.riftcompanions.entity;

/**
 * Server-derived, presentation-only head-gaze intent. The body continues to
 * follow its navigation heading; this enum never controls pathfinding, combat,
 * perception, player input, camera, or targeting.
 */
public enum CompanionLookIntent {
    FORWARD,
    GLANCE_LEFT,
    GLANCE_RIGHT,
    CHECK_BACK_LEFT,
    CHECK_BACK_RIGHT;

    public static CompanionLookIntent byId(final int id) {
        final CompanionLookIntent[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, id))];
    }
}
