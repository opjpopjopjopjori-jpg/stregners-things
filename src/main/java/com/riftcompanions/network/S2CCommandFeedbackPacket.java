package com.riftcompanions.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Bounded feedback for a client command; detailed stack traces are never sent to players. */
public record S2CCommandFeedbackPacket(boolean success, String code, String detail) {
    public S2CCommandFeedbackPacket {
        code = trim(code, 80);
        detail = trim(detail, 160);
    }

    public static void encode(final S2CCommandFeedbackPacket packet, final FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.success);
        buffer.writeUtf(packet.code, 80);
        buffer.writeUtf(packet.detail, 160);
    }

    public static S2CCommandFeedbackPacket decode(final FriendlyByteBuf buffer) {
        return new S2CCommandFeedbackPacket(buffer.readBoolean(), buffer.readUtf(80), buffer.readUtf(160));
    }

    public static void handle(final S2CCommandFeedbackPacket packet, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.riftcompanions.client.ClientPacketHandlers.acceptFeedback(packet.success, packet.code, packet.detail)));
        context.setPacketHandled(true);
    }

    private static String trim(final String value, final int maximum) {
        if (value == null) {
            return "";
        }
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
