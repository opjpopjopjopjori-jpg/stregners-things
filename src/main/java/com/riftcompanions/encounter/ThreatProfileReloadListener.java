package com.riftcompanions.encounter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.riftcompanions.RiftCompanions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.LinkedHashMap;
import java.util.Map;

/** Loads data/riftcompanions/threat_profiles/*.json without changing safety gates. */
public final class ThreatProfileReloadListener extends SimpleJsonResourceReloadListener {
    // Initialize Gson first: the singleton constructor passes it to the reload-listener superclass.
    private static final Gson GSON = new Gson();
    public static final ThreatProfileReloadListener INSTANCE = new ThreatProfileReloadListener();

    private ThreatProfileReloadListener() { super(GSON, "threat_profiles"); }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> resources, final ResourceManager manager, final ProfilerFiller profiler) {
        final Map<ResourceLocation, ThreatProfileDefinition> rebuilt = new LinkedHashMap<>();
        for (final Map.Entry<ResourceLocation, JsonElement> source : resources.entrySet()) {
            try {
                if (!source.getValue().isJsonObject()) throw new IllegalArgumentException("root must be an object");
                final JsonObject root = source.getValue().getAsJsonObject();
                if (root.has("schema_version") && root.get("schema_version").getAsInt() != 1) {
                    throw new IllegalArgumentException("unsupported schema_version");
                }
                final JsonArray entries = root.has("entries") && root.get("entries").isJsonArray() ? root.getAsJsonArray("entries") : new JsonArray();
                final Map<ResourceLocation, ThreatProfileDefinition> fromOneFile = new LinkedHashMap<>();
                for (final JsonElement element : entries) {
                    final ThreatProfileDefinition definition = parse(element);
                    if (fromOneFile.putIfAbsent(definition.entityType(), definition) != null || rebuilt.containsKey(definition.entityType())) {
                        throw new IllegalArgumentException("duplicate entity_type " + definition.entityType());
                    }
                }
                rebuilt.putAll(fromOneFile);
            } catch (final RuntimeException exception) {
                RiftCompanions.LOGGER.warn("Ignoring invalid threat profile pack {}: {}", source.getKey(), exception.getMessage());
            }
        }
        ThreatProfileRegistry.replace(rebuilt);
        RiftCompanions.LOGGER.info("Loaded {} advisory threat profiles.", rebuilt.size());
    }

    private static ThreatProfileDefinition parse(final JsonElement element) {
        if (!element.isJsonObject()) throw new IllegalArgumentException("entry must be object");
        final JsonObject entry = element.getAsJsonObject();
        final String id = required(entry, "id", 64);
        final ResourceLocation entityType = new ResourceLocation(required(entry, "entity_type", 128));
        if (!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(entityType)) {
            throw new IllegalArgumentException("unknown entity_type " + entityType);
        }
        final ThreatArchetype archetype = ThreatArchetype.valueOf(required(entry, "archetype", 48));
        final int cautionBias = entry.has("caution_bias") ? entry.get("caution_bias").getAsInt() : 0;
        if (cautionBias < 0 || cautionBias > 30) throw new IllegalArgumentException("caution_bias out of range");
        final String observableTell = optional(entry, "observable_tell", 120);
        final String counterplay = optional(entry, "counterplay", 120);
        return new ThreatProfileDefinition(id, entityType, archetype, cautionBias, observableTell, counterplay);
    }

    private static String optional(final JsonObject object, final String key, final int maximum) {
        if (!object.has(key)) return "";
        if (!object.get(key).isJsonPrimitive()) throw new IllegalArgumentException("invalid " + key);
        final String value = object.get(key).getAsString();
        if (value.length() > maximum) throw new IllegalArgumentException("invalid " + key);
        return value;
    }

    private static String required(final JsonObject object, final String key, final int maximum) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) throw new IllegalArgumentException("missing " + key);
        final String value = object.get(key).getAsString();
        if (value.isBlank() || value.length() > maximum) throw new IllegalArgumentException("invalid " + key);
        return value;
    }
}
