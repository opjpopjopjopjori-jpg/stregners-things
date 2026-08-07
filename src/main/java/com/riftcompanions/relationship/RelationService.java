package com.riftcompanions.relationship;

import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.server.TeamSavedData;
import net.minecraft.server.level.ServerPlayer;

/** Event-based positive relationship updates, deliberately small and transparent. */
public final class RelationService {
    private RelationService() {}

    public static void recordRescue(final ServerPlayer player, final CompanionRole role) {
        adjust(player, role, 6, true);
    }

    public static void recordPlanSuccess(final ServerPlayer player) {
        for (final CompanionRole role : CompanionRole.values()) {
            adjust(player, role, 2, true);
        }
    }

    public static void recordRespectfulPowerChoice(final ServerPlayer player, final CompanionRole role) {
        adjust(player, role, 1, false);
    }

    public static void adjust(final ServerPlayer player, final CompanionRole role, final int delta, final boolean milestone) {
        final TeamSavedData data = TeamSavedData.get(player.server);
        data.blackboard(player.getUUID()).adjustRelation(role, delta, milestone);
        data.markChanged();
    }
}
