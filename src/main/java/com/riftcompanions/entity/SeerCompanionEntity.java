package com.riftcompanions.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class SeerCompanionEntity extends CompanionEntity {
    public SeerCompanionEntity(final EntityType<SeerCompanionEntity> type, final Level level) {
        super(type, level, CompanionRole.SEER);
    }
}
