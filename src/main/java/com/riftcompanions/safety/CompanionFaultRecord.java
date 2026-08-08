package com.riftcompanions.safety;

import net.minecraft.nbt.CompoundTag;

/** Bounded per-role circuit-breaker evidence; detailed stack traces stay in the log. */
public final class CompanionFaultRecord {
    private final long firstAt;
    private final long lastAt;
    private final int count;
    private final String lastCode;

    private CompanionFaultRecord(final long firstAt, final long lastAt, final int count, final String lastCode) {
        this.firstAt = Math.max(0L, firstAt);
        this.lastAt = Math.max(this.firstAt, lastAt);
        this.count = Math.max(0, Math.min(999, count));
        this.lastCode = clip(lastCode);
    }

    public static CompanionFaultRecord first(final long now, final String code) {
        return new CompanionFaultRecord(now, now, 1, code);
    }

    public CompanionFaultRecord observe(final long now, final String code, final long windowTicks) {
        if (now - lastAt > Math.max(20L, windowTicks)) return first(now, code);
        return new CompanionFaultRecord(firstAt, now, Math.min(999, count + 1), code);
    }

    public long firstAt() { return firstAt; }
    public long lastAt() { return lastAt; }
    public int count() { return count; }
    public String lastCode() { return lastCode; }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putLong("FirstAt", firstAt);
        tag.putLong("LastAt", lastAt);
        tag.putInt("Count", count);
        tag.putString("LastCode", lastCode);
        return tag;
    }

    public static CompanionFaultRecord load(final CompoundTag tag) {
        if (tag == null) return first(0L, "UNKNOWN");
        return new CompanionFaultRecord(tag.getLong("FirstAt"), tag.getLong("LastAt"), tag.getInt("Count"), tag.getString("LastCode"));
    }

    @Override
    public String toString() {
        return "count=" + count + ",last=" + lastCode + ",at=" + lastAt;
    }

    private static String clip(final String value) {
        if (value == null || value.isBlank()) return "UNKNOWN";
        return value.length() <= 96 ? value : value.substring(0, 96);
    }
}
