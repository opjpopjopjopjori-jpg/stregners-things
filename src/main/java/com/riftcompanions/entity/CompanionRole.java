package com.riftcompanions.entity;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Logic stays original/generic; the personal presentation layer maps these roles to the supplied characters. */
public enum CompanionRole {
    SEER("seer", "Will / Seer"),
    GUARDIAN("guardian", "Hopper / Guardian"),
    GIFTED("gifted", "Eleven / Gifted"),
    SCOUT("scout", "Max / Scout");

    private final String id;
    private final String personalName;

    CompanionRole(final String id, final String personalName) {
        this.id = id;
        this.personalName = personalName;
    }

    public String id() {
        return id;
    }

    public String personalName() {
        return com.riftcompanions.content.ContentProfileRegistry.profile(this).displayName();
    }

    public static Optional<CompanionRole> parse(final String value) {
        final String normalized = value.toLowerCase(Locale.ROOT).trim();
        return Arrays.stream(values()).filter(role -> role.id.equals(normalized)).findFirst();
    }
}
