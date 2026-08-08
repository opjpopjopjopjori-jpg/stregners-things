package com.riftcompanions.client;

import com.riftcompanions.RiftCompanions;
import com.riftcompanions.client.hud.CompanionDeveloperOverlay;
import com.riftcompanions.client.hud.CompanionHudOverlay;
import com.riftcompanions.client.input.ClientKeyMappings;
import com.riftcompanions.client.render.CompanionRenderer;
import com.riftcompanions.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only companion rendering registration; it never decides AI, damage, or persistence. */
@Mod.EventBusSubscriber(modid = RiftCompanions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {}

    @SubscribeEvent
    public static void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        ClientKeyMappings.register(event);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(final RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("companion_status", CompanionHudOverlay.OVERLAY);
        event.registerAboveAll("companion_developer", CompanionDeveloperOverlay.OVERLAY);
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SEER.get(), CompanionRenderer::new);
        event.registerEntityRenderer(ModEntities.GUARDIAN.get(), CompanionRenderer::new);
        event.registerEntityRenderer(ModEntities.GIFTED.get(), CompanionRenderer::new);
        event.registerEntityRenderer(ModEntities.SCOUT.get(), CompanionRenderer::new);
    }
}
