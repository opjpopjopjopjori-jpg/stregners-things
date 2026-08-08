package com.riftcompanions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server-to-client display-only Will control indicator. Heart load and target
 * counts are explanatory HUD data only; the client cannot select or alter a
 * target through this packet.
 */
public record S2CHiveLinkStatusPacket(int targetEntityId, int modeId, int remainingTicks,
                                      int targetCount, int targetLimit,
                                      int controlLoadHearts, int controlCapacityHearts,
                                      String message) {
    public static void encode(final S2CHiveLinkStatusPacket packet, final FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.targetEntityId + 1);
        buffer.writeVarInt(packet.modeId + 1);
        buffer.writeVarInt(Math.max(0, packet.remainingTicks));
        buffer.writeVarInt(Math.max(0, packet.targetCount));
        buffer.writeVarInt(Math.max(0, packet.targetLimit));
        buffer.writeVarInt(Math.max(0, packet.controlLoadHearts));
        buffer.writeVarInt(Math.max(0, packet.controlCapacityHearts));
        buffer.writeUtf(packet.message == null ? "" : packet.message, 160);
    }

    public static S2CHiveLinkStatusPacket decode(final FriendlyByteBuf buffer) {
        return new S2CHiveLinkStatusPacket(buffer.readVarInt() - 1, buffer.readVarInt() - 1,
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readUtf(160));
    }

    public static void handle(final S2CHiveLinkStatusPacket packet, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.riftcompanions.client.ClientPacketHandlers.acceptHiveLink(packet.targetEntityId,
                        packet.modeId, packet.remainingTicks, packet.targetCount, packet.targetLimit,
                        packet.controlLoadHearts, packet.controlCapacityHearts, packet.message)));
        context.setPacketHandled(true);
    }
}
