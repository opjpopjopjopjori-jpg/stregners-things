package com.riftcompanions.team.events;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Optional;

/** Merges duplicate events and lets P0 suppress lower-priority chatter/work. */
public final class TeamEventQueue {
    private final EnumMap<TeamEventType, TeamEvent> pending = new EnumMap<>(TeamEventType.class);

    public void submit(final TeamEvent event) {
        if (event == null) {
            return;
        }
        pending.merge(event.type(), event, TeamEvent::merge);
    }

    public Optional<TeamEvent> takeHighest(final long now) {
        expire(now);
        return pending.values().stream()
                .min(Comparator.comparingInt((TeamEvent event) -> event.priority().weight()).thenComparingLong(TeamEvent::createdAt))
                .map(event -> {
                    pending.remove(event.type());
                    return event;
                });
    }

    public boolean hasPriorityAtLeast(final TeamEventPriority priority, final long now) {
        expire(now);
        return pending.values().stream().anyMatch(event -> event.priority().weight() <= priority.weight());
    }

    public void expire(final long now) {
        pending.values().removeIf(event -> event.expired(now));
    }

    public void clear() {
        pending.clear();
    }

    public int size() {
        return pending.size();
    }
}
