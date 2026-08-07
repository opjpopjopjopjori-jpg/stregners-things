package com.riftcompanions.persistence;

import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.formation.FormationType;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.safety.SafeModeReason;
import com.riftcompanions.safety.SafeModeService;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamBlackboard;
import com.riftcompanions.team.TeamPlanStatus;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs before login recap/status presentation. It only cancels unconfirmed work
 * or moves it to a safe state; it never invents an item, target, memory, or
 * successful action after a reload.
 */
public final class SaveRecoveryService {
    private SaveRecoveryService() {}

    public static RecoveryReport recoverAfterLogin(final ServerPlayer player) {
        if (player == null || player.server == null) return RecoveryReport.empty();
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        // Guardian Brace is deliberately transient and never resumes as a
        // guessed protection state after Save/Load.
        com.riftcompanions.server.GuardianBraceService.cancelForOwner(player, "LOAD_RECOVERY");
        final long now = player.level().getGameTime();
        final List<String> issues = new ArrayList<>();
        if (data.hasMigrationFailure()) {
            issues.add("Data migration requires review: " + data.migrationFailureDetail());
            issues.add(MigrationBackupAdvisory.write(player, data.migrationFailureDetail()));
        }

        final int incomplete = board.actionLedger().recoverIncomplete(now, "LOAD_ROLLBACK_UNCONFIRMED_ACTION");
        if (incomplete > 0) issues.add(incomplete + " unconfirmed action(s) rolled back.");
        // A player-rescue progress loop cannot be trusted across logout; it is
        // always restarted by an explicit nearby player action after load.
        board.releaseRescue(null);
        final int expiredReservations = board.reservations().releaseExpired(now);
        if (expiredReservations > 0) issues.add(expiredReservations + " expired reservation(s) released.");
        final int invalidReservations = board.reservations().releaseMissingOrUncommitted(board.actionLedger());
        if (invalidReservations > 0) {
            board.releaseRescue(null);
            issues.add(invalidReservations + " reservation(s) without a committed owner released.");
        }

        if (board.plan().isOpen() && (board.plan().timedOut(now) || !board.plan().hasValidTarget(player) || !hasLoadedPlanLeader(player, board))) {
            final var invalidPlan = board.plan();
            board.completePlan(TeamPlanStatus.FAILED_SAFE);
            com.riftcompanions.behavior.GuardianBehaviorService.recordAfterAction(player, invalidPlan,
                    TeamPlanStatus.FAILED_SAFE, "LOAD_PLAN_VALIDATION_FAILED");
            board.setFormation(FormationType.FOLLOW);
            issues.add("An invalid or expired plan was changed to FAILED_SAFE.");
        }
        for (final CompanionRole role : CompanionRole.values()) {
            CompanionLifecycleService.findForOwner(player, role).ifPresent(companion -> {
                final String issue = companion.recoverAfterLoad(player);
                if (!"RECOVERY_CLEAR".equals(issue)) issues.add(role.id() + ": " + issue);
            });
            data.entry(player.getUUID(), role).ifPresent(entry -> {
                if (entry.lifecycle() == com.riftcompanions.entity.CompanionLifecycle.RESTING) {
                    final var snapshot = entry.restingSnapshot().orElse(null);
                    if (snapshot == null || !snapshot.contains("RestingDimension")) {
                        issues.add(role.id() + ": RESTING_SNAPSHOT_MISSING_OR_LEGACY");
                    }
                }
            });
        }

        if (!issues.isEmpty()) {
            board.addMemory(new MemoryRecord(MemoryType.RECOVERY, now / 24000L,
                    "Save recovery: " + String.join(" ", issues).substring(0, Math.min(220, String.join(" ", issues).length())), 95));
            SafeModeService.enable(player, data.hasMigrationFailure() ? SafeModeReason.DATA_MIGRATION_FAILED : SafeModeReason.RECOVERY_INCOMPLETE_ACTION,
                    String.join(" ", issues));
        }
        data.markChanged();
        return new RecoveryReport(List.copyOf(issues), board.safeMode().enabled(), board.safeMode().detail());
    }

    private static boolean hasLoadedPlanLeader(final ServerPlayer player, final TeamBlackboard board) {
        if (!board.plan().isOpen()) return true;
        return CompanionLifecycleService.findForOwner(player, board.plan().leader())
                .filter(companion -> companion.getCompanionState() != CompanionState.DOWNED)
                .isPresent();
    }

    public record RecoveryReport(List<String> issues, boolean safeModeEnabled, String safeModeDetail) {
        public static RecoveryReport empty() { return new RecoveryReport(List.of(), false, ""); }
        public boolean changed() { return !issues.isEmpty(); }
    }
}
