package com.riftcompanions.context;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable server-side authored scene lookup replaced atomically after reload. */
public final class ContextInteractionRegistry {
    private static volatile Map<ContextInteractionKind, Map<String, List<ContextInteractionDefinition>>> DEFINITIONS = Map.of();

    private ContextInteractionRegistry() {}

    public static void replace(final List<ContextInteractionDefinition> definitions) {
        final EnumMap<ContextInteractionKind, Map<String, List<ContextInteractionDefinition>>> rebuilt = new EnumMap<>(ContextInteractionKind.class);
        for (final ContextInteractionKind kind : ContextInteractionKind.values()) rebuilt.put(kind, new LinkedHashMap<>());
        if (definitions != null) {
            for (final ContextInteractionDefinition definition : definitions) {
                if (definition == null || definition.id().isBlank() || definition.match().isBlank() || definition.leadTrigger().isBlank()) continue;
                final List<ContextInteractionDefinition> entries = rebuilt.get(definition.kind())
                        .computeIfAbsent(definition.match(), ignored -> new ArrayList<>());
                if (entries.stream().noneMatch(existing -> existing.id().equals(definition.id()))) entries.add(definition);
            }
        }
        final EnumMap<ContextInteractionKind, Map<String, List<ContextInteractionDefinition>>> immutable = new EnumMap<>(ContextInteractionKind.class);
        for (final Map.Entry<ContextInteractionKind, Map<String, List<ContextInteractionDefinition>>> entry : rebuilt.entrySet()) {
            final Map<String, List<ContextInteractionDefinition>> values = new LinkedHashMap<>();
            entry.getValue().forEach((match, scenes) -> values.put(match, List.copyOf(scenes)));
            immutable.put(entry.getKey(), Map.copyOf(values));
        }
        DEFINITIONS = Map.copyOf(immutable);
    }

    public static Optional<ContextInteractionDefinition> find(final ContextInteractionKind kind, final String match,
                                                               final ContextInteractionStage stage) {
        if (kind == null || match == null || match.isBlank() || stage == null) return Optional.empty();
        return DEFINITIONS.getOrDefault(kind, Map.of()).getOrDefault(match, List.of()).stream()
                .filter(definition -> definition.stage() == stage).findFirst();
    }

    public static List<ContextInteractionDefinition> all() {
        final List<ContextInteractionDefinition> result = new ArrayList<>();
        DEFINITIONS.values().forEach(byMatch -> byMatch.values().forEach(result::addAll));
        return List.copyOf(result);
    }
}
