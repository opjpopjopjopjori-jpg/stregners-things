package com.riftcompanions.server;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.debug.DecisionTraceService;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionLifecycle;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.navigation.CompanionNavigationService;
import com.riftcompanions.safety.SafeModeReason;
import com.riftcompanions.safety.SafeModeService;
import com.riftcompanions.team.TeamPlanService;
import net.minecraft.server.level.ServerPlayer;

/**
 * Explicit player emergency controls. These are narrower than a hidden AI
 * override: they cancel work safely and leave the owner with visible Follow,
 * Recall, Journal, and Resume controls.
 */
public final class TeamSafetyControlService {
    private TeamSafetyControlService() {}

    /** STOP ALL ACTIONS is implemented as persisted manual Safe Mode. */
    public static Result stopAllActions(final ServerPlayer player) {
        if (player == null || player.server == null) return Result.failure("NO_SERVER", "No logical server is available.");
        final var safeMode = SafeModeService.enable(player, SafeModeReason.PLAYER_STOPPED_ACTIONS,
                "Player selected Stop All Actions. Plans, powers, automatic combat, and optional initiative were isolated.");
        return safeMode.successful()
                ? Result.success("STOP_ALL_ACTIONS_ACTIVE", "All optional companion actions stopped safely. Use Resume after reviewing the Journal.")
                : Result.failure(safeMode.code(), safeMode.detail());
    }

    /**
     * Cancels one companion's non-durable local task without inventing a path,
     * entity, item, or unsafe teleport. A plan led by that role is cancelled to
     * keep the team board coherent.
     */
    public static Result resetTask(final ServerPlayer player, final CompanionRole role) {
        if (player == null || player.server == null || role == null) {
            return Result.failure("INVALID_RESET_TASK_REQUEST", "Choose a valid loaded companion role.");
        }
        final CompanionEntity companion = CompanionLifecycleService.findForOwner(player, role).orElse(null);
        if (companion == null) return Result.failure("NO_LOADED_COMPANION", role.personalName() + " is not currently loaded.");
        final var board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        if (board.plan().isOpen() && board.plan().leader() == role) {
            TeamPlanService.cancel(player);
        }
        companion.getNavigation().stop();
        companion.clearFormationSlot("PLAYER_RESET_TASK");
        CompanionNavigationService.clearEntity(companion.getUUID());
        companion.setTarget(null);
        board.clearFocusTarget();
        if (companion.level() != player.level()) {
            if (companion.getCompanionState() != CompanionState.DOWNED) {
                companion.setCompanionState(CompanionState.HOLDING, "PLAYER_RESET_TASK_DIMENSION_MISMATCH");
            }
            TeamSavedData.get(player.server).markChanged();
            DecisionTraceService.log(player, "RESET_TASK", role.id() + " -> DIMENSION_MISMATCH_SAFE_HOLD");
            return Result.success("TASK_RESET_DIMENSION_DEFERRED", role.personalName()
                    + " is in another dimension and was held safely. No cross-dimension transfer was attempted.");
        }
        String fallback = "RESET_TO_FOLLOW";
        if (companion.getCompanionState() != CompanionState.DOWNED) {
            if (SafeTeleport.isSafeStanding(player.serverLevel(), companion, companion.blockPosition())) {
                companion.setCompanionState(CompanionState.FOLLOWING, "PLAYER_RESET_TASK");
            } else if (CompanionConfig.SAFE_RECALL_ENABLED.get() && SafeTeleport.recallNearPlayer(companion, player, 5)) {
                companion.setCompanionState(CompanionState.FOLLOWING, "PLAYER_RESET_TASK_SAFE_RECALL");
                fallback = "RESET_TO_FOLLOW_AFTER_SAFE_RECALL";
            } else {
                companion.setCompanionState(CompanionState.HOLDING, "PLAYER_RESET_TASK_NO_SAFE_RECALL");
                fallback = "RESET_TO_SAFE_HOLD";
            }
        } else {
            fallback = "DOWNED_STATE_PRESERVED";
        }
        TeamSavedData.get(player.server).markChanged();
        DecisionTraceService.log(player, "RESET_TASK", role.id() + " -> " + fallback);
        return Result.success("TASK_RESET", role.personalName() + " task reset: " + fallback + ".");
    }

    /**
     * Explicit pre-uninstall preparation. It only handles roster entries that
     * already exist; it never creates an unavailable role just to dismiss it.
     */
    public static Result dismissAllExisting(final ServerPlayer player) {
        if (player == null || player.server == null) return Result.failure("NO_SERVER", "No logical server is available.");
        final TeamSavedData data = TeamSavedData.get(player.server);
        int dismissed = 0;
        int downedDeferred = 0;
        for (final CompanionRole role : CompanionRole.values()) {
            final var entry = data.entry(player.getUUID(), role).orElse(null);
            if (entry == null || entry.lifecycle() == CompanionLifecycle.UNAVAILABLE || entry.lifecycle() == CompanionLifecycle.DISMISSED) continue;
            if (entry.lifecycle() == CompanionLifecycle.DOWNED) {
                // Never discard a downed live entity through an unverified
                // uninstall helper. Resolve it explicitly first, then rerun.
                downedDeferred++;
                continue;
            }
            CompanionLifecycleService.dismiss(player, role);
            dismissed++;
        }
        DecisionTraceService.log(player, "DISMISS_ALL", "Dismissed=" + dismissed + " downed_deferred=" + downedDeferred);
        final String detail = "Dismissed " + dismissed + " companion roster entry or entries."
                + (downedDeferred > 0 ? " " + downedDeferred + " downed entry or entries were left untouched; resolve them before removal." : "")
                + " Review inventory and make a world backup before removing the mod.";
        return Result.success("DISMISS_ALL_COMPLETE", detail);
    }

    public record Result(boolean successful, String code, String detail) {
        public static Result success(final String code, final String detail) { return new Result(true, code, detail); }
        public static Result failure(final String code, final String detail) { return new Result(false, code, detail); }
    }
}
