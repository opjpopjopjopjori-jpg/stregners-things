package com.riftcompanions.consequence;

import net.minecraft.nbt.CompoundTag;

/** Bounded record of a player-visible decision effect. */
public record ConsequenceRecord(ConsequenceType type, long day, String key, String summary, int confidence) {
    public ConsequenceRecord {
        type = type == null ? ConsequenceType.MEMORY : type;
        day = Math.max(0L, day);
        key = clip(key, 64);
        summary = clip(summary, 180);
        confidence = Math.max(0, Math.min(100, confidence));
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name());
        tag.putLong("Day", day);
        tag.putString("Key", key);
        tag.putString("Summary", summary);
        tag.putInt("Confidence", confidence);
        return tag;
    }

    public static ConsequenceRecord load(final CompoundTag tag) {
        try {
            return new ConsequenceRecord(ConsequenceType.valueOf(tag.getString("Type")), tag.getLong("Day"),
                    tag.getString("Key"), tag.getString("Summary"), tag.getInt("Confidence"));
        } catch (final IllegalArgumentException ignored) {
            return new ConsequenceRecord(ConsequenceType.MEMORY, 0L, "invalid", "Invalid consequence record ignored.", 0);
        }
    }

    private static String clip(final String value, final int maximum) {
        if (value == null) return "";
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
