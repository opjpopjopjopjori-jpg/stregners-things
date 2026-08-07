package com.riftcompanions.safety;

import net.minecraft.nbt.CompoundTag;

/** Persisted per-owner fail-safe state. It is intentionally small and readable. */
public final class SafeModeState {
    private boolean enabled;
    private SafeModeReason reason = SafeModeReason.MANUAL;
    private String detail = "";
    private long enabledAt;
    private int activations;

    public boolean enabled() { return enabled; }
    public SafeModeReason reason() { return reason; }
    public String detail() { return detail; }
    public long enabledAt() { return enabledAt; }
    public int activations() { return activations; }

    public void enable(final SafeModeReason newReason, final String newDetail, final long now) {
        enabled = true;
        reason = newReason == null ? SafeModeReason.PLAYER_REVIEW_REQUIRED : newReason;
        detail = clip(newDetail);
        enabledAt = Math.max(0L, now);
        activations = Math.min(9999, activations + 1);
    }

    public void clear() {
        enabled = false;
        detail = "";
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putBoolean("Enabled", enabled);
        tag.putString("Reason", reason.name());
        tag.putString("Detail", detail);
        tag.putLong("EnabledAt", enabledAt);
        tag.putInt("Activations", activations);
        return tag;
    }

    public static SafeModeState load(final CompoundTag tag) {
        final SafeModeState result = new SafeModeState();
        if (tag == null) return result;
        result.enabled = tag.getBoolean("Enabled");
        try { result.reason = SafeModeReason.valueOf(tag.getString("Reason")); }
        catch (final IllegalArgumentException ignored) { result.reason = SafeModeReason.PLAYER_REVIEW_REQUIRED; }
        result.detail = clip(tag.getString("Detail"));
        result.enabledAt = Math.max(0L, tag.getLong("EnabledAt"));
        result.activations = Math.max(0, Math.min(9999, tag.getInt("Activations")));
        return result;
    }

    private static String clip(final String value) {
        if (value == null) return "";
        return value.length() <= 160 ? value : value.substring(0, 160);
    }
}
