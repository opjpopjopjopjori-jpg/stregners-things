package com.riftcompanions.network;

import com.riftcompanions.entity.CompanionRole;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server asks the physical client to open a bounded conversation UI for one nearby role. */
public record S2COpenConversationPacket(int roleId) {
    public static void encode(final S2COpenConversationPacket packet, final FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.roleId);
    }

    public static S2COpenConversationPacket decode(final FriendlyByteBuf buffer) {
        return new S2COpenConversationPacket(buffer.readVarInt());
    }

    public static void handle(final S2COpenConversationPacket packet, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.riftcompanions.client.ClientPacketHandlers.openConversation(packet.roleId)));
        context.setPacketHandled(true);
    }
}
