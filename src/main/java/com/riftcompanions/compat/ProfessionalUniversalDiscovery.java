package com.riftcompanions.compat;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Professional Universal Discovery System v3.0
 * Auto-discovers any namespace, validates features against categories,
 * and builds AI-ready structured data for story generation.
 */
public final class ProfessionalUniversalDiscovery {
    private ProfessionalUniversalDiscovery() {}

    public enum FeatureType { MOB, BIOME, STRUCTURE, ITEM, BLOCK, WEAPON, MAGIC, WEATHER, DIMENSION, ENTITY, EFFECT }

    public static class DiscoveredFeature {
        public final String id;
        public final FeatureType type;
        public final String namespace;
        public final String description;
        public final boolean hostile;
        public final boolean magical;

        public DiscoveredFeature(String id, FeatureType type, String namespace, String desc, boolean hostile, boolean magical) {
            this.id = id; this.type = type; this.namespace = namespace; this.description = desc; this.hostile = hostile; this.magical = magical;
        }
    }

    public static List<DiscoveredFeature> discoverAll(final String namespace) {
        Set<String> raw = UniversalModIntegrationEngine.discoverFeatures(namespace);
        List<DiscoveredFeature> result = new ArrayList<>();
        for (String feature : raw) {
            FeatureType type = classifyFeature(feature);
            result.add(new DiscoveredFeature(feature, type, namespace,
                "Discovered from namespace: " + namespace, isHostile(type, feature), isMagical(feature)));
        }
        return result;
    }

    private static FeatureType classifyFeature(String feature) {
        if (feature.contains("dragon") || feature.contains("troll") || feature.contains("hydra")) return FeatureType.MOB;
        if (feature.contains("biome")) return FeatureType.BIOME;
        if (feature.contains("structure") || feature.contains("tower") || feature.contains("cave")) return FeatureType.STRUCTURE;
        if (feature.contains("sword") || feature.contains("weapon")) return FeatureType.WEAPON;
        if (feature.contains("item")) return FeatureType.ITEM;
        if (feature.contains("magic") || feature.contains("spell") || feature.contains("shard")) return FeatureType.MAGIC;
        if (feature.contains("weather") || feature.contains("blizzard")) return FeatureType.WEATHER;
        return FeatureType.ENTITY;
    }

    private static boolean isHostile(FeatureType type, String feature) {
        return (type == FeatureType.MOB && (feature.contains("dragon") || feature.contains("death") || feature.contains("troll")));
    }

    private static boolean isMagical(String feature) {
        return feature.contains("magic") || feature.contains("spell") || feature.contains("ice_shard") || feature.contains("fire_ball");
    }

    public static String buildProfessionalPrompt(List<DiscoveredFeature> features) {
        StringBuilder sb = new StringBuilder();
        sb.append("PROFESSIONAL INTEGRATION DATA:\n");
        for (DiscoveredFeature f : features) {
            sb.append("- [").append(f.type).append("] ").append(f.namespace).append(":").append(f.id)
              .append(" (hostile=").append(f.hostile).append(", magical=").append(f.magical).append(")\n");
        }
        sb.append("Generate a complete 5-chapter story using ONLY these discovered elements. No errors. Every dialogue references actual discovered features.");
        return sb.toString();
    }
}
