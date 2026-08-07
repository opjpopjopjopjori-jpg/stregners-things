package com.riftcompanions.compat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry deliberately falls back to Unknown/Caution when a pack is missing or invalid. */
public final class CompatibilityPackRegistry {
    private static volatile Map<String, CompatibilityPackStatus> statuses = Map.of(
            "vanilla", new CompatibilityPackStatus("vanilla", "minecraft", CompatibilityLevel.VERIFIED_VANILLA, true, "Built-in vanilla baseline")
    );

    private CompatibilityPackRegistry() {}

    public static void replace(final Map<String, CompatibilityPackStatus> packs) {
        LinkedHashMap<String, CompatibilityPackStatus> next = new LinkedHashMap<>();
        next.put("vanilla", new CompatibilityPackStatus("vanilla", "minecraft", CompatibilityLevel.VERIFIED_VANILLA, true, "Built-in vanilla baseline"));
        next.putAll(packs);
        statuses = Map.copyOf(next);
    }

    public static CompatibilityPackStatus statusForNamespace(final String namespace) {
        return statuses.values().stream()
                .filter(status -> status.targetMod().equals(namespace))
                .findFirst()
                .orElse(new CompatibilityPackStatus("unknown:" + namespace, namespace, CompatibilityLevel.EXPERIMENTAL, false,
                        "No valid compatibility pack; use Unknown/Caution behavior"));
    }

    public static List<CompatibilityPackStatus> allStatuses() {
        return statuses.values().stream().sorted(java.util.Comparator.comparing(CompatibilityPackStatus::id)).toList();
    }
}
