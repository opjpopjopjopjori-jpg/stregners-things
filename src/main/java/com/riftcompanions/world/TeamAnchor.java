package com.riftcompanions.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** A dimension-aware, player-authored anchor. It is a waypoint, not a placed block. */
public record TeamAnchor(ResourceLocation dimension, BlockPos position, long createdDay) {
    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Position", position.asLong());
        tag.putLong("CreatedDay", createdDay);
        return tag;
    }

    public static TeamAnchor load(final CompoundTag tag) {
        try {
            return new TeamAnchor(new ResourceLocation(tag.getString("Dimension")), BlockPos.of(tag.getLong("Position")), tag.getLong("CreatedDay"));
        } catch (final RuntimeException exception) {
            return null;
        }
    }
}
