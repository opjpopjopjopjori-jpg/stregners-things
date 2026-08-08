package com.riftcompanions;

import com.mojang.logging.LogUtils;
import com.riftcompanions.registry.ModEntities;
import com.riftcompanions.registry.ModSounds;
import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.network.ModNetwork;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Server-authoritative entry point for the Forge 1.20.1 project.
 *
 * The approved workspace ships with a Gradle wrapper, Forge 47.x, Java 17,
 * and GeckoLib 4 wiring. A clean build verifies packaging only; runtime world
 * behavior still requires the documented client test gates.
 */
@Mod(RiftCompanions.MOD_ID)
public final class RiftCompanions {
    public static final String MOD_ID = "riftcompanions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RiftCompanions() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.REGISTER.register(modBus);
        ModSounds.REGISTER.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CompanionConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CompanionConfig.CLIENT_SPEC);
        ModNetwork.register();

        // Gameplay decisions, persistence and datapack dialogue loading belong
        // to the logical server, including an integrated single-player server.
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);
    }

    private void onAddReloadListeners(final AddReloadListenerEvent event) {
        event.addListener(com.riftcompanions.dialogue.DialogueReloadListener.INSTANCE);
        event.addListener(com.riftcompanions.compat.CompatibilityPackReloadListener.INSTANCE);
        event.addListener(com.riftcompanions.intention.IntentionReloadListener.INSTANCE);
        event.addListener(com.riftcompanions.resource.ItemClassificationReloadListener.INSTANCE);
        event.addListener(com.riftcompanions.encounter.ThreatProfileReloadListener.INSTANCE);
        event.addListener(com.riftcompanions.world.assessment.StructureProfileOverrideReloadListener.INSTANCE);
        event.addListener(com.riftcompanions.mental.MentalEffectReloadListener.INSTANCE);
        event.addListener(com.riftcompanions.encounter.EncounterDialogueReloadListener.INSTANCE);
        event.addListener(com.riftcompanions.behavior.BehaviorProfileReloadListener.INSTANCE);
        event.addListener(com.riftcompanions.context.ContextInteractionReloadListener.INSTANCE);
    }
}
