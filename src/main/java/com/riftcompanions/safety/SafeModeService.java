package com.riftcompanions.safety;

import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.formation.FormationType;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamBlackboard;
import com.riftcompanions.team.TeamDirector;
import com.riftcompanions.team.TeamPlanStatus;
import net.minecraft.server.level.ServerPlayer;

/**
 * Safe Mode preserves Follow, Safe Recall and the Journal while isolating
 * risky optional systems. It is per-player even in an integrated server.
 */
public final class SafeModeService {
    private SafeModeService() {}

    public static boolean enabled(final ServerPlayer player) {
        return player != null && TeamSavedData.get(player.server).blackboard(player.getUUID()).safeMode().enabled();
    }

    public static SafeModeResult enable(final ServerPlayer player, final SafeModeReason reason, final String detail) {
        if (player == null || player.server == null) return SafeModeResult.failure("NO_SERVER", "No logical server is available.");
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        final long now = player.level().getGameTime();
        board.safeMode().enable(reason, detail, now);
        board.clearFocusTarget();
        TeamDirector.clearSession(player.getUUID()); // discard stale low-priority events; new critical events can be observed after review.
        board.clearReservations();
        board.actionLedger().recoverIncomplete(now, "SAFE_MODE_RECOVERY");
        if (board.plan().isOpen()) {
            final var interruptedPlan = board.plan();
            board.completePlan(TeamPlanStatus.FAILED_SAFE);
            com.riftcompanions.behavior.GuardianBehaviorService.recordAfterAction(player, interruptedPlan,
                    TeamPlanStatus.FAILED_SAFE, "SAFE_MODE_INTERRUPTED_PLAN");
        }
        board.setFormation(FormationType.FOLLOW);
        com.riftcompanions.hive.control.HiveChannelManager.cancelForSafety(player, "Safe Mode cancelled the Hive focus safely.");
        com.riftcompanions.server.GuardianBraceService.cancelForOwner(player, "SAFE_MODE_PROTECTION_CANCELLED");
        int isolated = 0;
        for (final CompanionRole role : CompanionRole.values()) {
            final var companion = CompanionLifecycleService.findForOwner(player, role);
            if (companion.isEmpty()) continue;
            companion.get().getNavigation().stop();
            companion.get().setTarget(null);
            companion.get().disableActiveProtection("SAFE_MODE_PROTECTION_CANCELLED");
            companion.get().setCombatEnabled(false);
            if (companion.get().getCompanionState() != CompanionState.DOWNED) {
                companion.get().setCompanionState(CompanionState.FOLLOWING, "SAFE_MODE_" + reason.name());
            }
            isolated++;
        }
        data.markChanged();
        com.riftcompanions.server.CompanionPresentationSoundService.playSafeMode(player);
        com.riftcompanions.onboarding.OnboardingService.onSafeMode(player);
        return SafeModeResult.success("SAFE_MODE_ENABLED", "Safe Mode is active for " + isolated
                + " companion(s): " + board.safeMode().reason() + ". Follow, Recall, and Journal remain available.");
    }

    public static SafeModeResult clear(final ServerPlayer player) {
        if (player == null || player.server == null) return SafeModeResult.failure("NO_SERVER", "No logical server is available.");
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        if (!board.safeMode().enabled()) return SafeModeResult.failure("SAFE_MODE_NOT_ACTIVE", "Safe Mode is not currently active.");
        board.safeMode().clear();
        data.markChanged();
        return SafeModeResult.success("SAFE_MODE_CLEARED", "Safe Mode was cleared. Optional systems remain subject to their normal safety checks.");
    }

    public record SafeModeResult(boolean successful, String code, String detail) {
        public static SafeModeResult success(final String code, final String detail) { return new SafeModeResult(true, code, detail); }
        public static SafeModeResult failure(final String code, final String detail) { return new SafeModeResult(false, code, detail); }
    }
}
