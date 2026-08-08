package com.riftcompanions.network;

import com.riftcompanions.conversation.ConversationService;
import com.riftcompanions.conversation.ConversationTopic;
import com.riftcompanions.entity.CompanionRole;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/** Bounded role/topic request; the server validates distance and team ownership. */
public record C2SConversationTopicPacket(int roleId, int topicId) {
    public static void encode(final C2SConversationTopicPacket packet, final FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.roleId);
        buffer.writeVarInt(packet.topicId);
    }

    public static C2SConversationTopicPacket decode(final FriendlyByteBuf buffer) {
        return new C2SConversationTopicPacket(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(final C2SConversationTopicPacket packet, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null) return;
            final CompanionRole[] roles = CompanionRole.values();
            final Optional<ConversationTopic> topic = ConversationTopic.byId(packet.topicId);
            if (packet.roleId < 0 || packet.roleId >= roles.length || topic.isEmpty()) {
                ModNetwork.sendToPlayer(new S2CCommandFeedbackPacket(false, "INVALID_CONVERSATION_REQUEST", "The conversation request is invalid."), player);
                return;
            }
            final ConversationService.ConversationResult result = ConversationService.respond(player, roles[packet.roleId], topic.get());
            ModNetwork.sendToPlayer(new S2CCommandFeedbackPacket(result.successful(), result.code(), result.text()), player);
        });
        context.setPacketHandled(true);
    }
}
