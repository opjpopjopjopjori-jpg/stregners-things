package com.riftcompanions.encounter;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.Optional;

/** Immutable advisory snapshot installed only after a valid data-pack reload. */
public final class ThreatProfileRegistry {
    private static volatile Map<ResourceLocation, ThreatProfileDefinition> profiles = Map.of();

    private ThreatProfileRegistry() {}

    public static void replace(final Map<ResourceLocation, ThreatProfileDefinition> accepted) {
        profiles = Map.copyOf(accepted);
    }

    public static Optional<ThreatProfileDefinition> forEntity(final Entity entity) {
        if (entity == null) return Optional.empty();
        final ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return Optional.ofNullable(key == null ? null : profiles.get(key));
    }

    public static Optional<ThreatProfileDefinition> forType(final ResourceLocation type) {
        return Optional.ofNullable(type == null ? null : profiles.get(type));
    }

    public static int size() { return profiles.size(); }
}
