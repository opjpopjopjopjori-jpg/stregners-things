package com.riftcompanions.spatial;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Short interaction-radius reservation created by a real player block use.
 * Companions yield space; they never inspect or access the container itself.
 */
public final class SpatialEtiquetteService {
    private static final Map<UUID, InteractionWindow> WINDOWS = new HashMap<>();

    private SpatialEtiquetteService() {}

    public static void observeInteraction(final ServerPlayer player, final BlockPos position) {
        if (player == null || position == null) return;
        WINDOWS.put(player.getUUID(), new InteractionWindow(position.immutable(), player.level().getGameTime() + 80L));
    }

    public static boolean isInteractionRadiusReserved(final UUID owner, final BlockPos candidate, final long now) {
        if (owner == null || candidate == null) return false;
        final InteractionWindow window = WINDOWS.get(owner);
        if (window == null) return false;
        if (now >= window.expiresAt) {
            WINDOWS.remove(owner);
            return false;
        }
        return window.position.distSqr(candidate) <= 3.0D * 3.0D;
    }

    public static void clear(final UUID owner) {
        if (owner != null) WINDOWS.remove(owner);
    }

    private record InteractionWindow(BlockPos position, long expiresAt) {}
}
