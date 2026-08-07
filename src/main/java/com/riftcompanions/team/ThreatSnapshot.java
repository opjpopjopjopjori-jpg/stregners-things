package com.riftcompanions.team;

import com.riftcompanions.server.SafeTeleport;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;

import java.util.ArrayList;
import java.util.List;

/**
 * Explainable threat dimensions. A high score is not a magic AI answer: the
 * reason codes tell a plan/UI exactly why a retreat or warning was proposed.
 */
public record ThreatSnapshot(
        int immediateDamageRisk,
        int crowdRisk,
        int terrainRisk,
        int escapeRisk,
        int resourceRisk,
        int unknownRisk,
        int dangerScore,
        List<String> reasonCodes
) {
    public static ThreatSnapshot assess(final ServerPlayer player) {
        final List<String> reasons = new ArrayList<>();
        final List<Monster> nearby = player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(14.0D));

        int immediate = 0;
        int crowd = 0;
        int terrain = 0;
        int escape = 0;
        int resource = 0;
        int unknown = 0;

        if (!nearby.isEmpty()) {
            immediate = 20;
            reasons.add("HOSTILES_CLOSE");
        }
        final long veryClose = nearby.stream().filter(monster -> monster.distanceToSqr(player) <= 5.0D * 5.0D).count();
        if (veryClose > 0) {
            immediate += 25;
            reasons.add("HOSTILE_WITHIN_5_BLOCKS");
        }
        if (nearby.size() >= 4) {
            crowd = Math.min(35, nearby.size() * 6);
            reasons.add("HOSTILE_CROWD_" + nearby.size());
        }
        if (player.isInLava() || player.isOnFire()) {
            terrain = 50;
            reasons.add("LAVA_OR_FIRE");
        } else if (player.fallDistance > 3.0F) {
            terrain = 35;
            reasons.add("FALL_RISK");
        }
        if (player.getHealth() <= player.getMaxHealth() * 0.25F) {
            resource = 45;
            reasons.add("PLAYER_HEALTH_CRITICAL");
        } else if (player.getHealth() <= player.getMaxHealth() * 0.50F) {
            resource = 20;
            reasons.add("PLAYER_HEALTH_LOW");
        }
        if (SafeTeleport.findSafeSpot(player.serverLevel(), player, player.blockPosition(), 3).isEmpty()) {
            escape = 30;
            reasons.add("NO_LOCAL_SAFE_RECALL_POSITION");
        }
        for (final Monster monster : nearby) {
            final ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(monster.getType());
            final var advisory = com.riftcompanions.encounter.ThreatProfileRegistry.forEntity(monster).orElse(null);
            if (advisory != null && advisory.cautionBias() > 0) {
                unknown = Math.max(unknown, advisory.cautionBias());
                reasons.add("THREAT_PROFILE_" + advisory.id().toUpperCase(java.util.Locale.ROOT));
            }
            if (key == null || !"minecraft".equals(key.getNamespace())) {
                // A data profile may improve vocabulary, but never removes the
                // conservative Unknown/Caution fallback for a modded threat.
                unknown = Math.max(unknown, 20);
                reasons.add("MODDED_THREAT_UNCLASSIFIED");
            }
        }

        // Immediate/terrain/resource are weighted because they can kill within
        // seconds. The score is capped but dimensions remain individually visible.
        final int score = Math.min(100, immediate + crowd + terrain + escape + resource + unknown);
        return new ThreatSnapshot(immediate, crowd, terrain, escape, resource, unknown, score, List.copyOf(reasons));
    }
}
