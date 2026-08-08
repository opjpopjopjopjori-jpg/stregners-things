package com.riftcompanions.navigation;

/** High-level purpose passed into the server navigation safety planner. */
public enum NavigationIntent {
    FOLLOW,
    FORMATION,
    GUARD,
    RETREAT,
    RETURN_HOME,
    BASE_ACTIVITY,
    SCOUT,
    STUCK_RECOVERY
}
