package com.riftcompanions.safety;

import com.riftcompanions.RiftCompanions;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.server.TeamSavedData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Circuit breaker for repeat failures in companion-owned server logic. A player
 * sees a short Safe Mode reason, while the detailed throwable remains in the
 * local log for diagnosis.
 */
public final class CompanionFaultService {
    private static final long FAULT_WINDOW_TICKS = 1200L;
    private static final int SAFE_MODE_THRESHOLD = 3;

    private CompanionFaultService() {}

    public static void recordException(final CompanionEntity companion, final RuntimeException exception) {
        if (companion == null || companion.level().isClientSide) return;
        final ServerPlayer owner = companion.getOwnerPlayer().orElse(null);
        if (owner == null || owner.server == null) return;
        final String code = "EXCEPTION_" + exception.getClass().getSimpleName();
        final int count = record(owner, companion.getRole(), companion.getRole().id(), owner.level().getGameTime(), code);
        RiftCompanions.LOGGER.error("Companion {} fault {} (count {} in safety window)", companion.getRole().id(), code, count, exception);
        companion.getNavigation().stop();
        companion.setTarget(null);
        if (companion.getCompanionState() != CompanionState.DOWNED) {
            companion.setCompanionState(CompanionState.HOLDING, "FAULT_CIRCUIT_BREAKER_" + code);
        }
        if (count >= SAFE_MODE_THRESHOLD) {
            SafeModeService.enable(owner, SafeModeReason.REPEATED_COMPANION_EXCEPTION,
                    companion.getRole().personalName() + " encountered repeated internal errors. Review the local diagnostic report.");
        }
    }

    public static void recordInvalidWorldState(final ServerPlayer owner, final CompanionRole role, final String code) {
        if (owner == null || owner.server == null || role == null) return;
        final int count = record(owner, role, role.id(), owner.level().getGameTime(), "WORLD_" + code);
        if (count >= SAFE_MODE_THRESHOLD) {
            SafeModeService.enable(owner, SafeModeReason.INVALID_WORLD_STATE,
                    role.personalName() + " repeatedly encountered an invalid world state: " + code + ".");
        }
    }

    private static int record(final ServerPlayer owner, final CompanionRole role, final String identity,
                              final long now, final String code) {
        final TeamSavedData data = TeamSavedData.get(owner.server);
        final int count = data.blackboard(owner.getUUID()).recordFault(role, identity, now, code, FAULT_WINDOW_TICKS);
        data.markChanged();
        return count;
    }
}
