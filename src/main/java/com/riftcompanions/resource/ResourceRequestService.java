package com.riftcompanions.resource;

import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionEntity;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded request barks; companions ask once rather than narrating every item. */
public final class ResourceRequestService {
    private static final Map<UUID, Long> LAST_REQUEST = new HashMap<>();
    private static final long REQUEST_COOLDOWN = 1200L;

    private ResourceRequestService() {}

    public static void request(final ServerPlayer player, final CompanionEntity companion, final ItemCategory category) {
        if (player == null || companion == null) return;
        final long now = player.level().getGameTime();
        final Long previous = LAST_REQUEST.get(player.getUUID());
        if (previous != null && now - previous < REQUEST_COOLDOWN) return;
        LAST_REQUEST.put(player.getUUID(), now);
        final String trigger = switch (category) {
            case HEALING -> "resource_request_healing";
            case PERSONAL_AMMO -> "resource_request_ammo";
            case QUEST_OR_MEMORY_ITEM, UNKNOWN -> "resource_request_unknown";
            default -> "resource_request";
        };
        DialogueService.get().speak(companion, trigger, 3);
    }

    public static void clear(final UUID owner) {
        if (owner != null) LAST_REQUEST.remove(owner);
    }
}
