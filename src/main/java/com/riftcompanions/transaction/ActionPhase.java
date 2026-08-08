package com.riftcompanions.transaction;

/** A monotonic transaction state; recovery never guesses a missing commit. */
public enum ActionPhase {
    PREPARED,
    RESERVED,
    APPLIED,
    COMMITTED,
    ROLLED_BACK,
    EXPIRED,
    FAILED;

    public boolean terminal() {
        return this == COMMITTED || this == ROLLED_BACK || this == EXPIRED || this == FAILED;
    }
}
