package com.riftcompanions.mental;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

/** Temporary safe anchor data; never places blocks or grants invulnerability. */
public final class MindAnchorState {
    private ResourceLocation dimension;
    private BlockPos position;
    private long expiresAt;
    private long cooldownUntil;

    public boolean activeAt(final ResourceLocation currentDimension, final long now) {
        return dimension != null && position != null && dimension.equals(currentDimension) && now < expiresAt;
    }

    public Optional<BlockPos> position() { return Optional.ofNullable(position == null ? null : position.immutable()); }
    public long cooldownRemaining(final long now) { return Math.max(0L, cooldownUntil - now); }

    public void activate(final ResourceLocation newDimension, final BlockPos newPosition, final long now, final long duration, final long cooldown) {
        dimension = newDimension;
        position = newPosition == null ? null : newPosition.immutable();
        expiresAt = now + Math.max(20L, duration);
        cooldownUntil = now + Math.max(20L, cooldown);
    }

    public void clear() {
        dimension = null;
        position = null;
        expiresAt = 0L;
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        if (dimension != null) tag.putString("Dimension", dimension.toString());
        if (position != null) tag.putLong("Position", position.asLong());
        tag.putLong("ExpiresAt", expiresAt);
        tag.putLong("CooldownUntil", cooldownUntil);
        return tag;
    }

    public static MindAnchorState load(final CompoundTag tag) {
        final MindAnchorState state = new MindAnchorState();
        if (tag == null) return state;
        try {
            if (tag.contains("Dimension") && tag.contains("Position")) {
                state.dimension = new ResourceLocation(tag.getString("Dimension"));
                state.position = BlockPos.of(tag.getLong("Position"));
            }
        } catch (final RuntimeException ignored) { state.clear(); }
        state.expiresAt = Math.max(0L, tag.getLong("ExpiresAt"));
        state.cooldownUntil = Math.max(0L, tag.getLong("CooldownUntil"));
        return state;
    }
}
