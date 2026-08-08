package com.riftcompanions.content;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.entity.CompanionRole;

import java.util.EnumMap;
import java.util.Map;

/** Content layer is replaceable without changing Team Director, plans, or powers. */
public final class ContentProfileRegistry {
    private static final Map<CompanionRole, CompanionProfile> PERSONAL = new EnumMap<>(CompanionRole.class);
    private static final Map<CompanionRole, CompanionProfile> PUBLIC = new EnumMap<>(CompanionRole.class);

    static {
        PERSONAL.put(CompanionRole.SEER, new CompanionProfile("Will / Seer", "will_seer.png"));
        PERSONAL.put(CompanionRole.GUARDIAN, new CompanionProfile("Hopper / Guardian", "hopper_sheriff.png"));
        PERSONAL.put(CompanionRole.GIFTED, new CompanionProfile("Eleven / Gifted", "eleven_gifted.png"));
        PERSONAL.put(CompanionRole.SCOUT, new CompanionProfile("Max / Scout", "max_scout.png"));

        PUBLIC.put(CompanionRole.SEER, new CompanionProfile("Seer", "seer_public.png"));
        PUBLIC.put(CompanionRole.GUARDIAN, new CompanionProfile("Guardian", "guardian_public.png"));
        PUBLIC.put(CompanionRole.GIFTED, new CompanionProfile("Gifted", "gifted_public.png"));
        PUBLIC.put(CompanionRole.SCOUT, new CompanionProfile("Scout", "scout_public.png"));
    }

    private ContentProfileRegistry() {}

    public static CompanionProfile profile(final CompanionRole role) {
        final Map<CompanionRole, CompanionProfile> active = CompanionConfig.CONTENT_MODE.get() == ContentMode.PUBLIC ? PUBLIC : PERSONAL;
        return active.getOrDefault(role, new CompanionProfile(role.id(), role.id() + "_public.png"));
    }
}
