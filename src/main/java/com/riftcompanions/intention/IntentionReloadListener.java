package com.riftcompanions.intention;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.riftcompanions.RiftCompanions;
import com.riftcompanions.entity.CompanionRole;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.LinkedHashMap;
import java.util.Map;

/** Loads optional content from data/riftcompanions/companion_intentions/*.json. */
public final class IntentionReloadListener extends SimpleJsonResourceReloadListener {
    // Initialize Gson first: the singleton constructor passes it to the reload-listener superclass.
    private static final Gson GSON = new Gson();
    public static final IntentionReloadListener INSTANCE = new IntentionReloadListener();

    private IntentionReloadListener() { super(GSON, "companion_intentions"); }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> resources, final ResourceManager manager, final ProfilerFiller profiler) {
        final Map<String, IntentionDefinition> rebuilt = new LinkedHashMap<>();
        for (final Map.Entry<ResourceLocation, JsonElement> source : resources.entrySet()) {
            try {
                if (!source.getValue().isJsonObject()) throw new IllegalArgumentException("root must be an object");
                final JsonObject root = source.getValue().getAsJsonObject();
                if (root.has("schema_version") && root.get("schema_version").getAsInt() != 1) {
                    throw new IllegalArgumentException("unsupported schema_version");
                }
                final JsonArray entries = root.has("entries") && root.get("entries").isJsonArray() ? root.getAsJsonArray("entries") : new JsonArray();
                final Map<String, IntentionDefinition> fromOneFile = new LinkedHashMap<>();
                for (final JsonElement element : entries) {
                    final IntentionDefinition definition = parse(element);
                    if (fromOneFile.putIfAbsent(definition.id(), definition) != null || rebuilt.containsKey(definition.id())) {
                        throw new IllegalArgumentException("duplicate id " + definition.id());
                    }
                }
                rebuilt.putAll(fromOneFile);
            } catch (final RuntimeException exception) {
                RiftCompanions.LOGGER.warn("Ignoring invalid intention pack {}: {}", source.getKey(), exception.getMessage());
            }
        }
        IntentionDefinitionRegistry.replace(rebuilt);
        RiftCompanions.LOGGER.info("Loaded {} Rift Companions optional intention definitions.", rebuilt.size());
    }

    private static IntentionDefinition parse(final JsonElement value) {
        if (!value.isJsonObject()) throw new IllegalArgumentException("entry must be an object");
        final JsonObject obj = value.getAsJsonObject();
        final String id = required(obj, "id", 64);
        final CompanionRole role = CompanionRole.parse(required(obj, "role", 24)).orElseThrow(() -> new IllegalArgumentException("invalid role"));
        return new IntentionDefinition(id, role, required(obj, "context", 200), required(obj, "optional_action", 200),
                required(obj, "completion_key", 64), required(obj, "reward_summary", 200), required(obj, "dialogue_trigger", 48));
    }

    private static String required(final JsonObject obj, final String key, final int maximum) {
        if (!obj.has(key) || !obj.get(key).isJsonPrimitive()) throw new IllegalArgumentException("missing " + key);
        final String value = obj.get(key).getAsString();
        if (value.isBlank() || value.length() > maximum) throw new IllegalArgumentException("invalid " + key);
        return value;
    }
}
