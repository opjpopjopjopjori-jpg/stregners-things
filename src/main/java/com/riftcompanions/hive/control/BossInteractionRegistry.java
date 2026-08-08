package com.riftcompanions.hive.control;

import com.riftcompanions.registry.ModTags;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/** Tagged vanilla bosses default to a short PARTIAL Stagger window; adapters may impose stricter compatibility resistance. */
public final class BossInteractionRegistry {
    private static final List<BossInteractionAdapter> ADAPTERS = new ArrayList<>();

    private BossInteractionRegistry() {}

    public static void register(BossInteractionAdapter adapter) {
        if (adapter != null) ADAPTERS.add(adapter);
    }

    public static BossResistanceLevel resistance(Entity entity) {
        for (BossInteractionAdapter adapter : ADAPTERS) {
            try {
                if (adapter.supports(entity)) return adapter.resistance(entity);
            } catch (RuntimeException ignored) {
                return BossResistanceLevel.IMMUNE;
            }
        }
        return entity.getType().is(ModTags.HIVE_BOSSES) ? BossResistanceLevel.PARTIAL : BossResistanceLevel.VULNERABLE_WINDOW;
    }
}
