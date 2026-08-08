package com.riftcompanions.navigation;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Short-lived server-only slot claims prevent a formation from collapsing onto
 * one safe square when vanilla navigation chooses nearby alternatives.
 */
public final class FormationSlotReservationService {
    private static final Map<UUID, Map<UUID, Reservation>> BY_OWNER = new HashMap<>();

    private FormationSlotReservationService() {}

    public static boolean isReservedByOther(final UUID owner, final UUID companion, final BlockPos position,
                                            final long now, final double radius) {
        if (owner == null || companion == null || position == null) return false;
        final Map<UUID, Reservation> claims = BY_OWNER.get(owner);
        if (claims == null) return false;
        purge(claims, now);
        final double square = Math.max(0.5D, radius) * Math.max(0.5D, radius);
        for (final Map.Entry<UUID, Reservation> entry : claims.entrySet()) {
            if (!entry.getKey().equals(companion) && entry.getValue().position.distSqr(position) <= square) return true;
        }
        return false;
    }

    public static boolean claim(final UUID owner, final UUID companion, final BlockPos position,
                                final long now, final long expiresAt, final double radius) {
        if (owner == null || companion == null || position == null) return false;
        final Map<UUID, Reservation> claims = BY_OWNER.computeIfAbsent(owner, ignored -> new HashMap<>());
        purge(claims, now);
        if (isReservedByOther(owner, companion, position, now, radius)) return false;
        claims.put(companion, new Reservation(position.immutable(), Math.max(now + 1L, expiresAt)));
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
            if (iterator.next().getValue().expiresAt <= now) iterator.remove();
        }
    }

    private record Reservation(BlockPos position, long expiresAt) {}
}
