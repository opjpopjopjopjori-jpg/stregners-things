package com.riftcompanions.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Compact, persistent player annotation. */
public record WorldAnnotation(WorldAnnotationType type, ResourceLocation dimension, BlockPos position, long createdDay) {
    public WorldAnnotation {
        position = position.immutable();
    }

    public CompoundTag save() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Type", type.name());
        tag.putString("Dimension", dimension.toString());
        tag.putLong("Position", position.asLong());
        tag.putLong("Day", createdDay);
        return tag;
    }

    public static WorldAnnotation load(final CompoundTag tag) {
        try {
            return new WorldAnnotation(WorldAnnotationType.valueOf(tag.getString("Type")), new ResourceLocation(tag.getString("Dimension")),
                    BlockPos.of(tag.getLong("Position")), tag.getLong("Day"));
        } catch (final RuntimeException exception) {
            return null;
        }
    }
}
