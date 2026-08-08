package com.riftcompanions.network;

import com.riftcompanions.duo.DuoDynamicsService;
import com.riftcompanions.duo.TeamPair;
import com.riftcompanions.server.TeamStatusService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Two enum ids only; the server validates safe-anchor, plan, and DOWNED rules. */
public record C2SSelectDuoPacket(int firstRoleId, int secondRoleId) {
    public static C2SSelectDuoPacket of(final TeamPair pair) {
        return new C2SSelectDuoPacket(pair.first().ordinal(), pair.second().ordinal());
    }

    public static void encode(final C2SSelectDuoPacket packet, final FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.firstRoleId() + 1);
        buffer.writeVarInt(packet.secondRoleId() + 1);
    }

    public static C2SSelectDuoPacket decode(final FriendlyByteBuf buffer) {
        return new C2SSelectDuoPacket(buffer.readVarInt() - 1, buffer.readVarInt() - 1);
    }

    public static void handle(final C2SSelectDuoPacket packet, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null) return;
            if (player.server == null || !player.server.isSingleplayer()) {
                ModNetwork.sendToPlayer(new S2CCommandFeedbackPacket(false, "SINGLEPLAYER_ONLY", "This edition is designed for offline single-player worlds only."), player);
                return;
            }
            final var roles = com.riftcompanions.entity.CompanionRole.values();
            if (packet.firstRoleId() < 0 || packet.firstRoleId() >= roles.length || packet.secondRoleId() < 0 || packet.secondRoleId() >= roles.length) {
                ModNetwork.sendToPlayer(new S2CCommandFeedbackPacket(false, "INVALID_DUO_SELECTION", "The requested duo IDs were rejected."), player);
                return;
            }
            final DuoDynamicsService.DuoResult result = DuoDynamicsService.selectAtBase(player,
                    roles[packet.firstRoleId()], roles[packet.secondRoleId()]);
            ModNetwork.sendToPlayer(new S2CCommandFeedbackPacket(result.successful(), result.code(), result.detail()), player);
            TeamStatusService.sync(player);
        });
        context.setPacketHandled(true);
    }
}
