package com.riftcompanions.resource;

import com.riftcompanions.entity.CompanionRole;

import java.util.EnumSet;
import java.util.Set;

/** Role-specific bounded supply relevance; never a general loot/mule policy. */
public final class CompanionResourceProfile {
    private CompanionResourceProfile() {}

    public static Set<ItemCategory> allowedAutoCategories(final CompanionRole role) {
        return switch (role) {
            case GUARDIAN -> EnumSet.of(ItemCategory.HEALING, ItemCategory.BASIC_FOOD, ItemCategory.TEAM_EMERGENCY_SUPPLY);
            case SEER -> EnumSet.of(ItemCategory.HEALING, ItemCategory.BASIC_FOOD, ItemCategory.QUEST_OR_MEMORY_ITEM);
            case GIFTED -> EnumSet.of(ItemCategory.HEALING);
            case SCOUT -> EnumSet.of(ItemCategory.HEALING, ItemCategory.BASIC_FOOD, ItemCategory.PERSONAL_AMMO, ItemCategory.QUEST_OR_MEMORY_ITEM);
        };
    }

    public static int dailyTeamSupplyCap(final CompanionRole role) {
        return switch (role) {
            case GUARDIAN -> 5;
            case SEER -> 2;
            case GIFTED -> 1;
            case SCOUT -> 17;
        };
    }

    public static boolean neverAutoTake(final ItemCategory category) {
        return category == ItemCategory.RARE_OR_PROTECTED || category == ItemCategory.UNKNOWN
                || category == ItemCategory.PERSONAL_WEAPON || category == ItemCategory.BUILDING_MATERIAL_SAFE
                || category == ItemCategory.JUNK_OR_LOW_PRIORITY;
    }
}
