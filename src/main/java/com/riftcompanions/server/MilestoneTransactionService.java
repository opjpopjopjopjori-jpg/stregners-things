package com.riftcompanions.server;

import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.transaction.ActionPhase;
import com.riftcompanions.transaction.ActionTransaction;
import com.riftcompanions.transaction.ActionType;
import net.minecraft.server.level.ServerPlayer;

/** Bounded idempotent path for important journal memories and unlock markers. */
public final class MilestoneTransactionService {
    private MilestoneTransactionService() {}

    public static boolean recordJournalMilestone(final ServerPlayer player, final String stableKey, final MemoryRecord memory) {
        if (player == null || player.server == null || stableKey == null || stableKey.isBlank() || memory == null) return false;
        final TeamSavedData data = TeamSavedData.get(player.server);
        final var board = data.blackboard(player.getUUID());
        final String subject = "journal:" + stableKey;
        if (board.actionLedger().hasCommitted(ActionType.JOURNAL_MILESTONE, subject)) return false;
        final long now = player.level().getGameTime();
        final var begun = board.actionLedger().begin(ActionType.JOURNAL_MILESTONE, subject, memory.type().name(), now, 24000L);
        if (begun.reused()) return false;
        final ActionTransaction transaction = begun.transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("JOURNAL_MILESTONE_VALIDATED");
        board.addMemory(memory);
        if (transaction.phase() == ActionPhase.RESERVED) transaction.applied("JOURNAL_MEMORY_WRITTEN");
        if (transaction.phase() == ActionPhase.APPLIED) transaction.commit(now, "JOURNAL_MILESTONE_COMMITTED");
        data.markChanged();
        return true;
    }

    public static boolean recordUnlock(final ServerPlayer player, final String stableKey) {
        if (player == null || player.server == null || stableKey == null || stableKey.isBlank()) return false;
        final TeamSavedData data = TeamSavedData.get(player.server);
        final var board = data.blackboard(player.getUUID());
        final String subject = "unlock:" + stableKey;
        if (board.actionLedger().hasCommitted(ActionType.COMPANION_UNLOCK, subject)) return false;
        final long now = player.level().getGameTime();
        final var begun = board.actionLedger().begin(ActionType.COMPANION_UNLOCK, subject, stableKey, now, 24000L);
        if (begun.reused()) return false;
        final ActionTransaction transaction = begun.transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("UNLOCK_STATE_VALIDATED");
        if (transaction.phase() == ActionPhase.RESERVED) transaction.applied("UNLOCK_STATE_WRITTEN");
        if (transaction.phase() == ActionPhase.APPLIED) transaction.commit(now, "UNLOCK_COMMITTED");
        data.markChanged();
        return true;
    }
}
