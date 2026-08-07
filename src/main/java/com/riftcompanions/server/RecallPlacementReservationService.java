package com.riftcompanions.server;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ephemeral server-only destination claims for Safe Recall and companion spawn.
 *
 * <p>A claim is not persisted, does not expose coordinates to the client, and
 * expires quickly. It only prevents two companions owned by the same player
 * from selecting the same nearby recall square in one burst.</p>
 */
public final class RecallPlacementReservationService {
    private static final int MAX_CLAIMS_PER_OWNER = 4;
    private static final Map<UUID, Map<UUID, Reservation>> BY_OWNER = new HashMap<>();

    private RecallPlacementReservationService() {}

    public static boolean isReservedByOther(final UUID owner, final UUID companion, final BlockPos position,
                                            final long now, final double separation) {
        if (owner == null || companion == null || position == null) return false;
        final Map<UUID, Reservation> claims = BY_OWNER.get(owner);
        if (claims == null) return false;
        purge(claims, now);
        final double safeRadius = Math.max(0.75D, separation);
        final double squared = safeRadius * safeRadius;
        for (final Map.Entry<UUID, Reservation> entry : claims.entrySet()) {
            if (entry.getKey().equals(companion)) continue;
            final BlockPos claimed = entry.getValue().position();
            final double dx = claimed.getX() - position.getX();
            final double dz = claimed.getZ() - position.getZ();
            if (dx * dx + dz * dz <= squared && Math.abs(claimed.getY() - position.getY()) <= 1) return true;
        }
        return false;
    }

    public static boolean claim(final UUID owner, final UUID companion, final BlockPos position,
                                final long now, final long expiresAt, final double separation) {
        if (owner == null || companion == null || position == null) return false;
        final Map<UUID, Reservation> claims = BY_OWNER.computeIfAbsent(owner, ignored -> new LinkedHashMap<>());
        purge(claims, now);
        if (isReservedByOther(owner, companion, position, now, separation)) return false;
        claims.put(companion, new Reservation(position.immutable(), Math.max(now + 1L, expiresAt)));
        while (claims.size() > MAX_CLAIMS_PER_OWNER) {
            final Iterator<UUID> iterator = claims.keySet().iterator();
            if (!iterator.hasNext()) break;
            iterator.next();
            iterator.remove();
        }
        return true;
    }

    public static void release(final UUID owner, final UUID companion) {
        if (owner == null || companion == null) return;
        final Map<UUID, Reservation> claims = BY_OWNER.get(owner);
        if (claims == null) return;
        claims.remove(companion);
        if (claims.isEmpty()) BY_OWNER.remove(owner);
    }

    public static void clearOwner(final UUID owner) {
        if (owner != null) BY_OWNER.remove(owner);
    }

    private static void purge(final Map<UUID, Reservation> claims, final long now) {
        final Iterator<Map.Entry<UUID, Reservation>> iterator = claims.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt() <= now) iterator.remove();
        }
    }

    private record Reservation(BlockPos position, long expiresAt) {}
}
