package com.riftcompanions.server;

import com.riftcompanions.network.ModNetwork;
import com.riftcompanions.network.S2CTeamStatusPacket;
import com.riftcompanions.network.TeamStatusSnapshot;
import net.minecraft.server.level.ServerPlayer;

/** Owner-only state synchronization for the HUD, Command Wheel and Team Journal. */
public final class TeamStatusService {
    private TeamStatusService() {}

    public static void sync(final ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }
        ModNetwork.sendToPlayer(new S2CTeamStatusPacket(TeamStatusSnapshot.fromServer(player)), player);
    }
}
