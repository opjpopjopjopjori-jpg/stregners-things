package com.riftcompanions.dialogue;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Loads data/riftcompanions/companions_dialogue/*.json on world load and /reload. */
public final class DialogueReloadListener extends SimpleJsonResourceReloadListener {
    // Initialize Gson first: the singleton constructor passes it to the reload-listener superclass.
    private static final Gson GSON = new Gson();
    public static final DialogueReloadListener INSTANCE = new DialogueReloadListener();
    private volatile Map<String, List<DialogueLine>> linesByKey = Map.of();

    private DialogueReloadListener() {
        super(GSON, "companions_dialogue");
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> entries, final ResourceManager resourceManager, final ProfilerFiller profiler) {
        final Map<String, List<DialogueLine>> rebuilt = new HashMap<>();
        int accepted = 0;
        for (final Map.Entry<ResourceLocation, JsonElement> resource : entries.entrySet()) {
            if (!resource.getValue().isJsonObject()) {
                RiftCompanions.LOGGER.warn("Ignoring non-object dialogue resource {}", resource.getKey());
                continue;
            }
            final JsonObject root = resource.getValue().getAsJsonObject();
            final JsonArray lines = root.has("entries") && root.get("entries").isJsonArray() ? root.getAsJsonArray("entries") : new JsonArray();
            for (final JsonElement candidate : lines) {
                final Optional<DialogueLine> parsed = parse(candidate, resource.getKey());
                if (parsed.isEmpty()) {
                    continue;
                }
                final DialogueLine line = parsed.get();
                rebuilt.computeIfAbsent(key(line.role(), line.trigger()), ignored -> new ArrayList<>()).add(line);
                accepted++;
            }
        }
        final Map<String, List<DialogueLine>> frozen = new HashMap<>();
        rebuilt.forEach((key, value) -> frozen.put(key, List.copyOf(value)));
        this.linesByKey = Map.copyOf(frozen);
        RiftCompanions.LOGGER.info("Loaded {} Rift Companions dialogue lines across {} trigger groups.", accepted, frozen.size());
    }

    public List<DialogueLine> find(final CompanionRole role, final String trigger) {
        return this.linesByKey.getOrDefault(key(role, trigger), List.of());
    }

    private static Optional<DialogueLine> parse(final JsonElement element, final ResourceLocation source) {
        if (!element.isJsonObject()) {
            return Optional.empty();
        }
        final JsonObject entry = element.getAsJsonObject();
        try {
            final String id = requiredString(entry, "id");
            final CompanionRole role = CompanionRole.parse(requiredString(entry, "role")).orElseThrow();
            final String trigger = requiredString(entry, "trigger");
            final String text = requiredString(entry, "text");
            final int priority = entry.has("priority") ? entry.get("priority").getAsInt() : 2;
            final long cooldown = entry.has("cooldown_ticks") ? entry.get("cooldown_ticks").getAsLong() : 1200L;
            if (priority < 0 || priority > 4 || cooldown < 0 || text.length() > 240) {
                throw new IllegalArgumentException("value range");
            }
            return Optional.of(new DialogueLine(id, role, trigger, priority, cooldown, text));
        } catch (final RuntimeException exception) {
            RiftCompanions.LOGGER.warn("Ignoring invalid dialogue entry in {}: {}", source, exception.getMessage());
            return Optional.empty();
        }
    }

    private static String requiredString(final JsonObject object, final String field) {
        if (!object.has(field) || !object.get(field).isJsonPrimitive()) {
            throw new IllegalArgumentException("missing " + field);
        }
        return object.get(field).getAsString();
    }

    private static String key(final CompanionRole role, final String trigger) {
        return role.id() + "|" + trigger;
    }
}
