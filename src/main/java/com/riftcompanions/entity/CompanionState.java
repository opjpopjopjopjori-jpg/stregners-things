package com.riftcompanions.entity;

/**
 * Every state has a recovery route.  No state is allowed to run forever:
 * CompanionEntity applies timeouts and returns to FOLLOW/IDLE when necessary.
 */
public enum CompanionState {
    IDLE,
    FOLLOWING,
    HOLDING,
    GUARDING,
    OBSERVING,
    SCOUTING,
    FIGHTING,
    RETREATING,
    RETURNING_HOME,
    BASE_ACTIVITY,
    RECOVERING,
    EXHAUSTED,
    DOWNED,
    RESTING,
    STUCK_RECOVERY;

    public boolean permitsNavigation() {
        return this != HOLDING && this != DOWNED && this != RESTING;
    }

    public boolean isEmergency() {
        return this == RETREATING || this == DOWNED || this == STUCK_RECOVERY;
    }
}
