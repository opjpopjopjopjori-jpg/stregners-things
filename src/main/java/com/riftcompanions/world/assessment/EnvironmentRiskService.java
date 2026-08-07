package com.riftcompanions.world.assessment;

import com.riftcompanions.server.SafeTeleport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;

/** Bounded measurable signals for weather/light/terrain/hostiles/escape. */
public final class EnvironmentRiskService {
    private EnvironmentRiskService() {}

    public static EnvironmentRiskAssessment assess(final ServerPlayer player) {
        final ServerLevel level = player.serverLevel();
        final BlockPos origin = player.blockPosition();
        final List<String> reasons = new ArrayList<>();
        int score = 0;
        final long time = level.getDayTime() % 24000L;
        if (time >= 12000L && time <= 23000L) {
            score += 15;
            reasons.add("NIGHT");
        }
        if (level.isThundering()) {
            score += 10;
            reasons.add("THUNDER");
        } else if (level.isRaining()) {
            score += 4;
            reasons.add("RAIN");
        }
        if (level.getMaxLocalRawBrightness(origin) <= 5) {
            score += 12;
            reasons.add("LOW_LIGHT");
        }
        final int hostiles = level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(14.0D)).size();
        if (hostiles > 0) {
            score += Math.min(30, hostiles * 6);
            reasons.add("HOSTILES_" + hostiles);
        }
        if (lavaNearby(level, origin)) {
            score += 24;
            reasons.add("LAVA_NEARBY");
        }
        if (player.fallDistance > 3.0F || player.isInLava()) {
            score += 35;
            reasons.add(player.isInLava() ? "LAVA_IMMEDIATE" : "FALL_RISK");
        }
        if (player.getHealth() <= player.getMaxHealth() * 0.25F) {
            score += 25;
            reasons.add("PLAYER_HEALTH_CRITICAL");
        }
        if (SafeTeleport.findSafeSpot(level, player, origin, 3).isEmpty()) {
            score += 14;
            reasons.add("NO_LOCAL_SAFE_SPOT");
        }
        final ResourceLocation biome = level.registryAccess().registryOrThrow(Registries.BIOME).getKey(level.getBiome(origin).value());
        return new EnvironmentRiskAssessment(biome, Math.min(100, score), EnvironmentRiskAssessment.bandFor(score), reasons);
    }

    private static boolean lavaNearby(final ServerLevel level, final BlockPos origin) {
        for (final BlockPos pos : BlockPos.betweenClosed(origin.offset(-2, -1, -2), origin.offset(2, 1, 2))) {
            if (level.hasChunkAt(pos) && level.getFluidState(pos).is(Fluids.LAVA)) {
                return true;
            }
        }
        return false;
    }
}
