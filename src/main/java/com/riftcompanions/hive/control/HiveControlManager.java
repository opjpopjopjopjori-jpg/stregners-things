package com.riftcompanions.hive.control;

import com.riftcompanions.config.CompanionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * Server-authoritative temporary hostile-control state. Persistent target data
 * records the mode, expiry, and only the flags this system changed, so reloads
 * and short resisted boss windows cannot leave a mob with altered AI/gravity.
 */
public final class HiveControlManager {
    private static final String ROOT = "riftcompanions_hive_control";
    private static final String MODE = "mode";
    private static final String EXPIRES = "expires";
    private static final String ORIGINAL_NO_AI = "original_no_ai";
    private static final String ORIGINAL_NO_GRAVITY = "original_no_gravity";
    private static final String CONTROLS_AI = "controls_ai";
    private static final String CONTROLS_GRAVITY = "controls_gravity";
    private static final String SOURCE_X = "source_x";
    private static final String SOURCE_Z = "source_z";
    private static final String LIFT_TOP_Y = "lift_top_y";

    private HiveControlManager() {}

    public static boolean apply(final Mob target, final HiveControlMode mode, final long duration, final Vec3 sourcePosition) {
        if (!(target.level() instanceof ServerLevel level) || !level.hasChunkAt(target.blockPosition())) return false;
        final CompoundTag root = target.getPersistentData();
        if (root.contains(ROOT)) release(target, false);

        final boolean controlsAi = mode != HiveControlMode.STAGGER;
        final boolean controlsGravity = mode == HiveControlMode.SUSPEND || mode == HiveControlMode.SWARM_FREEZE;
        final CompoundTag state = new CompoundTag();
        state.putString(MODE, mode.name());
        state.putLong(EXPIRES, level.getGameTime() + Math.max(1L, duration));
        state.putBoolean(ORIGINAL_NO_AI, target.isNoAi());
        state.putBoolean(ORIGINAL_NO_GRAVITY, target.isNoGravity());
        state.putBoolean(CONTROLS_AI, controlsAi);
        state.putBoolean(CONTROLS_GRAVITY, controlsGravity);
        state.putDouble(SOURCE_X, sourcePosition.x);
        state.putDouble(SOURCE_Z, sourcePosition.z);
        if (controlsGravity) {
            state.putDouble(LIFT_TOP_Y, target.getY() + (mode == HiveControlMode.SUSPEND ? 1.15D : 0.45D));
        }
        root.put(ROOT, state);

        if (controlsAi) {
            target.setNoAi(true);
            target.setDeltaMovement(Vec3.ZERO);
            target.fallDistance = 0.0F;
            target.setNoGravity(controlsGravity);
        }
        if (mode == HiveControlMode.SHATTER || mode == HiveControlMode.STAGGER) {
            // Non-graphic structural shatter / resisted stagger: a temporary
            // debuff only, never gore, loot mutation, or an execution path.
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, (int) duration, 1, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int) duration, 1, false, true, true));
        }
        emit(level, target, mode, true);
        return true;
    }

    public static void tick(final LivingEntity entity) {
        if (!(entity instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        final CompoundTag root = mob.getPersistentData();
        if (!root.contains(ROOT, net.minecraft.nbt.Tag.TAG_COMPOUND)) return;
        final CompoundTag state = root.getCompound(ROOT);
        final HiveControlMode mode;
        try {
            mode = HiveControlMode.valueOf(state.getString(MODE));
        } catch (IllegalArgumentException exception) {
            release(mob, false);
            return;
        }
        final long now = level.getGameTime();
        if (now >= state.getLong(EXPIRES) || !mob.isAlive()) {
            release(mob, true);
            return;
        }
        switch (mode) {
            case SUSPEND, SWARM_FREEZE -> {
                // Lift through already loaded collision-safe space only. This
                // control state never teleports, passes blocks, or loads chunks.
                liftSafely(level, mob, state);
                mob.setDeltaMovement(Vec3.ZERO);
                mob.setNoAi(true);
                mob.setNoGravity(true);
                mob.fallDistance = 0.0F;
            }
            case SHATTER -> {
                mob.setDeltaMovement(Vec3.ZERO);
                mob.setNoAi(true);
                mob.fallDistance = 0.0F;
            }
            case REDIRECT -> {
                mob.setNoAi(true);
                redirectSafely(level, mob, state);
            }
            case STAGGER -> {
                // Partial resistance never disables boss AI or gravity. The
                // short effect is supplied by the debuffs applied at release.
                mob.fallDistance = 0.0F;
            }
        }
        if (now % 10L == 0L) emit(level, mob, mode, false);
    }

    /** Raises a controlled target gradually without teleporting or loading terrain. */
    private static void liftSafely(final ServerLevel level, final Mob target, final CompoundTag state) {
        if (!state.contains(LIFT_TOP_Y)) return;
        final double remaining = state.getDouble(LIFT_TOP_Y) - target.getY();
        if (remaining <= 0.02D) return;
        final Vec3 step = new Vec3(0.0D, Math.min(0.08D, remaining), 0.0D);
        final BlockPos next = BlockPos.containing(target.position().add(step));
        if (!level.hasChunkAt(next) || !level.noCollision(target, target.getBoundingBox().move(step))) return;
        target.move(MoverType.SELF, step);
    }

    private static void redirectSafely(final ServerLevel level, final Mob target, final CompoundTag state) {
        Vec3 away = target.position().subtract(new Vec3(state.getDouble(SOURCE_X), target.getY(), state.getDouble(SOURCE_Z)));
        if (away.lengthSqr() < 0.001D) away = new Vec3(1.0D, 0.0D, 0.0D);
        away = away.normalize().scale(0.10D);
        final Vec3 next = target.position().add(away);
        final BlockPos nextPos = BlockPos.containing(next);
        if (!level.hasChunkAt(nextPos) || !level.getFluidState(nextPos).isEmpty()) return;
        if (!level.noCollision(target, target.getBoundingBox().move(away))) return;
        target.move(MoverType.SELF, away);
        target.fallDistance = 0.0F;
    }

    public static void release(final Mob target, final boolean applyAfterEffect) {
        final CompoundTag root = target.getPersistentData();
        if (!root.contains(ROOT, net.minecraft.nbt.Tag.TAG_COMPOUND)) return;
        final CompoundTag state = root.getCompound(ROOT);
        final boolean controlledAi = state.getBoolean(CONTROLS_AI);
        final boolean controlledGravity = state.getBoolean(CONTROLS_GRAVITY);
        if (controlledAi) target.setNoAi(state.getBoolean(ORIGINAL_NO_AI));
        if (controlledGravity || controlledAi) target.setNoGravity(state.getBoolean(ORIGINAL_NO_GRAVITY));
        target.fallDistance = 0.0F;
        if (applyAfterEffect && controlledAi && target.level() instanceof ServerLevel level) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20, 0, false, true, true));
            emit(level, target, HiveControlMode.STAGGER, true);
        }
        root.remove(ROOT);
    }

    public static boolean isControlled(final LivingEntity entity) {
        return entity.getPersistentData().contains(ROOT, net.minecraft.nbt.Tag.TAG_COMPOUND);
    }

    public static HiveControlMode mode(final LivingEntity entity) {
        if (!isControlled(entity)) return null;
        try {
            return HiveControlMode.valueOf(entity.getPersistentData().getCompound(ROOT).getString(MODE));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public static long remainingTicks(final LivingEntity entity) {
        if (!isControlled(entity)) return 0L;
        return Math.max(0L, entity.getPersistentData().getCompound(ROOT).getLong(EXPIRES) - entity.level().getGameTime());
    }

    private static void emit(final ServerLevel level, final Mob target, final HiveControlMode mode, final boolean burst) {
        final int base = burst ? 12 : 4;
        final int count = CompanionConfig.LOW_EFFECTS.get() ? Math.max(1, base / 4) : base;
        final ParticleOptions particle = switch (mode) {
            case SUSPEND, SWARM_FREEZE -> ParticleTypes.END_ROD;
            case REDIRECT -> ParticleTypes.REVERSE_PORTAL;
            case SHATTER, STAGGER -> ParticleTypes.ELECTRIC_SPARK;
        };
        level.sendParticles(particle, target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ(),
                count, 0.25D, 0.35D, 0.25D, 0.01D);
    }
}
