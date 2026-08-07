package com.riftcompanions.resource;

import net.minecraft.world.entity.item.ItemEntity;

/** Conservative ownership resolver. A world item with an explicit owner is never auto-taken. */
public final class ResourceOwnershipResolver {
    private ResourceOwnershipResolver() {}

    public static ResourceOwnership worldItemOwnership(final ItemEntity item) {
        if (item == null || item.getItem().isEmpty()) return ResourceOwnership.PROTECTED;
        if (item.getOwner() != null) return ResourceOwnership.PLAYER_PRIVATE;
        return ResourceOwnership.WORLD_FREE;
    }
}
