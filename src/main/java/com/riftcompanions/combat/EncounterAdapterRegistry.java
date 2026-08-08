package com.riftcompanions.combat;

import com.riftcompanions.RiftCompanions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional compatibility adapter registry with a mandatory safe fallback.
 * The base mod intentionally registers no custom hostile roster; unsupported
 * vanilla or modded threats remain Unknown/Caution until an explicit adapter
 * proves a bounded profile.
 */
public final class EncounterAdapterRegistry {
    private static final List<EncounterAdapter> ADAPTERS = new ArrayList<>();

    private EncounterAdapterRegistry() {}

    public static void register(final EncounterAdapter adapter) {
        if (adapter != null) ADAPTERS.add(adapter);
    }

    public static EncounterProfile profileFor(final Entity entity) {
        return entity == null ? EncounterProfile.unknown("null") : profileFor(entity.getType());
    }

    public static EncounterProfile profileFor(final EntityType<?> type) {
        for (final EncounterAdapter adapter : ADAPTERS) {
            try {
                if (adapter.supports(type)) return adapter.profile(type);
            } catch (RuntimeException exception) {
                RiftCompanions.LOGGER.warn("Encounter adapter failed for {}. Falling back to Unknown/Caution.", type, exception);
                break;
            }
        }
        final ResourceLocation id = EntityType.getKey(type);
        return EncounterProfile.unknown(id == null ? "unknown" : id.toString());
    }
}
