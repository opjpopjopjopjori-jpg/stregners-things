package com.riftcompanions.navigation;

/** Compact outcome for entity state logic; no client-side navigation prediction is involved. */
public enum NavigationOutcome {
    MOVING,
    ARRIVED,
    WAITING_FOR_REPATH,
    RETRYING_ALTERNATE,
    TARGET_REJECTED,
    STUCK
}
