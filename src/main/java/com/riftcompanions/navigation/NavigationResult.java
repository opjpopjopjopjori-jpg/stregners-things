package com.riftcompanions.navigation;

import net.minecraft.core.BlockPos;

/** Server result with a readable reason code for diagnostics and limited player feedback. */
public record NavigationResult(NavigationOutcome outcome, String reasonCode, BlockPos selectedTarget, int recoveryAttempts) {
    public static NavigationResult moving(final BlockPos target, final int attempts) {
        return new NavigationResult(NavigationOutcome.MOVING, "PATH_ACTIVE", target, attempts);
    }

    public static NavigationResult arrived(final BlockPos target) {
        return new NavigationResult(NavigationOutcome.ARRIVED, "TARGET_REACHED", target, 0);
    }

    public static NavigationResult waiting(final BlockPos target, final int attempts) {
        return new NavigationResult(NavigationOutcome.WAITING_FOR_REPATH, "REPATH_INTERVAL", target, attempts);
    }

    public static NavigationResult retrying(final String reason, final BlockPos target, final int attempts) {
        return new NavigationResult(NavigationOutcome.RETRYING_ALTERNATE, reason, target, attempts);
    }

    public static NavigationResult rejected(final String reason, final int attempts) {
        return new NavigationResult(NavigationOutcome.TARGET_REJECTED, reason, null, attempts);
    }

    public static NavigationResult stuck(final String reason, final BlockPos target, final int attempts) {
        return new NavigationResult(NavigationOutcome.STUCK, reason, target, attempts);
    }
}
