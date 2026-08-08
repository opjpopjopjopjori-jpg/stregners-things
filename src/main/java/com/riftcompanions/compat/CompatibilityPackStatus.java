package com.riftcompanions.compat;

/** Public diagnostic status. Errors never grant a compatibility behavior. */
public record CompatibilityPackStatus(
        String id,
        String targetMod,
        CompatibilityLevel level,
        boolean loaded,
        String note
) {}
