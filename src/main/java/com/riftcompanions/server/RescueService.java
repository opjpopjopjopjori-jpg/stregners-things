package com.riftcompanions.server;

import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.transaction.ActionPhase;
import com.riftcompanions.transaction.ActionTransaction;
import com.riftcompanions.transaction.ActionType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;

import java.util.Comparator;
import java.util.Optional;

/**
 * Coordinates the player-led DOWNED rescue transaction. This never teleports a
 * rescuer into danger and never turns a downed companion into invulnerable bait.
 */
public final class RescueService {
    private RescueService() {}

    public static RescueResult beginNearestPlayerRescue(final ServerPlayer player) {
        Optional<CompanionEntity> nearest = java.util.Arrays.stream(CompanionRole.values())
                .map(role -> CompanionLifecycleService.findForOwner(player, role))
                .flatMap(Optional::stream)
                .filter(companion -> companion.getCompanionState() == CompanionState.DOWNED)
                .max(Comparator.comparingInt(companion -> RescuePriorityService.score(player, companion)));
        return nearest.map(companion -> beginPlayerRescue(player, companion))
                .orElseGet(() -> RescueResult.failure("NO_DOWNED_COMPANION", "No downed companion is nearby to rescue."));
    }

    public static RescueResult beginPlayerRescue(final ServerPlayer player, final CompanionEntity companion) {
        if (companion == null || companion.getCompanionState() != CompanionState.DOWNED) {
            return RescueResult.failure("TARGET_NOT_DOWNED", "This companion does not need rescue right now.");
        }
        if (!companion.isOwnedBy(player)) {
            return RescueResult.failure("NOT_TEAM_OWNER", "You do not have permission to rescue this companion.");
        }
        if (player.distanceToSqr(companion) > 4.5D * 4.5D) {
            return RescueResult.failure("RESCUE_TOO_FAR", "Move closer to the downed companion to begin the rescue.");
        }
        if (!isRescueAreaSafe(player, companion)) {
            return RescueResult.failure("RESCUE_AREA_UNSAFE", "The area is too dangerous. Create space or start a retreat first.");
        }
        final TeamSavedData data = TeamSavedData.get(player.server);
        final var board = data.blackboard(player.getUUID());
        final long now = player.level().getGameTime();
        final var transactionBegin = board.actionLedger().begin(ActionType.RESCUE_BEGIN,
                "rescue:" + companion.getUUID(), companion.getRole().id(), now, 120L);
        if (transactionBegin.reused()) return RescueResult.failure("RESCUE_ACTION_ALREADY_PENDING", "A rescue action is already pending for this companion.");
        final ActionTransaction transaction = transactionBegin.transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("RESCUE_TARGET_VALIDATED");
        if (!board.claimRescue(companion.getUUID(), transaction.actionId(), now, 100L)) {
            transaction.rollback(now, "ANOTHER_RESCUE_RESERVED");
            data.markChanged();
            return RescueResult.failure("ANOTHER_RESCUE_RESERVED", "The team is already coordinating another rescue first.");
        }
        if (!companion.beginPlayerRescue(player)) {
            board.releaseRescue(companion.getUUID());
            transaction.rollback(now, "RESCUE_REJECTED");
            data.markChanged();
            return RescueResult.failure("RESCUE_REJECTED", "The rescue could not start in the current situation.");
        }
        if (transaction.phase() == ActionPhase.RESERVED) transaction.applied("RESCUE_PROGRESS_STARTED");
        if (transaction.phase() == ActionPhase.APPLIED) transaction.commit(now, "RESCUE_BEGIN_CONFIRMED");
        data.markChanged();
        return RescueResult.success("RESCUE_STARTED", "Rescue started. Stay close for three seconds and avoid danger.");
    }

    /** A small local query only; no broad scan or chunk load happens during a rescue tick. */
    public static boolean isRescueAreaSafe(final ServerPlayer player, final CompanionEntity companion) {
        if (player.level() != companion.level() || player.isInLava() || player.isOnFire()) {
            return false;
        }
        final boolean threatAtRescuer = !player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(3.0D)).isEmpty();
        final boolean threatAtCompanion = !companion.level().getEntitiesOfClass(Monster.class, companion.getBoundingBox().inflate(3.0D)).isEmpty();
        return !threatAtRescuer && !threatAtCompanion;
    }

    /** Called after the companion's own postcondition checks have revived it. */
    public static void recordCompletion(final ServerPlayer player, final CompanionEntity companion) {
        if (player == null || companion == null) return;
        final TeamSavedData data = TeamSavedData.get(player.server);
        final long now = player.level().getGameTime();
        final var transactionBegin = data.blackboard(player.getUUID()).actionLedger().begin(ActionType.RESCUE_COMPLETE,
                "rescue-complete:" + companion.getUUID() + ":" + now, companion.getRole().id(), now, 120L);
        if (transactionBegin.reused()) return;
        final ActionTransaction transaction = transactionBegin.transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("REVIVE_POSTCONDITION_VALIDATED");
        if (transaction.phase() == ActionPhase.RESERVED) transaction.applied("COMPANION_REVIVED");
        if (transaction.phase() == ActionPhase.APPLIED) transaction.commit(now, "RESCUE_COMPLETED");
        data.markChanged();
    }

    public record RescueResult(boolean successful, String code, String detail) {
        public static RescueResult success(final String code, final String detail) {
            return new RescueResult(true, code, detail);
        }

        public static RescueResult failure(final String code, final String detail) {
            return new RescueResult(false, code, detail);
        }
    }
}
