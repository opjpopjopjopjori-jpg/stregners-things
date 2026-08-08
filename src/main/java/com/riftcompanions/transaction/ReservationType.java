package com.riftcompanions.transaction;

/** Shared facts that need a short-lived exclusive owner. */
public enum ReservationType {
    RESCUE_TARGET,
    TARGET,
    ROUTE,
    SPEAKER,
    ITEM_ENTITY,
    ANCHOR,
    PLAN_TARGET,
    SQUAD_SWITCH
}
