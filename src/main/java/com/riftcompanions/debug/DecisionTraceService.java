package com.riftcompanions.debug;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Bounded per-player decision trace for debugging path/event/plan issues. */
public final class DecisionTraceService {
    private static final int LIMIT = 64;
    private static final Map<UUID, Deque<DecisionTraceEntry>> TRACES = new HashMap<>();

    private DecisionTraceService() {}

    public static void log(final ServerPlayer player, final String category, final String detail) {
        if (player == null) return;
        final Deque<DecisionTraceEntry> trace = TRACES.computeIfAbsent(player.getUUID(), ignored -> new ArrayDeque<>());
        if (trace.size() >= LIMIT) trace.removeFirst();
        trace.addLast(new DecisionTraceEntry(player.level().getGameTime(), trim(category, 48), trim(detail, 180)));
    }

    public static List<DecisionTraceEntry> recent(final UUID owner) {
        return List.copyOf(TRACES.getOrDefault(owner, new ArrayDeque<>()));
    }

    public static void clear(final UUID owner) {
        TRACES.remove(owner);
    }

    private static String trim(final String value, final int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
