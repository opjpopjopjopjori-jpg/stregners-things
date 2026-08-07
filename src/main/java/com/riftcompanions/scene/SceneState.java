package com.riftcompanions.scene;

import net.minecraft.nbt.CompoundTag;

/** One bounded pending micro-scene plus a silence window; no cutscene timeline. */
public final class SceneState {
    private SetPieceType pending = SetPieceType.NONE;
    private long requestedAt;
    private long expiresAt;
    private long silentUntil;

    public SetPieceType pending() { return pending; }
    public long silentUntil() { return silentUntil; }
    public boolean isSilent(final long now) { return now < silentUntil; }

    public boolean request(final SetPieceType type, final long now, final long lifetime) {
        if (type == null || type == SetPieceType.NONE || (pending != SetPieceType.NONE && now < expiresAt)) return false;
        pending = type;
        requestedAt = now;
        expiresAt = now + Math.max(40L, lifetime);
        return true;
    }

    public void beginSilentWalk(final long now, final long duration) {
        silentUntil = Math.max(silentUntil, now + Math.max(0L, duration));
        request(SetPieceType.SILENT_WALK, now, Math.max(40L, duration));
    }

    public boolean consume(final long now) {
        if (pending == SetPieceType.NONE) return false;
        pending = SetPieceType.NONE;
        requestedAt = 0L;
        expiresAt = 0L;
        return true;
    }

    public void expire(final long now) {
        if (pending != SetPieceType.NONE && now >= expiresAt) consume(now);
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Pending", pending.name());
        tag.putLong("RequestedAt", requestedAt);
        tag.putLong("ExpiresAt", expiresAt);
        tag.putLong("SilentUntil", silentUntil);
        return tag;
    }

    public static SceneState load(final CompoundTag tag) {
        final SceneState state = new SceneState();
        if (tag == null) return state;
        try { state.pending = SetPieceType.valueOf(tag.getString("Pending")); }
        catch (final IllegalArgumentException ignored) { state.pending = SetPieceType.NONE; }
        state.requestedAt = Math.max(0L, tag.getLong("RequestedAt"));
        state.expiresAt = Math.max(0L, tag.getLong("ExpiresAt"));
        state.silentUntil = Math.max(0L, tag.getLong("SilentUntil"));
        return state;
    }
}
