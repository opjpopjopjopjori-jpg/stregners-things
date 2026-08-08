package com.riftcompanions.hive.control;

import com.riftcompanions.config.CompanionConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side pressure model for Will's temporary hostile control.
 *
 * <p>Each target consumes an effective control-heart load. Maximum health is
 * the primary value; attack and armor add a small bounded pressure adjustment.
 * The visible HUD load is therefore a readable strength budget rather than a
 * hidden random chance. A target too large for the remaining budget can still
 * receive one short resisted Stagger window, never permanent ownership.</p>
 */
public final class WillControlBudget {
    private static final float LOAD_DURATION_PENALTY = 0.45F;
    private static final float MIN_NORMAL_DURATION_SCALE = 0.45F;
    private static final float ENERGY_LOAD_MULTIPLIER = 0.65F;
    private static final float STRAIN_LOAD_MULTIPLIER = 0.85F;
    private static final float RESISTANCE_COST_SURCHARGE = 0.10F;

    private WillControlBudget() {}

    public static ControlBudget budget(final boolean surge) {
        final int base = CompanionConfig.WILL_CONTROL_HEART_BUDGET.get();
        final int bonus = surge
                ? Math.round(base * CompanionConfig.WILL_CONTROL_SURGE_HEART_BONUS_PERCENT.get() / 100.0F)
                : 0;
        // Retain the prior swarm key as a compatibility ceiling while exposing
        // the clearer Will control cap for new configurations.
        final int configuredCap = Math.min(CompanionConfig.HIVE_SWARM_TARGET_CAP.get(),
                CompanionConfig.WILL_CONTROL_TARGET_CAP.get());
        return new ControlBudget(Math.max(1, base + bonus), Math.max(1, configuredCap));
    }

    public static Selection select(final List<? extends Mob> orderedCandidates,
                                   final HiveControlMode requestedMode,
                                   final boolean surge) {
        return select(orderedCandidates, requestedMode, budget(surge));
    }

    /**
     * Revalidates a channel against its captured budget. The caller supplies
     * candidates in distance order, so a crowded encounter has deterministic,
     * visible nearest-first selection instead of a random hidden target swap.
     */
    public static Selection select(final List<? extends Mob> orderedCandidates,
                                   final HiveControlMode requestedMode,
                                   final ControlBudget budget) {
        final List<ControlTarget> selected = new ArrayList<>();
        final ControlBudget effectiveBudget = new ControlBudget(budget.capacityHearts(),
                requestedMode == HiveControlMode.SWARM_FREEZE ? budget.targetLimit() : 1);
        final int targetLimit = effectiveBudget.targetLimit();
        int usedHearts = 0;
        int immuneCount = 0;

        if (orderedCandidates != null) {
            for (final Mob candidate : orderedCandidates) {
                if (!WillControlEligibility.isEligible(candidate)) continue;
                final TargetPressure pressure = pressureFor(candidate);
                if (pressure.resistance() == BossResistanceLevel.IMMUNE) {
                    immuneCount++;
                    continue;
                }
                if (selected.size() >= targetLimit) break;

                final int remaining = Math.max(0, effectiveBudget.capacityHearts() - usedHearts);
                final boolean overload = pressure.effectiveHeartLoad() > remaining;
                // A first high-strength target receives one short resisted
                // opening. Once a link is already allocated, it cannot crowd
                // out selected targets or make the channel unbounded.
                if (overload && !selected.isEmpty()) continue;

                final boolean resisted = overload || pressure.resistance() == BossResistanceLevel.PARTIAL;
                final HiveControlMode resolvedMode = resisted ? HiveControlMode.STAGGER : requestedMode;
                final int reservedHearts = overload ? effectiveBudget.capacityHearts() : pressure.effectiveHeartLoad();
                selected.add(new ControlTarget(candidate, pressure, resolvedMode, resisted, overload));
                usedHearts = Math.min(effectiveBudget.capacityHearts(), usedHearts + reservedHearts);
                if (overload || selected.size() >= targetLimit) break;
            }
        }

        final float loadRatio = effectiveBudget.capacityHearts() <= 0 ? 1.0F
                : Mth.clamp((float) usedHearts / (float) effectiveBudget.capacityHearts(), 0.0F, 1.0F);
        final float normalDurationScale = Math.max(MIN_NORMAL_DURATION_SCALE, 1.0F - LOAD_DURATION_PENALTY * loadRatio);
        return new Selection(List.copyOf(selected), effectiveBudget, usedHearts, immuneCount, normalDurationScale);
    }

