package com.riftcompanions.entity;

/** Persistent roster lifecycle; distinct from the moment-to-moment AI state. */
public enum CompanionLifecycle {
    UNAVAILABLE,
    AVAILABLE,
    ACTIVE,
    DOWNED,
    RESTING,
    DISMISSED,
    RECOVERY
}
