package com.riftcompanions.behavior;

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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Loads bounded personality/tactical metadata; it cannot change safety permissions. */
public final class BehaviorProfileReloadListener extends SimpleJsonResourceReloadListener {
    // Initialize Gson first: the singleton constructor passes it to the reload-listener superclass.
    private static final Gson GSON = new Gson();
    public static final BehaviorProfileReloadListener INSTANCE = new BehaviorProfileReloadListener();
    private BehaviorProfileReloadListener() { super(GSON, "behavior_profiles"); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        Map<CompanionRole, CompanionBehaviorProfile> rebuilt = new EnumMap<>(CompanionRole.class);
        for (Map.Entry<ResourceLocation, JsonElement> source : resources.entrySet()) {
            try {
                if (!source.getValue().isJsonObject()) throw new IllegalArgumentException("root must be object");
                JsonObject root = source.getValue().getAsJsonObject();
                if (root.has("schema_version") && root.get("schema_version").getAsInt() != 1) throw new IllegalArgumentException("unsupported schema_version");
                JsonArray entries = root.has("entries") && root.get("entries").isJsonArray() ? root.getAsJsonArray("entries") : new JsonArray();
                Map<CompanionRole, CompanionBehaviorProfile> file = new EnumMap<>(CompanionRole.class);
                for (JsonElement element : entries) {
                    if (!element.isJsonObject()) throw new IllegalArgumentException("entry must be object");
                    JsonObject entry = element.getAsJsonObject();
                    CompanionRole role = CompanionRole.parse(required(entry,"role",32)).orElseThrow(() -> new IllegalArgumentException("invalid role"));
                    CompanionBehaviorProfile profile = new CompanionBehaviorProfile(role, required(entry,"formation_bias",64),
                            stringList(entry,"priority_domains",8,48), stringList(entry,"forbidden_actions",8,48), required(entry,"dialogue_style",64));
                    if (file.putIfAbsent(role, profile) != null || rebuilt.containsKey(role)) throw new IllegalArgumentException("duplicate role " + role);
                }
                rebuilt.putAll(file);
            } catch (RuntimeException exception) {
                RiftCompanions.LOGGER.warn("Ignoring invalid behavior profile pack {}: {}", source.getKey(), exception.getMessage());
            }
        }
        BehaviorProfileRegistry.replace(rebuilt);
        RiftCompanions.LOGGER.info("Loaded {} companion behavior profiles.", rebuilt.size());
    }

    private static String required(JsonObject object,String key,int max) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) throw new IllegalArgumentException("missing "+key);
        String value=object.get(key).getAsString(); if(value.isBlank()||value.length()>max) throw new IllegalArgumentException("invalid "+key); return value;
    }
    private static List<String> stringList(JsonObject object,String key,int max,int maxLen) {
        if (!object.has(key) || !object.get(key).isJsonArray()) return List.of();
        List<String> values=new ArrayList<>();
        for(JsonElement e:object.getAsJsonArray(key)) { if(!e.isJsonPrimitive()) throw new IllegalArgumentException("invalid "+key); String v=e.getAsString(); if(v.isBlank()||v.length()>maxLen) throw new IllegalArgumentException("invalid "+key); if(values.size()<max) values.add(v); }
        return List.copyOf(values);
    }
}
