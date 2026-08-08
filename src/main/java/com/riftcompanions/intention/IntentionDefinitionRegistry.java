package com.riftcompanions.intention;

import com.riftcompanions.entity.CompanionRole;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable snapshot replaced only after a fully valid reload pass. */
public final class IntentionDefinitionRegistry {
    private static volatile Map<String, IntentionDefinition> definitions = Map.of();

    private IntentionDefinitionRegistry() {}

    public static void replace(final Map<String, IntentionDefinition> accepted) {
        definitions = Map.copyOf(accepted);
    }

    public static Optional<IntentionDefinition> get(final String id) {
        return Optional.ofNullable(definitions.get(id));
    }

    public static List<IntentionDefinition> forRole(final CompanionRole role) {
        return definitions.values().stream().filter(definition -> definition.role() == role)
                .sorted(Comparator.comparing(IntentionDefinition::id)).toList();
    }

    public static int size() { return definitions.size(); }
}
