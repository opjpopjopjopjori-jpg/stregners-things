package com.riftcompanions.relationship;

/** Long-term trust is descriptive and never permits unsafe orders or stolen resources. */
public enum RelationshipLevel {
    CAUTIOUS,
    TRUSTING,
    CLOSE,
    FRUSTRATED;

    public static RelationshipLevel fromTrust(final int trust) {
        if (trust < 20) return CAUTIOUS;
        if (trust < 70) return TRUSTING;
        return CLOSE;
    }
}
