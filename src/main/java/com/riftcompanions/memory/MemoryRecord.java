package com.riftcompanions.memory;

import net.minecraft.nbt.CompoundTag;

/** Small serializable journal summary. It avoids unbounded raw event history. */
public record MemoryRecord(MemoryType type, long day, String summary, int confidence) {
    public MemoryRecord {
        summary = summary == null ? "" : summary.substring(0, Math.min(180, summary.length()));
        confidence = Math.max(0, Math.min(100, confidence));
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name());
        tag.putLong("Day", day);
        tag.putString("Summary", summary);
        tag.putInt("Confidence", confidence);
        return tag;
    }

    public static MemoryRecord load(final CompoundTag tag) {
        try {
            return new MemoryRecord(MemoryType.valueOf(tag.getString("Type")), tag.getLong("Day"), tag.getString("Summary"), tag.getInt("Confidence"));
        } catch (final IllegalArgumentException exception) {
            return new MemoryRecord(MemoryType.MILESTONE, 0L, "Recovered legacy memory", 0);
        }
    }
}
