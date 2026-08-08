package com.riftcompanions.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class GiftedCompanionEntity extends CompanionEntity {
    public GiftedCompanionEntity(final EntityType<GiftedCompanionEntity> type, final Level level) {
        super(type, level, CompanionRole.GIFTED);
    }
}
