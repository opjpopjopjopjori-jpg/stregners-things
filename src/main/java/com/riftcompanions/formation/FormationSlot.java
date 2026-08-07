package com.riftcompanions.formation;

import com.riftcompanions.entity.CompanionRole;
import net.minecraft.world.phys.Vec3;

/** Role-relative local offset expressed in player forward/right space. */
public record FormationSlot(CompanionRole role, double forward, double right, double vertical) {
    public Vec3 resolve(final Vec3 playerPosition, final Vec3 forwardVector, final Vec3 rightVector) {
        return playerPosition.add(forwardVector.scale(forward)).add(rightVector.scale(right)).add(0.0D, vertical, 0.0D);
    }
}
