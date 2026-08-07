package com.riftcompanions.power;

/** Player-selected policy. Explicit UI/command use counts as ASK_FIRST approval. */
public enum PowerPolicy {
    OFF,
    SENSE_ONLY,
    RESCUE_ONLY,
    ASK_FIRST,
    EMERGENCY_ONLY,
    ALLOWED;

    public boolean permitsExplicitNonEmergency() {
        return this == ASK_FIRST || this == ALLOWED;
    }

    public boolean permitsEmergency() {
        return this == RESCUE_ONLY || this == ASK_FIRST || this == EMERGENCY_ONLY || this == ALLOWED;
    }
}
