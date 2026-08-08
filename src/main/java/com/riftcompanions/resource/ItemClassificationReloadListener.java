package com.riftcompanions.resource;

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

/** Safe optional data pack for item labels under data/riftcompanions/item_classifications. */
public final class ItemClassificationReloadListener extends SimpleJsonResourceReloadListener {
    // Initialize Gson first: the singleton constructor passes it to the reload-listener superclass.
    private static final Gson GSON = new Gson();
    public static final ItemClassificationReloadListener INSTANCE = new ItemClassificationReloadListener();

    private ItemClassificationReloadListener() { super(GSON, "item_classifications"); }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> resources, final ResourceManager manager, final ProfilerFiller profiler) {
        final Map<ResourceLocation, ItemCategory> rebuilt = new LinkedHashMap<>();
        for (final Map.Entry<ResourceLocation, JsonElement> source : resources.entrySet()) {
            try {
                if (!source.getValue().isJsonObject()) throw new IllegalArgumentException("root must be an object");
                final JsonObject root = source.getValue().getAsJsonObject();
                if (root.has("schema_version") && root.get("schema_version").getAsInt() != 1) {
                    throw new IllegalArgumentException("unsupported schema_version");
                }
                final JsonArray entries = root.has("entries") && root.get("entries").isJsonArray() ? root.getAsJsonArray("entries") : new JsonArray();
                final Map<ResourceLocation, ItemCategory> fromOneFile = new LinkedHashMap<>();
                for (final JsonElement element : entries) {
                    if (!element.isJsonObject()) throw new IllegalArgumentException("entry must be an object");
                    final JsonObject entry = element.getAsJsonObject();
                    if (!entry.has("item") || !entry.has("category")) throw new IllegalArgumentException("missing item/category");
                    final ResourceLocation item = new ResourceLocation(entry.get("item").getAsString());
                    final ItemCategory category = ItemCategory.valueOf(entry.get("category").getAsString());
                    fromOneFile.put(item, category);
                }
                rebuilt.putAll(fromOneFile);
            } catch (final RuntimeException exception) {
                RiftCompanions.LOGGER.warn("Ignoring invalid item classification pack {}: {}", source.getKey(), exception.getMessage());
            }
        }
        ItemClassificationRegistry.replace(rebuilt);
        RiftCompanions.LOGGER.info("Loaded {} safe item classification overrides.", rebuilt.size());
    }
}
