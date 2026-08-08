package com.riftcompanions.mode;

import com.riftcompanions.config.CompanionConfig;

/** Small centralized performance decisions; no external optimizer is required. */
public final class PerformancePolicy {
    private PerformancePolicy() {}

    public static PerformanceTier tier() { return CompanionConfig.PERFORMANCE_TIER.get(); }
    public static long scaledInterval(long standard) {
        return switch (tier()) {
            case LIGHT -> Math.max(standard, standard * 2L);
            case STANDARD -> standard;
            case CINEMATIC -> Math.max(10L, standard / 2L);
        };
    }
    public static boolean allowsAmbientScenes() { return tier() != PerformanceTier.LIGHT; }
    public static boolean allowsInventoryAssist() { return tier() != PerformanceTier.LIGHT; }
}
