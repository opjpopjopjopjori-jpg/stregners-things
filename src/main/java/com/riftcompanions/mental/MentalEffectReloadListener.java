package com.riftcompanions.mental;

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

/** Loads explicit supported mental effects from data/riftcompanions/mental_effects. */
public final class MentalEffectReloadListener extends SimpleJsonResourceReloadListener {
    // Initialize Gson first: the singleton constructor passes it to the reload-listener superclass.
    private static final Gson GSON = new Gson();
    public static final MentalEffectReloadListener INSTANCE = new MentalEffectReloadListener();

    private MentalEffectReloadListener() { super(GSON, "mental_effects"); }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> resources, final ResourceManager manager, final ProfilerFiller profiler) {
        final Map<ResourceLocation, MentalEffectProfile> rebuilt = new LinkedHashMap<>();
        for (final Map.Entry<ResourceLocation, JsonElement> source : resources.entrySet()) {
            try {
                if (!source.getValue().isJsonObject()) throw new IllegalArgumentException("root must be object");
                final JsonObject root = source.getValue().getAsJsonObject();
                if (root.has("schema_version") && root.get("schema_version").getAsInt() != 1) throw new IllegalArgumentException("unsupported schema_version");
                final JsonArray entries = root.has("entries") && root.get("entries").isJsonArray() ? root.getAsJsonArray("entries") : new JsonArray();
                final Map<ResourceLocation, MentalEffectProfile> oneFile = new LinkedHashMap<>();
                for (final JsonElement element : entries) {
                    if (!element.isJsonObject()) throw new IllegalArgumentException("entry must be object");
                    final JsonObject entry = element.getAsJsonObject();
                    final ResourceLocation effect = new ResourceLocation(required(entry, "effect", 128));
                    if (!net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.containsKey(effect)) {
                        throw new IllegalArgumentException("unknown effect " + effect);
                    }
                    final int priority = entry.has("priority") ? entry.get("priority").getAsInt() : 50;
                    if (priority < 0 || priority > 100) throw new IllegalArgumentException("priority out of range");
                    final MentalEffectProfile profile = new MentalEffectProfile(effect, priority);
                    if (oneFile.putIfAbsent(effect, profile) != null || rebuilt.containsKey(effect)) throw new IllegalArgumentException("duplicate effect " + effect);
                }
                rebuilt.putAll(oneFile);
            } catch (final RuntimeException exception) {
                RiftCompanions.LOGGER.warn("Ignoring invalid mental effect pack {}: {}", source.getKey(), exception.getMessage());
            }
        }
        MentalEffectRegistry.replace(rebuilt);
        RiftCompanions.LOGGER.info("Loaded {} supported mental effect profiles.", rebuilt.size());
    }

    private static String required(final JsonObject object, final String key, final int maximum) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) throw new IllegalArgumentException("missing " + key);
        final String value = object.get(key).getAsString();
        if (value.isBlank() || value.length() > maximum) throw new IllegalArgumentException("invalid " + key);
        return value;
    }
}
