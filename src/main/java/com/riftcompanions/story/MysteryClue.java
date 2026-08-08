package com.riftcompanions.story;

import net.minecraft.nbt.CompoundTag;

/** Small player-visible clue record based on an actual annotation or observation. */
public record MysteryClue(String id, String summary, MysteryClueConfidence confidence, long day, boolean deferred) {
    public MysteryClue {
        id = clip(id, 80);
        summary = clip(summary, 180);
        confidence = confidence == null ? MysteryClueConfidence.SUSPECTED : confidence;
        day = Math.max(0L, day);
    }

    public MysteryClue defer() { return new MysteryClue(id, summary, confidence, day, true); }
    public MysteryClue confirm(MysteryClueConfidence next) { return new MysteryClue(id, summary, next, day, deferred); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id);
        tag.putString("Summary", summary);
        tag.putString("Confidence", confidence.name());
        tag.putLong("Day", day);
        tag.putBoolean("Deferred", deferred);
        return tag;
    }

    public static MysteryClue load(CompoundTag tag) {
        try {
            return new MysteryClue(tag.getString("Id"), tag.getString("Summary"),
                    MysteryClueConfidence.valueOf(tag.getString("Confidence")), tag.getLong("Day"), tag.getBoolean("Deferred"));
        } catch (IllegalArgumentException ignored) {
            return new MysteryClue("invalid", "Invalid clue ignored.", MysteryClueConfidence.REJECTED, 0L, false);
        }
    }

    private static String clip(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
