package com.riftcompanions.transaction;

/**
 * Server-owned actions that change persistent or scarce state. Every type is
 * recorded in the bounded ledger before a service reports success.
 */
public enum ActionType {
    COMPANION_SPAWN,
    COMPANION_RECALL,
    COMPANION_DISMISS,
    COMPANION_REST,
    COMPANION_RESTORE,
    SQUAD_SWITCH,
    INVENTORY_GIVE,
    INVENTORY_WITHDRAW,
    WORLD_ITEM_PICKUP,
    TEAM_SUPPLY_WITHDRAW,
    PLAN_CREATE,
    PLAN_ACTIVATE,
    PLAN_FINISH,
    POWER_CAST,
    RESCUE_BEGIN,
    RESCUE_COMPLETE,
    INTENTION_COMPLETE,
    JOURNAL_MILESTONE,
    COMPANION_UNLOCK
}
