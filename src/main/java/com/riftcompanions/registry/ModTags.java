package com.riftcompanions.registry;

import com.riftcompanions.RiftCompanions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/** Tags are the only way special anomaly logic may identify compatible modded threats. */
public final class ModTags {
    public static final TagKey<EntityType<?>> HIVE_LINKED = entityTypeTag("hive_linked");
    public static final TagKey<EntityType<?>> HIVE_BOSSES = entityTypeTag("hive_bosses");
    public static final TagKey<EntityType<?>> PROTECTED_FROM_COMPANIONS = entityTypeTag("protected_from_companions");

    private ModTags() {}

    private static TagKey<EntityType<?>> entityTypeTag(final String path) {
        return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(RiftCompanions.MOD_ID, path));
    }
}
