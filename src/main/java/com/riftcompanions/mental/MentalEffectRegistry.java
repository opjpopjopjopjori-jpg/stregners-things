package com.riftcompanions.mental;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable safe support list; absent effects are never removed or claimed as mental. */
public final class MentalEffectRegistry {
    private static volatile Map<ResourceLocation, MentalEffectProfile> profiles = Map.of();

    private MentalEffectRegistry() {}

    public static void replace(final Map<ResourceLocation, MentalEffectProfile> values) {
        profiles = Map.copyOf(values);
    }

    public static Optional<MentalEffectProfile> profile(final MobEffect effect) {
        if (effect == null) return Optional.empty();
        final ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect);
        return Optional.ofNullable(id == null ? null : profiles.get(id));
    }

    public static List<MobEffect> supportedEffects() {
        return profiles.values().stream()
                .sorted(Comparator.comparingInt(MentalEffectProfile::priority).reversed())
                .map(profile -> net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(profile.effectId()))
                .filter(effect -> effect != null)
                .toList();
    }

    public static int size() { return profiles.size(); }
}
