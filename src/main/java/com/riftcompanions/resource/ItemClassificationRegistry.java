package com.riftcompanions.resource;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/** Data-pack classifications affect only transparent item labels, never permissions or automation. */
public final class ItemClassificationRegistry {
    private static volatile Map<ResourceLocation, ItemCategory> overrides = Map.of();

    private ItemClassificationRegistry() {}

    public static void replace(final Map<ResourceLocation, ItemCategory> values) { overrides = Map.copyOf(values); }
    public static Optional<ItemCategory> override(final ResourceLocation itemId) { return Optional.ofNullable(overrides.get(itemId)); }
    public static int size() { return overrides.size(); }
}
