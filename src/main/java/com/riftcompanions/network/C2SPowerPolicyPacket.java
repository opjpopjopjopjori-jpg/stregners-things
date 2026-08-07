package com.riftcompanions.network;

import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.policy.PlayerPolicyService;
import com.riftcompanions.server.TeamStatusService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Fixed role id and reset flag only; the server owns the next allowed policy. */
public record C2SPowerPolicyPacket(int roleId, boolean reset) {
    public static C2SPowerPolicyPacket cycle(final CompanionRole role) { return new C2SPowerPolicyPacket(role.ordinal(), false); }
    public static C2SPowerPolicyPacket reset(final CompanionRole role) { return new C2SPowerPolicyPacket(role.ordinal(), true); }

    public static void encode(final C2SPowerPolicyPacket packet, final FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.roleId() + 1);
        buffer.writeBoolean(packet.reset());
    }

    public static C2SPowerPolicyPacket decode(final FriendlyByteBuf buffer) {
        return new C2SPowerPolicyPacket(buffer.readVarInt() - 1, buffer.readBoolean());
    }

    public static void handle(final C2SPowerPolicyPacket packet, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null || player.server == null || !player.server.isSingleplayer()) return;
            final CompanionRole[] roles = CompanionRole.values();
            if (packet.roleId() < 0 || packet.roleId() >= roles.length) {
                ModNetwork.sendToPlayer(new S2CCommandFeedbackPacket(false, "INVALID_POLICY_ROLE", "The requested policy role was rejected."), player);
                return;
            }
            final PlayerPolicyService.PolicyResult result = packet.reset()
                    ? PlayerPolicyService.resetPowerPolicy(player, roles[packet.roleId()])
                    : PlayerPolicyService.cyclePowerPolicy(player, roles[packet.roleId()]);
            ModNetwork.sendToPlayer(new S2CCommandFeedbackPacket(result.successful(), result.code(), result.detail()), player);
            TeamStatusService.sync(player);
        });
        context.setPacketHandled(true);
    }
}
