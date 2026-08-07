package com.riftcompanions.team;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Player-visible, already-loaded plan focus; never a structure locator or forced chunk load. */
public record PlanTarget(ResourceLocation dimension, BlockPos position, String kind) {
    public PlanTarget {
        if (dimension == null) throw new IllegalArgumentException("dimension");
        if (position == null) throw new IllegalArgumentException("position");
        position = position.immutable();
        kind = kind == null || kind.isBlank() ? "GENERIC" : kind.length() <= 48 ? kind : kind.substring(0, 48);
    }

    public boolean isValidFor(final ServerPlayer player) {
        return player != null && player.level().dimension().location().equals(dimension)
                && player.serverLevel().hasChunkAt(position);
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Position", position.asLong());
        tag.putString("Kind", kind);
        return tag;
    }

    public static Optional<PlanTarget> load(final CompoundTag tag) {
        if (tag == null || !tag.contains("Dimension") || !tag.contains("Position")) return Optional.empty();
        try {
            return Optional.of(new PlanTarget(new ResourceLocation(tag.getString("Dimension")), BlockPos.of(tag.getLong("Position")), tag.getString("Kind")));
        } catch (final RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
