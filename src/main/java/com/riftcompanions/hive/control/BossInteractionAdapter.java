package com.riftcompanions.hive.control;

import net.minecraft.world.entity.Entity;

/** Boss behavior contract. An adapter controls resistance; companion code never auto-wins a boss. */
public interface BossInteractionAdapter {
    boolean supports(Entity entity);
    BossResistanceLevel resistance(Entity entity);
}
