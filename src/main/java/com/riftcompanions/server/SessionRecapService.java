package com.riftcompanions.server;

import com.riftcompanions.config.CompanionConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** One optional, player-visible recap on login; no hidden information is invented. */
public final class SessionRecapService {
    private SessionRecapService() {}

    public static void maybeSend(final ServerPlayer player) {
        if (!CompanionConfig.SESSION_RECAP_ENABLED.get()) {
            return;
        }
        final var board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        if (board.safeMode().enabled()) {
            player.sendSystemMessage(Component.literal("[Team Recap] Safe Mode: " + board.safeMode().reason() + " — " + board.safeMode().detail()));
            return;
        }
        if (board.plan().isActive() || board.plan().awaitsApproval()) {
            player.sendSystemMessage(Component.literal("[Team Recap] Pending plan: " + board.plan().type() + " / " + board.plan().status() + " — " + board.plan().objective()));
            return;
        }
        final var memories = board.memories();
        if (!memories.isEmpty()) {
            final var latest = memories.get(memories.size() - 1);
            player.sendSystemMessage(Component.literal("[Team Recap] Latest memory: " + latest.summary()));
        }
    }
}
