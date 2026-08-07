package com.riftcompanions.arc;

import com.riftcompanions.entity.CompanionRole;

/** Optional milestone arcs; they add reflection and clarity, never progression gates. */
public enum CompanionArc {
    HOPPER_SAFE_WAY_BACK(CompanionRole.GUARDIAN, "The Safe Way Back"),
    WILL_PATTERN_CHANGES(CompanionRole.SEER, "The Pattern That Changes"),
    ELEVEN_CHOICE_NOT_WEAPON(CompanionRole.GIFTED, "Choice, Not Weapon"),
    MAX_ROUTE_WORTH_TAKING(CompanionRole.SCOUT, "A Route Worth Taking");

    private final CompanionRole role;
    private final String title;

    CompanionArc(final CompanionRole role, final String title) {
        this.role = role;
        this.title = title;
    }

    public CompanionRole role() { return role; }
    public String title() { return title; }
}
