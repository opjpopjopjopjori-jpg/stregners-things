package com.riftcompanions.network;

import com.riftcompanions.RiftCompanions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** One versioned channel; packets are fixed-size/capped and direction-specific. */
public final class ModNetwork {
    // Protocol 33 adds contextual interaction visual-action vocabulary.
    private static final String PROTOCOL = "33";
    private static int nextPacketId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RiftCompanions.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private ModNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(C2SCompanionCommandPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SCompanionCommandPacket::encode)
                .decoder(C2SCompanionCommandPacket::decode)
                .consumerMainThread(C2SCompanionCommandPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SRequestTeamStatusPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SRequestTeamStatusPacket::encode)
                .decoder(C2SRequestTeamStatusPacket::decode)
                .consumerMainThread(C2SRequestTeamStatusPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SSelectDuoPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SSelectDuoPacket::encode)
                .decoder(C2SSelectDuoPacket::decode)
                .consumerMainThread(C2SSelectDuoPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SPowerPolicyPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SPowerPolicyPacket::encode)
                .decoder(C2SPowerPolicyPacket::decode)
                .consumerMainThread(C2SPowerPolicyPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CTeamStatusPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CTeamStatusPacket::encode)
                .decoder(S2CTeamStatusPacket::decode)
                .consumerMainThread(S2CTeamStatusPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CCommandFeedbackPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CCommandFeedbackPacket::encode)
                .decoder(S2CCommandFeedbackPacket::decode)
                .consumerMainThread(S2CCommandFeedbackPacket::handle)
                .add();
        CHANNEL.messageBuilder(C2SConversationTopicPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SConversationTopicPacket::encode)
                .decoder(C2SConversationTopicPacket::decode)
                .consumerMainThread(C2SConversationTopicPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2COpenConversationPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2COpenConversationPacket::encode)
                .decoder(S2COpenConversationPacket::decode)
                .consumerMainThread(S2COpenConversationPacket::handle)
                .add();
        CHANNEL.messageBuilder(S2CHiveLinkStatusPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CHiveLinkStatusPacket::encode)
                .decoder(S2CHiveLinkStatusPacket::decode)
                .consumerMainThread(S2CHiveLinkStatusPacket::handle)
                .add();
    }

    public static void sendToServer(final Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(final Object packet, final ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static int nextId() {
        return nextPacketId++;
    }
}
