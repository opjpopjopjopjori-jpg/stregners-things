package com.riftcompanions.team.events;

import net.minecraft.core.BlockPos;

import java.util.List;

/** A compact transient event. Only final plan/memory summaries are persisted. */
public record TeamEvent(
        TeamEventType type,
        TeamEventPriority priority,
        long createdAt,
        long expiresAt,
        BlockPos location,
        List<String> reasonCodes
) {
    public TeamEvent {
        location = location == null ? null : location.immutable();
        reasonCodes = List.copyOf(reasonCodes == null ? List.of() : reasonCodes.stream().limit(8).toList());
    }

    public static TeamEvent create(final TeamEventType type, final long now, final BlockPos location, final List<String> reasons) {
        return new TeamEvent(type, type.priority(), now, now + type.defaultLifetime(), location, reasons);
    }

    public boolean expired(final long now) {
        return now >= expiresAt;
    }

    public TeamEvent merge(final TeamEvent newer) {
        final long expiry = Math.max(expiresAt, newer.expiresAt);
        final BlockPos mergedLocation = newer.location != null ? newer.location : location;
        final List<String> mergedReasons = java.util.stream.Stream.concat(reasonCodes.stream(), newer.reasonCodes.stream())
                .distinct().limit(8).toList();
        final TeamEventPriority mergedPriority = priority.weight() <= newer.priority.weight() ? priority : newer.priority;
        return new TeamEvent(type, mergedPriority, Math.min(createdAt, newer.createdAt), expiry, mergedLocation, mergedReasons);
    }
}
