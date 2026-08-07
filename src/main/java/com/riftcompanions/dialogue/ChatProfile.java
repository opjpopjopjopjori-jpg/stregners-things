package com.riftcompanions.dialogue;

/** Presentation policy only; AI and plans never depend on chat being enabled. */
public enum ChatProfile {
    /** P0 emergency alerts only. HUD, Journal, and command feedback remain readable. */
    CRITICAL_ONLY,
    /** P0/P1 urgent calls only. */
    MINIMAL,
    STANDARD,
    CINEMATIC;

    public boolean allowsPriority(final int priority) {
        return switch (this) {
            case CRITICAL_ONLY -> priority <= 0;
            case MINIMAL -> priority <= 1;
            case STANDARD -> priority <= 3;
            case CINEMATIC -> priority <= 4;
        };
    }
}
