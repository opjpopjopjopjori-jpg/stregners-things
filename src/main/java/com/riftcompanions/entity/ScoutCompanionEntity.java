package com.riftcompanions.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class ScoutCompanionEntity extends CompanionEntity {
    public ScoutCompanionEntity(final EntityType<ScoutCompanionEntity> type, final Level level) {
        super(type, level, CompanionRole.SCOUT);
    }
}
