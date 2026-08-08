package com.riftcompanions.hive.control;

import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.registry.ModTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;

/**
 * Authoritative target boundary for Will's temporary hostile-control powers.
 *
 * <p>Eligibility is intentionally type-based rather than tied to a custom
 * Hive tag: any loaded hostile {@link Enemy} that is also a {@link Mob} can be
 * considered. Explicitly protected entities, companions, villagers, animals,
 * and tameable entities remain outside the system. This is not a pet or
 * ownership path; all effects remain temporary server control states.</p>
 */
public final class WillControlEligibility {
    private WillControlEligibility() {}

    public static boolean isEligible(final Mob target) {
        if (target == null || !target.isAlive()) return false;
        if (!(target instanceof Enemy)) return false;
        if (target instanceof CompanionEntity || target instanceof Villager
                || target instanceof TamableAnimal || target instanceof Animal) return false;
        return !target.getType().is(ModTags.PROTECTED_FROM_COMPANIONS);
    }

    /**
     * Will can control any eligible hostile monster — even without active combat.
     * This allows normal-use hive control on nearby threats without requiring
     * them to be currently attacking the player.
     */
    public static boolean isNearbyThreat(final ServerPlayer owner, final Mob target) {
        if (owner == null || !isEligible(target)) return false;
        return true;
    }

    /**
     * Danger context: the owner is under active attack, low health, or mobs are
     * very close. In this state Will's control is 2x stronger (double duration)
     * and can subdue even boss-tier mobs for extended windows (>10 seconds).
     */
    public static boolean isDangerContext(final ServerPlayer owner, final Mob target) {
        if (owner == null || !isEligible(target)) return false;
        final LivingEntity currentTarget = target.getTarget();
        if (currentTarget == owner) return true;
        if (currentTarget instanceof CompanionEntity companion && companion.isOwnedBy(owner)) return true;
        if (target.distanceToSqr(owner) <= 10.0D * 10.0D) return true;
        return owner.getHealth() <= owner.getMaxHealth() * 0.50F;
    }

    /**
     * Legacy combat-context check retained for AbilityService gating.
     * The target must actively threaten the owner or an owned companion, with a
     * narrow low-health close-threat fallback for a last-second defensive cast.
     */
    public static boolean isActiveThreat(final ServerPlayer owner, final Mob target) {
        if (owner == null || !isEligible(target)) return false;
        final LivingEntity currentTarget = target.getTarget();
        if (currentTarget == owner) return true;
        if (currentTarget instanceof CompanionEntity companion && companion.isOwnedBy(owner)) return true;
        if (BossInteractionRegistry.resistance(target) != BossResistanceLevel.VULNERABLE_WINDOW
                && target.distanceToSqr(owner) <= 12.0D * 12.0D) return true;
        return owner.getHealth() <= owner.getMaxHealth() * 0.45F && target.distanceToSqr(owner) <= 8.0D * 8.0D;
    }
}
