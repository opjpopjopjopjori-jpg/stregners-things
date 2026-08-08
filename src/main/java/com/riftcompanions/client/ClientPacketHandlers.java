package com.riftcompanions.client;

import com.riftcompanions.network.TeamStatusSnapshot;

/** Called only through DistExecutor from S2C packet handlers. */
public final class ClientPacketHandlers {
    private ClientPacketHandlers() {}

    public static void acceptTeamStatus(final TeamStatusSnapshot snapshot) {
        ClientTeamState.acceptSnapshot(snapshot);
    }

    public static void acceptFeedback(final boolean success, final String code, final String detail) {
        ClientTeamState.acceptFeedback(success, code, detail);
    }

    public static void acceptHiveLink(int targetEntityId, int modeId, int remainingTicks, int targetCount,
                                      int targetLimit, int controlLoadHearts, int controlCapacityHearts,
                                      String message) {
        ClientTeamState.acceptHiveLink(targetEntityId, modeId, remainingTicks, targetCount, targetLimit,
                controlLoadHearts, controlCapacityHearts, message);
    }

    public static void openConversation(final int roleId) {
        final com.riftcompanions.entity.CompanionRole[] roles = com.riftcompanions.entity.CompanionRole.values();
        if (roleId < 0 || roleId >= roles.length) {
            return;
        }
        final net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new com.riftcompanions.client.gui.ConversationScreen(roles[roleId]));
        }
    }
}
