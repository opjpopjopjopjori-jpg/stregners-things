package com.riftcompanions.world.assessment;

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

/** Loads observed-block structure profile overrides from data packs safely. */
public final class StructureProfileOverrideReloadListener extends SimpleJsonResourceReloadListener {
    // Initialize Gson first: the singleton constructor passes it to the reload-listener superclass.
    private static final Gson GSON = new Gson();
    public static final StructureProfileOverrideReloadListener INSTANCE = new StructureProfileOverrideReloadListener();

    private StructureProfileOverrideReloadListener() { super(GSON, "structure_overrides"); }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> resources, final ResourceManager manager, final ProfilerFiller profiler) {
        final Map<ResourceLocation, StructureProfileOverride> rebuilt = new LinkedHashMap<>();
        for (final Map.Entry<ResourceLocation, JsonElement> source : resources.entrySet()) {
            try {
                if (!source.getValue().isJsonObject()) throw new IllegalArgumentException("root must be object");
                final JsonObject root = source.getValue().getAsJsonObject();
                if (root.has("schema_version") && root.get("schema_version").getAsInt() != 1) throw new IllegalArgumentException("unsupported schema_version");
                final JsonArray entries = root.has("entries") && root.get("entries").isJsonArray() ? root.getAsJsonArray("entries") : new JsonArray();
                final Map<ResourceLocation, StructureProfileOverride> fromOneFile = new LinkedHashMap<>();
                for (final JsonElement element : entries) {
                    final StructureProfileOverride override = parse(element);
                    if (fromOneFile.putIfAbsent(override.block(), override) != null || rebuilt.containsKey(override.block())) {
                        throw new IllegalArgumentException("duplicate block " + override.block());
                    }
                }
                rebuilt.putAll(fromOneFile);
            } catch (final RuntimeException exception) {
                RiftCompanions.LOGGER.warn("Ignoring invalid structure override pack {}: {}", source.getKey(), exception.getMessage());
            }
        }
        StructureProfileOverrideRegistry.replace(rebuilt);
        RiftCompanions.LOGGER.info("Loaded {} observed-block structure overrides.", rebuilt.size());
    }

    private static StructureProfileOverride parse(final JsonElement element) {
        if (!element.isJsonObject()) throw new IllegalArgumentException("entry must be object");
        final JsonObject entry = element.getAsJsonObject();
        final ResourceLocation block = new ResourceLocation(required(entry, "block", 128));
        if (!net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(block)) {
            throw new IllegalArgumentException("unknown block " + block);
        }
        final String profile = required(entry, "profile_id", 32);
        final int caution = entry.has("caution_bias") ? entry.get("caution_bias").getAsInt() : 0;
        if (caution < 0 || caution > 30) throw new IllegalArgumentException("caution_bias out of range");
        return new StructureProfileOverride(block, profile, caution);
    }

    private static String required(final JsonObject object, final String key, final int maximum) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) throw new IllegalArgumentException("missing " + key);
        final String value = object.get(key).getAsString();
        if (value.isBlank() || value.length() > maximum) throw new IllegalArgumentException("invalid " + key);
        return value;
    }
}
