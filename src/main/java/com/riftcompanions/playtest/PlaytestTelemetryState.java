package com.riftcompanions.playtest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Map;

/** Bounded aggregate ratings; no upload, analytics SDK, or online service exists. */
public final class PlaytestTelemetryState {
    private final EnumMap<AcceptanceAxis, Aggregate> aggregates = new EnumMap<>(AcceptanceAxis.class);

    public void record(final AcceptanceAxis axis, final int score, final long day) {
        if (axis == null || score < 1 || score > 5) return;
        aggregates.put(axis, aggregates.getOrDefault(axis, new Aggregate(0, 0, 0L)).add(score, day));
    }

    public double average(final AcceptanceAxis axis) {
        final Aggregate aggregate = aggregates.get(axis);
        return aggregate == null || aggregate.count == 0 ? 0.0D : (double) aggregate.total / aggregate.count;
    }

    public int count(final AcceptanceAxis axis) {
        return aggregates.getOrDefault(axis, new Aggregate(0, 0, 0L)).count;
    }

    public boolean goRulePasses() {
        for (final AcceptanceAxis axis : AcceptanceAxis.values()) {
            if (count(axis) > 0 && average(axis) < 3.0D) return false;
        }
        return true;
    }

    public Map<AcceptanceAxis, Aggregate> snapshot() { return Map.copyOf(aggregates); }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        final ListTag entries = new ListTag();
        aggregates.forEach((axis, aggregate) -> {
            final CompoundTag entry = new CompoundTag();
            entry.putString("Axis", axis.name());
            entry.putInt("Total", aggregate.total);
            entry.putInt("Count", aggregate.count);
            entry.putLong("LastDay", aggregate.lastDay);
            entries.add(entry);
        });
        tag.put("Ratings", entries);
        return tag;
    }

    public static PlaytestTelemetryState load(final CompoundTag tag) {
        final PlaytestTelemetryState state = new PlaytestTelemetryState();
        if (tag == null) return state;
        final ListTag entries = tag.getList("Ratings", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            final CompoundTag entry = entries.getCompound(i);
            try {
                final AcceptanceAxis axis = AcceptanceAxis.valueOf(entry.getString("Axis"));
                state.aggregates.put(axis, new Aggregate(Math.max(0, entry.getInt("Total")), Math.max(0, entry.getInt("Count")), Math.max(0L, entry.getLong("LastDay"))));
            } catch (final IllegalArgumentException ignored) { }
        }
        return state;
    }

    public record Aggregate(int total, int count, long lastDay) {
        private Aggregate add(final int score, final long day) {
            return new Aggregate(Math.min(10000, total + score), Math.min(2000, count + 1), Math.max(lastDay, day));
        }
    }
}