    public static TargetPressure pressureFor(final Mob target) {
        final int healthHearts = Math.max(1, ceil(target.getMaxHealth() / 2.0D));
        final double attackDamage = Math.max(0.0D, target.getAttributeValue(Attributes.ATTACK_DAMAGE));
        final int attackLoad = Math.max(0, ceil((attackDamage - 3.0D) / 4.0D));
        final int armorLoad = Math.max(0, ceil(target.getArmorValue() / 5.0D));
        final int baseLoad = Math.max(1, healthHearts + attackLoad + armorLoad);
        final BossResistanceLevel resistance = BossInteractionRegistry.resistance(target);
        final int effectiveLoad = resistance == BossResistanceLevel.PARTIAL
                ? Math.max(baseLoad, ceil(baseLoad * 1.50D))
                : baseLoad;
        return new TargetPressure(healthHearts, attackLoad, armorLoad, effectiveLoad, resistance);
    }

    private static int ceil(final double value) {
        return (int) Math.ceil(Math.max(0.0D, value));
    }

    public record ControlBudget(int capacityHearts, int targetLimit) {
        public ControlBudget {
            capacityHearts = Math.max(1, capacityHearts);
            targetLimit = Math.max(1, targetLimit);
        }
    }

    public record TargetPressure(int healthHearts, int attackLoad, int armorLoad,
                                 int effectiveHeartLoad, BossResistanceLevel resistance) {
        public TargetPressure {
            healthHearts = Math.max(1, healthHearts);
            attackLoad = Math.max(0, attackLoad);
            armorLoad = Math.max(0, armorLoad);
            effectiveHeartLoad = Math.max(1, effectiveHeartLoad);
            resistance = resistance == null ? BossResistanceLevel.IMMUNE : resistance;
        }
    }

    public record ControlTarget(Mob target, TargetPressure pressure, HiveControlMode resolvedMode,
                                boolean resisted, boolean overload) {
        public ControlTarget {
            if (target == null || pressure == null || resolvedMode == null) {
                throw new IllegalArgumentException("Will control target data must be complete");
            }
        }
    }

    public record Selection(List<ControlTarget> targets, ControlBudget budget, int usedHearts,
                            int immuneCount, float normalDurationScale) {
        public Selection {
            targets = targets == null ? List.of() : List.copyOf(targets);
            budget = budget == null ? new ControlBudget(1, 1) : budget;
            usedHearts = Mth.clamp(usedHearts, 0, budget.capacityHearts());
            immuneCount = Math.max(0, immuneCount);
            normalDurationScale = Mth.clamp(normalDurationScale, 0.05F, 1.0F);
        }

        public boolean hasTargets() {
            return !targets.isEmpty();
        }

        public boolean hasResistedTarget() {
            return targets.stream().anyMatch(ControlTarget::resisted);
        }

        public float loadRatio() {
            return Mth.clamp((float) usedHearts / (float) budget.capacityHearts(), 0.0F, 1.0F);
        }

        public float durationScaleFor(final ControlTarget target) {
            if (target != null && target.resisted()) {
                final float resistedScale = CompanionConfig.WILL_CONTROL_OVERLOAD_DURATION_PERCENT.get() / 100.0F;
                return Math.min(normalDurationScale, Mth.clamp(resistedScale, 0.05F, 1.0F));
            }
            return normalDurationScale;
        }

        public float energyCost(final float baseCost) {
            return roundCost(baseCost * (1.0F + ENERGY_LOAD_MULTIPLIER * loadRatio()
                    + (hasResistedTarget() ? RESISTANCE_COST_SURCHARGE : 0.0F)));
        }

        public float strainCost(final float baseCost) {
            return roundCost(baseCost * (1.0F + STRAIN_LOAD_MULTIPLIER * loadRatio()
                    + (hasResistedTarget() ? RESISTANCE_COST_SURCHARGE : 0.0F)));
        }

        private static float roundCost(final float value) {
            return Math.max(0.0F, Math.round(value * 10.0F) / 10.0F);
        }
    }
}
