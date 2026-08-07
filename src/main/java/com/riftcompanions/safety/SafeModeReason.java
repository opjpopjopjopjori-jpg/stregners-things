package com.riftcompanions.safety;

/** Reasons exposed in the Journal and diagnostics; stack traces remain in logs. */
public enum SafeModeReason {
    MANUAL,
    PLAYER_STOPPED_ACTIONS,
    DATA_MIGRATION_FAILED,
    RECOVERY_INCOMPLETE_ACTION,
    INVALID_WORLD_STATE,
    REPEATED_COMPANION_EXCEPTION,
    PLAYER_REVIEW_REQUIRED
}
