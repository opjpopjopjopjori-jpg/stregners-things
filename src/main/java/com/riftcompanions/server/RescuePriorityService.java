package com.riftcompanions.server;

import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.DownedStatus;
import net.minecraft.server.level.ServerPlayer;

/** Deterministic local rescue ordering; it never crosses a dimension or overrides the player. */
public final class RescuePriorityService {
    private RescuePriorityService() {}

    public static int score(final ServerPlayer player, final CompanionEntity companion) {
        if (player == null || companion == null || companion.level() != player.level()) return Integer.MIN_VALUE;
        final int status = switch (companion.getDownedStatus()) {
            case DANGER -> 100;
            case UNREACHABLE -> 70;
            case RESCUING -> 60;
            case STABLE -> 45;
            case RECOVERING -> 20;
        };
        final int distance = Math.max(0, 40 - (int) Math.sqrt(player.distanceToSqr(companion)) * 2);
        final int ownerRisk = player.getHealth() <= player.getMaxHealth() * 0.30F ? -20 : 0;
        return status + distance + ownerRisk;
    }
}
