package com.riftcompanions.server;

import com.riftcompanions.arc.ArcMilestone;
import com.riftcompanions.arc.ArcService;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.team.TeamBlackboard;
import com.riftcompanions.team.TeamPlan;
import net.minecraft.server.level.ServerPlayer;

/** Low-volume base milestones derived from actual safe return and plan context. */
public final class BaseMilestoneService {
    private BaseMilestoneService() {}

    public static void onCompanionReachedHome(final ServerPlayer player, final CompanionRole role) {
        if (player == null || role == null) return;
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        if (!board.recordFirstSafeReturn()) return;
        final long day = player.level().getDayTime() / 24000L;
        MilestoneTransactionService.recordJournalMilestone(player, "first_safe_return", new MemoryRecord(MemoryType.MILESTONE, day,
                "The team completed its first safe return to HOME with " + role.personalName() + ".", 92));
        data.markChanged();
        ArcService.observe(player, ArcMilestone.TEAM_SAFE_RETURN);
    }

    public static void onPlanCreatedAtSafeBase(final ServerPlayer player, final TeamPlan plan) {
        if (player == null || plan == null) return;
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        if (!board.isAtSafeBase(player) || !board.recordFirstPlanFromHome()) return;
        final long day = player.level().getDayTime() / 24000L;
        MilestoneTransactionService.recordJournalMilestone(player, "first_plan_from_home", new MemoryRecord(MemoryType.MILESTONE, day,
                "The team drafted its first plan from a safe base: " + plan.type() + ".", 88));
        data.markChanged();
    }
}
