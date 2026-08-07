package com.riftcompanions.encounter;

import com.riftcompanions.entity.CompanionRole;

import java.util.Map;
import java.util.Optional;

/** Immutable reload snapshot for encounter-to-dialogue mappings. */
public final class EncounterDialogueRegistry {
    private static volatile Map<String, EncounterDialogueDefinition> definitions = Map.of();

    private EncounterDialogueRegistry() {}

    public static void replace(final Map<String, EncounterDialogueDefinition> values) { definitions = Map.copyOf(values); }
    public static Optional<EncounterDialogueDefinition> find(final String profileId, final CompanionRole role) {
        return Optional.ofNullable(definitions.get(key(profileId, role)));
    }
    public static int size() { return definitions.size(); }
    public static String key(final String profileId, final CompanionRole role) { return profileId + "|" + role.name(); }
}
