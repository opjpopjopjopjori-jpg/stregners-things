package com.riftcompanions.registry;

import com.riftcompanions.RiftCompanions;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.GiftedCompanionEntity;
import com.riftcompanions.entity.GuardianCompanionEntity;
import com.riftcompanions.entity.ScoutCompanionEntity;
import com.riftcompanions.entity.SeerCompanionEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The mod registers companion entities only. It intentionally adds no hostile
 * creatures, custom enemy roster, natural spawn, worldgen, or monster content.
 */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTER =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RiftCompanions.MOD_ID);

    public static final RegistryObject<EntityType<SeerCompanionEntity>> SEER = REGISTER.register("seer", () ->
            EntityType.Builder.of(SeerCompanionEntity::new, MobCategory.CREATURE)
                    .sized(0.60F, 1.95F).clientTrackingRange(12).updateInterval(2)
                    .build("riftcompanions:seer"));

    public static final RegistryObject<EntityType<GuardianCompanionEntity>> GUARDIAN = REGISTER.register("guardian", () ->
            EntityType.Builder.of(GuardianCompanionEntity::new, MobCategory.CREATURE)
                    .sized(0.66F, 1.98F).clientTrackingRange(12).updateInterval(2)
                    .build("riftcompanions:guardian"));

    public static final RegistryObject<EntityType<GiftedCompanionEntity>> GIFTED = REGISTER.register("gifted", () ->
            EntityType.Builder.of(GiftedCompanionEntity::new, MobCategory.CREATURE)
                    .sized(0.60F, 1.94F).clientTrackingRange(12).updateInterval(2)
                    .build("riftcompanions:gifted"));

    public static final RegistryObject<EntityType<ScoutCompanionEntity>> SCOUT = REGISTER.register("scout", () ->
            EntityType.Builder.of(ScoutCompanionEntity::new, MobCategory.CREATURE)
                    .sized(0.60F, 1.94F).clientTrackingRange(12).updateInterval(2)
                    .build("riftcompanions:scout"));

    private ModEntities() {}

    @Mod.EventBusSubscriber(modid = RiftCompanions.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class AttributeEvents {
        @SubscribeEvent
        public static void registerAttributes(final EntityAttributeCreationEvent event) {
            event.put(SEER.get(), CompanionEntity.createCompanionAttributes(18.0D, 0.25D, 2.0D).build());
            event.put(GUARDIAN.get(), CompanionEntity.createCompanionAttributes(30.0D, 0.23D, 4.5D).build());
            event.put(GIFTED.get(), CompanionEntity.createCompanionAttributes(20.0D, 0.27D, 2.5D).build());
            event.put(SCOUT.get(), CompanionEntity.createCompanionAttributes(18.0D, 0.31D, 2.2D).build());
        }
    }
}
