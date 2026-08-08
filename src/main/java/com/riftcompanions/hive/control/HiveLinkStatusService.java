package com.riftcompanions.hive.control;

import com.riftcompanions.network.ModNetwork;
import com.riftcompanions.network.S2CHiveLinkStatusPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Sends a compact display-only Will control indicator; no camera or input control. */
public final class HiveLinkStatusService {
    private HiveLinkStatusService() {}

    public static void send(final ServerPlayer player, final Entity target, final HiveControlMode mode,
                            final int ticks, final String message) {
        send(player, target, mode, ticks, 0, 0, 0, 0, message);
    }

    public static void send(final ServerPlayer player, final Entity target, final HiveControlMode mode,
                            final int ticks, final int targetCount, final int targetLimit,
                            final int controlLoadHearts, final int controlCapacityHearts,
                            final String message) {
        ModNetwork.sendToPlayer(new S2CHiveLinkStatusPacket(target == null ? -1 : target.getId(),
                mode == null ? -1 : mode.ordinal(), Math.max(0, ticks), Math.max(0, targetCount),
                Math.max(0, targetLimit), Math.max(0, controlLoadHearts), Math.max(0, controlCapacityHearts),
                message), player);
    }
}
