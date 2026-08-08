package com.riftcompanions.compat;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.riftcompanions.RiftCompanions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.fml.ModList;

import java.util.LinkedHashMap;
import java.util.Map;

/** Loads optional data/riftcompanions/compatibility_packs/*.json definitions safely. */
public final class CompatibilityPackReloadListener extends SimpleJsonResourceReloadListener {
    // Initialize Gson first: the singleton constructor passes it to the reload-listener superclass.
    private static final Gson GSON = new Gson();
    public static final CompatibilityPackReloadListener INSTANCE = new CompatibilityPackReloadListener();

    private CompatibilityPackReloadListener() {
        super(GSON, "compatibility_packs");
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> entries, final ResourceManager manager, final ProfilerFiller profiler) {
        Map<String, CompatibilityPackStatus> loaded = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> source : entries.entrySet()) {
            try {
                if (!source.getValue().isJsonObject()) throw new IllegalArgumentException("root must be an object");
                JsonObject obj = source.getValue().getAsJsonObject();
                String id = required(obj, "id");
                String targetMod = required(obj, "target_mod");
                CompatibilityLevel level = CompatibilityLevel.valueOf(required(obj, "level"));
                boolean modLoaded = ModList.get().isLoaded(targetMod);
                loaded.put(id, new CompatibilityPackStatus(id, targetMod, level, modLoaded,
                        modLoaded ? "Loaded" : "Target mod not present; safely inactive"));
            } catch (RuntimeException exception) {
                RiftCompanions.LOGGER.warn("Compatibility pack {} failed to load; Unknown/Caution fallback remains active: {}", source.getKey(), exception.getMessage());
            }
        }
        CompatibilityPackRegistry.replace(loaded);
    }

    private static String required(final JsonObject obj, final String field) {
        if (!obj.has(field) || !obj.get(field).isJsonPrimitive()) throw new IllegalArgumentException("missing " + field);
        return obj.get(field).getAsString();
    }
}
