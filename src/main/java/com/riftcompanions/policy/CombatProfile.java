package com.riftcompanions.policy;

public enum CombatProfile {
    DEFENSIVE,
    BALANCED,
    TACTICAL;

    public double pursuitRadius() {
        return switch (this) {
            case DEFENSIVE -> 10.0D;
            case BALANCED -> 16.0D;
            case TACTICAL -> 18.0D;
        };
    }
}
