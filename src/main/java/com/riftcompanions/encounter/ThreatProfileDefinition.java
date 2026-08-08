package com.riftcompanions.encounter;

import net.minecraft.resources.ResourceLocation;

/**
 * Data-pack advisory only. It can increase caution and improve observed-tell /
 * counterplay vocabulary, but cannot grant power eligibility, block permission,
 * drops, target ownership, or hidden mechanics.
 */
public record ThreatProfileDefinition(
        String id,
        ResourceLocation entityType,
        ThreatArchetype archetype,
        int cautionBias,
        String observableTell,
        String counterplay
) {
    public ThreatProfileDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id");
        if (entityType == null) throw new IllegalArgumentException("entityType");
        if (archetype == null) throw new IllegalArgumentException("archetype");
        cautionBias = Math.max(0, Math.min(30, cautionBias));
        observableTell = clip(observableTell, 120);
        counterplay = clip(counterplay, 120);
    }

    private static String clip(final String value, final int maximum) {
        if (value == null) return "";
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
