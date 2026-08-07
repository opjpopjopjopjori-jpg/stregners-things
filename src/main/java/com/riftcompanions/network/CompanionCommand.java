package com.riftcompanions.network;

import java.util.Arrays;
import java.util.Optional;

/** Fixed client->server command vocabulary. No packet can contain arbitrary block/entity actions. */
public enum CompanionCommand {
    CALL_ROLE,
    FOLLOW_ALL,
    REGROUP,
    HOLD_ROLE,
    GUARD_ROLE,
    FOCUS_TARGET,
    RETREAT,
    ACCEPT_PLAN,
    DECLINE_PLAN,
    CHECK_STRUCTURE,
    CANCEL_PLAN,
    RECALL_ROLE,
    RECALL_ALL,
    DISMISS_ROLE,
    COMBAT_ON,
    COMBAT_OFF,
    SAFE_MODE,
    CLEAR_SAFE_MODE,
    STOP_ALL_ACTIONS,
    RESET_TASK,
    DISMISS_ALL,
    ACCEPT_INTENTION,
    DEFER_INTENTION,
    CYCLE_POWER_POLICY,
    RESET_POWER_POLICY,
    SET_HOME_ANCHOR,
    RETURN_HOME,
    REVIVE_NEAREST,
    USE_ABILITY;

    public static Optional<CompanionCommand> byNetworkId(final int id) {
        final CompanionCommand[] values = values();
        return id >= 0 && id < values.length ? Optional.of(values[id]) : Optional.empty();
    }
}
