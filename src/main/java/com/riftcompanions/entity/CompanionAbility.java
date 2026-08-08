package com.riftcompanions.entity;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum CompanionAbility {
    SEER_SENSE("sense", CompanionRole.SEER),
    SEER_DISRUPT("disrupt", CompanionRole.SEER),
    SEER_SUSPEND("suspend", CompanionRole.SEER),
    SEER_REDIRECT("redirect", CompanionRole.SEER),
    SEER_SHATTER("shatter", CompanionRole.SEER),
    SEER_SWARM_FREEZE("swarm_freeze", CompanionRole.SEER),
    SEER_SURGE_SUSPEND("surge_suspend", CompanionRole.SEER),
    SEER_SURGE_REDIRECT("surge_redirect", CompanionRole.SEER),
    SEER_SURGE_SHATTER("surge_shatter", CompanionRole.SEER),
    SEER_SURGE_SWARM_FREEZE("surge_swarm_freeze", CompanionRole.SEER),
    GIFTED_PUSH("push", CompanionRole.GIFTED),
    GIFTED_SHIELD("shield", CompanionRole.GIFTED),
    GIFTED_RESCUE("rescue", CompanionRole.GIFTED),
    SCOUT_ROUTE("scout", CompanionRole.SCOUT),
    SCOUT_GROUNDING("ground", CompanionRole.SCOUT),
    SCOUT_ANCHOR_POINT("anchor_point", CompanionRole.SCOUT),
    SCOUT_BREAK_FREE("break_free", CompanionRole.SCOUT),
    SCOUT_ESCAPE_WINDOW("escape_window", CompanionRole.SCOUT),
    GUARDIAN_BRACE("brace", CompanionRole.GUARDIAN),
    SCOUT_SIGNAL("signal", CompanionRole.SCOUT);

    private final String id;
    private final CompanionRole role;

    CompanionAbility(final String id, final CompanionRole role) {
        this.id = id;
        this.role = role;
    }

    public String id() {
        return id;
    }

    public CompanionRole role() {
        return role;
    }

    public static Optional<CompanionAbility> parse(final String input) {
        final String normalized = input.toLowerCase(Locale.ROOT).trim();
        return Arrays.stream(values()).filter(ability -> ability.id.equals(normalized)).findFirst();
    }
}
