package com.riftcompanions.client.input;

import com.riftcompanions.RiftCompanions;
import com.riftcompanions.client.gui.CommandWheelScreen;
import com.riftcompanions.client.gui.TeamJournalScreen;
import com.riftcompanions.client.hud.CompanionDeveloperOverlay;
import com.riftcompanions.network.C2SCompanionCommandPacket;
import com.riftcompanions.network.CompanionCommand;
import com.riftcompanions.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Input never applies logic locally; it opens UI or sends a bounded server command packet. */
@Mod.EventBusSubscriber(modid = RiftCompanions.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientInputEvents {
    private ClientInputEvents() {}

    @SubscribeEvent
    public static void onClientTick(final TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        if (ClientKeyMappings.OPEN_COMMAND_WHEEL.consumeClick() && minecraft.screen == null) {
            minecraft.setScreen(new CommandWheelScreen());
        }
        if (ClientKeyMappings.OPEN_TEAM_JOURNAL.consumeClick() && minecraft.screen == null) {
            minecraft.setScreen(new TeamJournalScreen());
        }
        if (ClientKeyMappings.QUICK_RECALL.consumeClick() && minecraft.screen == null) {
            ModNetwork.sendToServer(C2SCompanionCommandPacket.of(CompanionCommand.RECALL_ALL, null, null));
        }
        if (ClientKeyMappings.QUICK_STATUS.consumeClick() && minecraft.screen == null) {
            minecraft.setScreen(new TeamJournalScreen(TeamJournalScreen.Tab.DIAGNOSTICS));
        }
        if (ClientKeyMappings.TOGGLE_DEVELOPER_OVERLAY.consumeClick() && minecraft.screen == null) {
            CompanionDeveloperOverlay.toggle();
        }
    }
}
