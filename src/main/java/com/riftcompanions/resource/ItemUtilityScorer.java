package com.riftcompanions.resource;

import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import net.minecraft.server.level.ServerPlayer;

/** Transparent bounded heuristic; a non-positive score always means do not auto-take. */
public final class ItemUtilityScorer {
    private ItemUtilityScorer() {}

    public static int score(final ServerPlayer player, final CompanionEntity companion, final ItemCategory category,
                            final boolean emergencyOnly) {
        if (player == null || companion == null || category == null || CompanionResourceProfile.neverAutoTake(category)) return Integer.MIN_VALUE;
        if (!CompanionResourceProfile.allowedAutoCategories(companion.getRole()).contains(category)) return Integer.MIN_VALUE;
        int score = 0;
        final boolean companionLow = companion.getHealth() <= companion.getMaxHealth() * 0.45F;
        final boolean playerLow = player.getHealth() <= player.getMaxHealth() * 0.35F;
        switch (category) {
            case HEALING -> score += companionLow || playerLow ? 80 : 28;
            case BASIC_FOOD -> score += companionLow ? 32 : 12;
            case PERSONAL_AMMO -> score += companion.getRole() == CompanionRole.SCOUT ? 36 : 0;
            case QUEST_OR_MEMORY_ITEM -> score += companion.getRole() == CompanionRole.SEER ? 20 : 4;
            case TEAM_EMERGENCY_SUPPLY -> score += companionLow || playerLow ? 45 : 10;
            default -> score -= 100;
        }
        if (emergencyOnly && (!(category == ItemCategory.HEALING || category == ItemCategory.BASIC_FOOD)
                || !(companionLow || playerLow))) return Integer.MIN_VALUE;
        score -= companion.getPersonalInventory().occupiedSlots() >= companion.getPersonalInventory().size() ? 100 : 0;
        return score;
    }
}
