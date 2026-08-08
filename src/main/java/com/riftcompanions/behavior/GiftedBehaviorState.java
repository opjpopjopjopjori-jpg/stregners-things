package com.riftcompanions.behavior;

import net.minecraft.nbt.CompoundTag;

/**
 * Persisted, bounded Gifted readiness context. It stores only an explainable
 * readiness band and suggestion cadence; gameplay authority stays in the
 * server AbilityService safety gate.
 */
public final class GiftedBehaviorState {
    private GiftedReadinessBand readiness = GiftedReadinessBand.READY;
    private String reasonCode = "INITIALIZED";
    private String lastSuggestion = "";
    private long updatedAt;
    private long lastSuggestionAt;

    public GiftedReadinessBand readiness() { return readiness; }
    public String reasonCode() { return reasonCode; }
    public String lastSuggestion() { return lastSuggestion; }
    public long updatedAt() { return updatedAt; }
    public long lastSuggestionAt() { return lastSuggestionAt; }

    /** Returns true only when the user-visible readiness reason actually changes. */
    public boolean update(final GiftedReadinessBand next, final String nextReason, final long now) {
        final GiftedReadinessBand safeBand = next == null ? GiftedReadinessBand.CAUTION : next;
        final String safeReason = trim(nextReason, 96, "READINESS_UNKNOWN");
        final boolean changed = safeBand != readiness || !safeReason.equals(reasonCode);
        readiness = safeBand;
        reasonCode = safeReason;
        updatedAt = Math.max(0L, now);
        return changed;
    }

    public boolean canSuggest(final long now, final long cooldownTicks) {
        return now - lastSuggestionAt >= Math.max(20L, cooldownTicks);
    }

    public void markSuggestion(final String trigger, final long now) {
        lastSuggestion = trim(trigger, 64, "");
        lastSuggestionAt = Math.max(0L, now);
    }

    public String summary() {
        return readiness + " — " + reasonCode;
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Readiness", readiness.name());
        tag.putString("Reason", reasonCode);
        tag.putString("LastSuggestion", lastSuggestion);
        tag.putLong("UpdatedAt", updatedAt);
        tag.putLong("LastSuggestionAt", lastSuggestionAt);
        return tag;
    }

    public static GiftedBehaviorState load(final CompoundTag tag) {
        final GiftedBehaviorState state = new GiftedBehaviorState();
        if (tag == null) return state;
        try {
            state.readiness = GiftedReadinessBand.valueOf(tag.getString("Readiness"));
        } catch (final IllegalArgumentException ignored) {
            state.readiness = GiftedReadinessBand.CAUTION;
        }
        state.reasonCode = trim(tag.getString("Reason"), 96, "READINESS_UNKNOWN");
        state.lastSuggestion = trim(tag.getString("LastSuggestion"), 64, "");
        state.updatedAt = Math.max(0L, tag.getLong("UpdatedAt"));
        state.lastSuggestionAt = Math.max(0L, tag.getLong("LastSuggestionAt"));
        return state;
    }

    private static String trim(final String value, final int maximum, final String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }
}
