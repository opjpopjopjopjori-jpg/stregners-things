package com.riftcompanions.mental;

import net.minecraft.resources.ResourceLocation;

/** Data-pack declaration for a supported mental/disorientation effect only. */
public record MentalEffectProfile(ResourceLocation effectId, int priority) {
    public MentalEffectProfile {
        if (effectId == null) throw new IllegalArgumentException("effectId");
        priority = Math.max(0, Math.min(100, priority));
    }
}
