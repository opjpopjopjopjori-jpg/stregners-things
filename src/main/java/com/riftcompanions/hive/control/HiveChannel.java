package com.riftcompanions.hive.control;

import java.util.List;
import java.util.UUID;

/**
 * Pending Will focus. Target UUIDs and the heart budget are captured before
 * release; costs are still charged only after the server revalidates a target.
 */
public record HiveChannel(
        UUID ownerUuid,
        UUID seerUuid,
        HiveControlMode mode,
        List<UUID> targetUuids,
        long releaseAt,
        int controlCapacityHearts,
        int targetLimit,
        boolean surge
) {
    public HiveChannel {
        targetUuids = targetUuids == null ? List.of() : List.copyOf(targetUuids);
        controlCapacityHearts = Math.max(1, controlCapacityHearts);
        targetLimit = Math.max(1, targetLimit);
    }

    public WillControlBudget.ControlBudget capturedBudget() {
        return new WillControlBudget.ControlBudget(controlCapacityHearts, targetLimit);
    }
}
