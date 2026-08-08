package com.riftcompanions.intention;

/** Optional, deferrable intention state. No state has a hard real-time timer. */
public enum IntentionStatus {
    AVAILABLE,
    ACTIVE,
    DEFERRED,
    COMPLETED,
    EXPIRED;

    public boolean open() { return this == AVAILABLE || this == ACTIVE || this == DEFERRED; }
}
