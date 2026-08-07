package com.riftcompanions.network;

import com.riftcompanions.server.TeamStatusService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A client may request its own roster snapshot; no request parameters are trusted. */
public final class C2SRequestTeamStatusPacket {
    public static void encode(final C2SRequestTeamStatusPacket ignored, final FriendlyByteBuf buffer) {
        // Deliberately empty.
    }

    public static C2SRequestTeamStatusPacket decode(final FriendlyByteBuf buffer) {
        return new C2SRequestTeamStatusPacket();
    }

    public static void handle(final C2SRequestTeamStatusPacket packet, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player != null) {
                TeamStatusService.sync(player);
            }
        });
        context.setPacketHandled(true);
    }
}
