package com.riftcompanions.event;

import com.riftcompanions.RiftCompanions;
import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.server.TeamStatusService;
import com.riftcompanions.server.SessionRecapService;
import com.riftcompanions.persistence.SaveRecoveryService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Lightweight owner-only status sync. Four compact rows are sent at a configurable interval. */
@Mod.EventBusSubscriber(modid = RiftCompanions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TeamSyncEvents {
    private TeamSyncEvents() {}

    @SubscribeEvent
    public static void onLogin(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.server.isSingleplayer()) {
            // Recovery must finish before the player sees a recap or HUD state.
            SaveRecoveryService.recoverAfterLogin(player);
            TeamStatusService.sync(player);
            SessionRecapService.maybeSend(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(final PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.server.isSingleplayer()) {
            TeamStatusService.sync(player);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(final PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.server.isSingleplayer()) {
            TeamStatusService.sync(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)
                || !player.server.isSingleplayer()) {
            return;
        }
        final int interval = Math.max(10, CompanionConfig.STATUS_SYNC_INTERVAL_TICKS.get());
        if (player.tickCount % interval == 0) {
            TeamStatusService.sync(player);
        }
    }
}
