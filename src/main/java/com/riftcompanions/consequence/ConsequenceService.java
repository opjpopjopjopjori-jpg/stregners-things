package com.riftcompanions.consequence;

import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.relationship.RelationService;
import com.riftcompanions.server.MilestoneTransactionService;
import com.riftcompanions.server.TeamSavedData;
import net.minecraft.server.level.ServerPlayer;

/**
 * Records consequences that remain understandable and non-punitive. A decision
 * can change memory, route context, or small trust clarity, never remove core
 * content, destroy a companion, or silently reduce relationship.
 */
public final class ConsequenceService {
    private ConsequenceService() {}

    public static boolean record(final ServerPlayer player, final ConsequenceType type, final String key,
                                 final String summary, final int confidence) {
        if (player == null || player.server == null || key == null || key.isBlank() || summary == null || summary.isBlank()) return false;
        final TeamSavedData data = TeamSavedData.get(player.server);
        final var board = data.blackboard(player.getUUID());
        if (!board.recordConsequence(new ConsequenceRecord(type, player.level().getDayTime() / 24000L, key, summary, confidence))) return false;
        MilestoneTransactionService.recordJournalMilestone(player, "consequence:" + key,
                new MemoryRecord(MemoryType.MILESTONE, player.level().getDayTime() / 24000L, summary, confidence));
        data.markChanged();
        return true;
    }

    public static void recordRespectfulChoice(final ServerPlayer player, final com.riftcompanions.entity.CompanionRole role,
                                              final String key, final String summary) {
        if (record(player, ConsequenceType.RELATIONSHIP, key, summary, 78)) {
            RelationService.recordRespectfulPowerChoice(player, role);
        }
    }
}
