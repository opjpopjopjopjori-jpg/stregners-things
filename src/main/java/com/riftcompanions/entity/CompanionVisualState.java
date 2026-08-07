package com.riftcompanions.entity;

/**
 * Read-only presentation state derived from authoritative AI state and visual
 * action. Animation never writes this state or applies an ability effect.
 */
public enum CompanionVisualState {
    IDLE_CALM,
    FOLLOW,
    GUARD,
    OBSERVE,
    ALERT,
    COMBAT_MELEE,
    COMBAT_RANGED,
    RETREAT,
    DOWNED,
    RECOVERY,
    POWER_FOCUS,
    POINT_ROUTE,
    INTERACT_ANCHOR
}
