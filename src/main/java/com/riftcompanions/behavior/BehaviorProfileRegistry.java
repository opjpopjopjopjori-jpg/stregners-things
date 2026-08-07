package com.riftcompanions.behavior;

import com.riftcompanions.entity.CompanionRole;

import java.util.Map;
import java.util.Optional;

/** Immutable profile snapshot loaded from safe data content. */
public final class BehaviorProfileRegistry {
    private static volatile Map<CompanionRole, CompanionBehaviorProfile> profiles = Map.of();
    private BehaviorProfileRegistry() {}
    public static void replace(Map<CompanionRole, CompanionBehaviorProfile> values) { profiles = Map.copyOf(values); }
    public static Optional<CompanionBehaviorProfile> profile(CompanionRole role) { return Optional.ofNullable(profiles.get(role)); }
    public static int size() { return profiles.size(); }
}
