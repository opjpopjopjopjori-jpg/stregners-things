package com.riftcompanions.entity.ai;

import com.riftcompanions.entity.CompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Small defensive melee goal. It never starts when a companion is holding,
 * retreating, exhausted, downed, or configured to avoid combat. It also has a
 * strict pursuit radius so companions cannot chase a monster through chunks.
 */
public final class CompanionMeleeGoal extends Goal {
    private final CompanionEntity companion;
    private final double speed;
    private int attackDelay;

    public CompanionMeleeGoal(final CompanionEntity companion, final double speed) {
        this.companion = companion;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        final LivingEntity target = companion.getTarget();
        final double pursuit = Math.max(32.0D, com.riftcompanions.config.CompanionConfig.COMBAT_PROFILE.get().pursuitRadius());
        return companion.allowsCombatAction()
                && target != null
                && target.isAlive()
                && companion.distanceToSqr(target) <= pursuit * pursuit;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        companion.getNavigation().stop();
        attackDelay = 0;
    }

    @Override
    public void tick() {
        final LivingEntity target = companion.getTarget();
        if (target == null) {
            return;
        }
        companion.orientCombatBodyToward(target);
        companion.clearFormationSlot("MELEE_TARGET_ENGAGED");
        companion.setCompanionState(com.riftcompanions.entity.CompanionState.FIGHTING, "MELEE_TARGET_ENGAGED");
        final double range = companion.getBbWidth() * 2.0D + target.getBbWidth();
        if (companion.distanceToSqr(target) > range * range) {
            companion.getNavigation().moveTo(target, Math.max(1.30D, speed));
            return;
        }
        companion.getNavigation().stop();
        if (attackDelay > 0) {
            attackDelay--;
            return;
        }
        attackDelay = 12;
        if (companion.doHurtTarget(target)) {
            companion.beginVisualAction(com.riftcompanions.entity.CompanionAction.MELEE_ATTACK, 14L);
        }
    }
}
