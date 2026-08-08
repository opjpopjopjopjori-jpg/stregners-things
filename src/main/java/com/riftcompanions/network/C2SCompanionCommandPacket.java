package com.riftcompanions.network;

import com.riftcompanions.entity.CompanionAbility;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.server.TeamCommandService;
import com.riftcompanions.server.TeamStatusService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/** Client input packet with enum IDs only; it cannot carry a world position, item, NBT or entity target. */
public record C2SCompanionCommandPacket(int commandId, int roleId, int abilityId) {
    public static final int NONE = -1;

    public static C2SCompanionCommandPacket of(final CompanionCommand command, final CompanionRole role, final CompanionAbility ability) {
        return new C2SCompanionCommandPacket(command.ordinal(), role == null ? NONE : role.ordinal(), ability == null ? NONE : ability.ordinal());
    }

    public static void encode(final C2SCompanionCommandPacket packet, final FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.commandId);
        buffer.writeVarInt(packet.roleId + 1);
        buffer.writeVarInt(packet.abilityId + 1);
    }

    public static C2SCompanionCommandPacket decode(final FriendlyByteBuf buffer) {
        return new C2SCompanionCommandPacket(buffer.readVarInt(), buffer.readVarInt() - 1, buffer.readVarInt() - 1);
    }

    public static void handle(final C2SCompanionCommandPacket packet, final Supplier<NetworkEvent.Context> contextSupplier) {
        final NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            final ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            final TeamCommandService.CommandResult result = TeamCommandService.execute(player, packet.commandId, packet.roleId, packet.abilityId);
            ModNetwork.sendToPlayer(new S2CCommandFeedbackPacket(result.success(), result.code(), result.detail()), player);
            TeamStatusService.sync(player);
        });
        context.setPacketHandled(true);
    }
}
