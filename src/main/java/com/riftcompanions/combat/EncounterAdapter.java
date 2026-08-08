package com.riftcompanions.combat;

import net.minecraft.world.entity.EntityType;

/** Adding a mob means adding an adapter/profile, never rewriting Guardian logic. */
public interface EncounterAdapter {
    boolean supports(EntityType<?> type);
    EncounterProfile profile(EntityType<?> type);
}
