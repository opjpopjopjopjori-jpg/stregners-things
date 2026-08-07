package com.riftcompanions.navigation;

import net.minecraft.core.BlockPos;

/** Small owner-only developer projection of an existing bounded navigation task. */
public record NavigationDebugSnapshot(
        String intent,
        BlockPos requestedTarget,
        BlockPos selectedTarget,
        int recoveryAttempts,
        long lastProgressAt,
        long nextRepathAt,
        String outcome,
        String reasonCode
) {
    public static NavigationDebugSnapshot empty() {
        return new NavigationDebugSnapshot("NONE", null, null, 0, 0L, 0L, "NONE", "NO_ACTIVE_PATH");
    }
}
