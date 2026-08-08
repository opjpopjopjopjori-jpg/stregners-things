package com.riftcompanions.world.assessment;

import net.minecraft.resources.ResourceLocation;

/** Advisory override for an observed block, not a hidden-structure discovery rule. */
public record StructureProfileOverride(ResourceLocation block, String profileId, int cautionBias) {
    public StructureProfileOverride {
        if (block == null) throw new IllegalArgumentException("block");
        if (!isAllowedProfile(profileId)) throw new IllegalArgumentException("profile_id");
        cautionBias = Math.max(0, Math.min(30, cautionBias));
    }

    public static boolean isAllowedProfile(final String id) {
        return "village".equals(id) || "dungeon".equals(id) || "mineshaft".equals(id)
                || "ruined_portal".equals(id) || "stronghold".equals(id) || "unknown".equals(id);
    }
}
