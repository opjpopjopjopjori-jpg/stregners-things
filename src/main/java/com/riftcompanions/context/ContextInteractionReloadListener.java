package com.riftcompanions.context;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.riftcompanions.RiftCompanions;
import com.riftcompanions.entity.CompanionAction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Loads bounded authored context scenes from data/riftcompanions/contextual_interactions. */
public final class ContextInteractionReloadListener extends SimpleJsonResourceReloadListener {
    // Gson must exist before singleton construction because the superclass captures it.
    private static final Gson GSON = new Gson();
    public static final ContextInteractionReloadListener INSTANCE = new ContextInteractionReloadListener();

    private ContextInteractionReloadListener() {
        super(GSON, "contextual_interactions");
    }

    @Override
    protected void apply(final Map<ResourceLocation, JsonElement> resources, final ResourceManager manager, final ProfilerFiller profiler) {
        final List<ContextInteractionDefinition> rebuilt = new ArrayList<>();
        for (final Map.Entry<ResourceLocation, JsonElement> source : resources.entrySet()) {
            try {
                if (!source.getValue().isJsonObject()) throw new IllegalArgumentException("root must be an object");
                final JsonObject root = source.getValue().getAsJsonObject();
                if (!root.has("schema_version") || root.get("schema_version").getAsInt() != 2) {
                    throw new IllegalArgumentException("unsupported schema_version");
                }
                final JsonArray entries = root.has("entries") && root.get("entries").isJsonArray()
                        ? root.getAsJsonArray("entries") : new JsonArray();
                final List<ContextInteractionDefinition> oneFile = new ArrayList<>();
                for (final JsonElement element : entries) {
                    if (!element.isJsonObject()) throw new IllegalArgumentException("entry must be an object");
                    final JsonObject entry = element.getAsJsonObject();
                    final String id = required(entry, "id", 96);
                    final ContextInteractionKind kind = ContextInteractionKind.valueOf(required(entry, "kind", 32));
                    final String match = required(entry, "match", 160);
                    final String group = required(entry, "group", 120);
                    final ContextInteractionStage stage = ContextInteractionStage.valueOf(required(entry, "stage", 32));
                    final String leadTrigger = required(entry, "lead_trigger", 120);
                    final String replyTrigger = optional(entry, "reply_trigger", 120);
                    final CompanionAction action = CompanionAction.valueOf(required(entry, "action", 64));
                    final CompanionAction replyAction = CompanionAction.valueOf(required(entry, "reply_action", 64));
                    final int priority = entry.has("priority") ? entry.get("priority").getAsInt() : 3;
                    final int cooldown = entry.has("cooldown_ticks") ? entry.get("cooldown_ticks").getAsInt() : 2400;
                    final boolean focused = entry.has("requires_focused_monster") && entry.get("requires_focused_monster").getAsBoolean();
                    if (priority < 0 || priority > 4 || cooldown < 100 || cooldown > 24000) {
                        throw new IllegalArgumentException("priority/cooldown out of bounds");
                    }
                    oneFile.add(new ContextInteractionDefinition(id, kind, match, group, stage, leadTrigger,
                            replyTrigger, action, replyAction, priority, cooldown, focused));
                }
                rebuilt.addAll(oneFile);
            } catch (final RuntimeException exception) {
                RiftCompanions.LOGGER.warn("Ignoring invalid contextual interaction pack {}: {}", source.getKey(), exception.getMessage());
            }
        }
        ContextInteractionRegistry.replace(rebuilt);
        RiftCompanions.LOGGER.info("Loaded {} contextual interaction scene definitions.", ContextInteractionRegistry.all().size());
    }

    private static String required(final JsonObject object, final String key, final int maximum) {
        final String value = optional(object, key, maximum);
        if (value.isBlank()) throw new IllegalArgumentException("missing " + key);
        return value;
    }

    private static String optional(final JsonObject object, final String key, final int maximum) {
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) return "";
        final String value = object.get(key).getAsString();
        if (value.length() > maximum) throw new IllegalArgumentException("invalid " + key);
        return value;
    }
}
