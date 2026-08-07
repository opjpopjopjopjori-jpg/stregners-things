package com.riftcompanions.encounter;

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

/** Loads safe dialogue mappings from data/riftcompanions/encounter_dialogue. */
public final class EncounterDialogueReloadListener extends SimpleJsonResourceReloadListener {
    // Initialize Gson first: the singleton constructor passes it to the reload-listener superclass.
    private static final Gson GSON = new Gson();
    public static final EncounterDialogueReloadListener INSTANCE = new EncounterDialogueReloadListener();

    private EncounterDialogueReloadListener() { super(GSON, "encounter_dialogue"); }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> resources, final ResourceManager manager, final ProfilerFiller profiler) {
        final Map<String, EncounterDialogueDefinition> rebuilt = new LinkedHashMap<>();
        for (final Map.Entry<ResourceLocation, JsonElement> source : resources.entrySet()) {
            try {
                if (!source.getValue().isJsonObject()) throw new IllegalArgumentException("root must be object");
                JsonObject root = source.getValue().getAsJsonObject();
                if (root.has("schema_version") && root.get("schema_version").getAsInt() != 1) throw new IllegalArgumentException("unsupported schema_version");
                JsonArray entries = root.has("entries") && root.get("entries").isJsonArray() ? root.getAsJsonArray("entries") : new JsonArray();
                Map<String, EncounterDialogueDefinition> file = new LinkedHashMap<>();
                for (JsonElement element : entries) {
                    if (!element.isJsonObject()) throw new IllegalArgumentException("entry must be object");
                    JsonObject entry = element.getAsJsonObject();
                    String profile = required(entry, "profile", 96);
                    CompanionRole role = CompanionRole.parse(required(entry, "role", 32)).orElseThrow(() -> new IllegalArgumentException("invalid role"));
                    String trigger = required(entry, "trigger", 64);
                    int minRisk = entry.has("min_risk") ? entry.get("min_risk").getAsInt() : 0;
                    if (minRisk < 0 || minRisk > 100) throw new IllegalArgumentException("min_risk out of range");
                    EncounterDialogueDefinition definition = new EncounterDialogueDefinition(profile, role, trigger, minRisk);
                    String key = EncounterDialogueRegistry.key(profile, role);
                    if (file.putIfAbsent(key, definition) != null || rebuilt.containsKey(key)) throw new IllegalArgumentException("duplicate " + key);
                }
                rebuilt.putAll(file);
            } catch (RuntimeException exception) {
                RiftCompanions.LOGGER.warn("Ignoring invalid encounter dialogue pack {}: {}", source.getKey(), exception.getMessage());
            }
        }
        EncounterDialogueRegistry.replace(rebuilt);
        RiftCompanions.LOGGER.info("Loaded {} encounter dialogue mappings.", rebuilt.size());
    }

    private static String required(JsonObject object, String key, int max) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) throw new IllegalArgumentException("missing " + key);
        String value = object.get(key).getAsString();
        if (value.isBlank() || value.length() > max) throw new IllegalArgumentException("invalid " + key);
        return value;
    }
}
