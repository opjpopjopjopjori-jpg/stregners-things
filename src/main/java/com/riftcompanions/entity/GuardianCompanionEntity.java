package com.riftcompanions.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class GuardianCompanionEntity extends CompanionEntity {
    public GuardianCompanionEntity(final EntityType<GuardianCompanionEntity> type, final Level level) {
        super(type, level, CompanionRole.GUARDIAN);
    }
}
