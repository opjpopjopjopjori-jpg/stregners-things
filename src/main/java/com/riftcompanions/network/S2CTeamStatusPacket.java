package com.riftcompanions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> owner-only visual snapshot. */
public record S2CTeamStatusPacket(TeamStatusSnapshot snapshot) {
    public static void encode(final S2CTeamStatusPacket packet, final FriendlyByteBuf buffer) {
        packet.snapshot.write(buffer);
    }

    public static S2CTeamStatusPacket decode(final FriendlyByteBuf buffer) {
        return new S2CTeamStatusPacket(TeamStatusSnapshot.read(buffer));
    }

    public static void handle(final S2CTeamStatusPacket packet, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.riftcompanions.client.ClientPacketHandlers.acceptTeamStatus(packet.snapshot)));
        context.setPacketHandled(true);
    }
}
