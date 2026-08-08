package com.riftcompanions.world.assessment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.Map;
import java.util.Optional;

/** Immutable data-pack advisory registry; it cannot make the scanner load blocks or chunks. */
public final class StructureProfileOverrideRegistry {
    private static volatile Map<ResourceLocation, StructureProfileOverride> overrides = Map.of();

    private StructureProfileOverrideRegistry() {}

    public static void replace(final Map<ResourceLocation, StructureProfileOverride> accepted) {
        overrides = Map.copyOf(accepted);
    }

    public static Optional<StructureProfileOverride> forBlock(final Block block) {
        if (block == null) return Optional.empty();
        final ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        return Optional.ofNullable(key == null ? null : overrides.get(key));
    }

    public static int size() { return overrides.size(); }
}
