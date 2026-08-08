package com.riftcompanions.resource;

/** Conservative item categories; unknown/modded items never become auto-pickup candidates by default. */
public enum ItemCategory {
    HEALING,
    BASIC_FOOD,
    PERSONAL_WEAPON,
    PERSONAL_AMMO,
    TEAM_EMERGENCY_SUPPLY,
    QUEST_OR_MEMORY_ITEM,
    BUILDING_MATERIAL_SAFE,
    RARE_OR_PROTECTED,
    UNKNOWN,
    JUNK_OR_LOW_PRIORITY,
    LOW_PRIORITY
}
