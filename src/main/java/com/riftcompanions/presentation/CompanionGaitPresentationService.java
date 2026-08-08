package com.riftcompanions.presentation;

import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionLookIntent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps body heading aligned to actual travel and emits a finite head-only look
 * gesture only when an already-selected target is newly meaningful: it appears,
 * moves into a lateral/rear sector, or changes sector. No idle/random head scan
 * occurs while following a normal path.
 */
public final class CompanionGaitPresentationService {
    private static final long GLANCE_TICKS = 20L;
    private static final long GLANCE_COOLDOWN_TICKS = 70L;
    private static final long BACK_CHECK_TICKS = 30L;
    private static final long BACK_CHECK_COOLDOWN_TICKS = 120L;
    private static final double MOVING_SPEED_SQUARED = 0.0016D;
    private static final Map<UUID, GaitState> STATES = new HashMap<>();

    private CompanionGaitPresentationService() {}

    public static void tick(final CompanionEntity companion, final ServerPlayer owner, final long now) {
        if (companion == null || owner == null || companion.level().isClientSide) return;
        final GaitState state = STATES.computeIfAbsent(companion.getUUID(), ignored -> new GaitState(owner.getUUID()));
        state.owner = owner.getUUID();

        final Vec3 velocity = companion.getDeltaMovement();
        if (velocity.horizontalDistanceSqr() >= MOVING_SPEED_SQUARED) {
            stabilizeBodyToTravel(companion, velocity);
        }

        // Finish a gesture once, then return to forward. Never pin the head to
        // a side target and never repeat a scan while the target remains there.
        if (now < state.activeUntil) {
            companion.setLookIntent(state.activeIntent);
            return;
        }
        state.activeIntent = CompanionLookIntent.FORWARD;

        final LivingEntity focus = companion.getTarget();
        if (focus == null || !focus.isAlive() || focus.level() != companion.level()) {
            state.clearFocus();
            companion.setLookIntent(CompanionLookIntent.FORWARD);
            return;
        }

        final Sector sector = sectorFor(companion, focus);
        if (!focus.getUUID().equals(state.focusId)) {
            state.focusId = focus.getUUID();
            state.lastSector = Sector.FORWARD;
            state.nextGestureAt = 0L;
        }

        if (sector == Sector.FORWARD) {
            state.lastSector = Sector.FORWARD;
            companion.setLookIntent(CompanionLookIntent.FORWARD);
            return;
        }

        // A direction change is the meaningful event. A persistent target in
        // the same sector causes no repeated glance every few seconds.
        if (sector != state.lastSector && now >= state.nextGestureAt) {
            final boolean rear = sector == Sector.BACK_LEFT || sector == Sector.BACK_RIGHT;
            final CompanionLookIntent intent = switch (sector) {
                case LEFT -> CompanionLookIntent.GLANCE_LEFT;
                case RIGHT -> CompanionLookIntent.GLANCE_RIGHT;
                case BACK_LEFT -> CompanionLookIntent.CHECK_BACK_LEFT;
                case BACK_RIGHT -> CompanionLookIntent.CHECK_BACK_RIGHT;
                case FORWARD -> CompanionLookIntent.FORWARD;
            };
            beginGesture(companion, state, intent, now,
                    rear ? BACK_CHECK_TICKS : GLANCE_TICKS,
                    rear ? BACK_CHECK_COOLDOWN_TICKS : GLANCE_COOLDOWN_TICKS);
        } else {
            companion.setLookIntent(CompanionLookIntent.FORWARD);
        }
        state.lastSector = sector;
    }

    public static void clearEntity(final UUID companion) {
        if (companion != null) STATES.remove(companion);
    }

    public static void clearOwner(final UUID owner) {
        if (owner == null) return;
        final Iterator<Map.Entry<UUID, GaitState>> iterator = STATES.entrySet().iterator();
        while (iterator.hasNext()) {
            if (owner.equals(iterator.next().getValue().owner)) iterator.remove();
        }
    }

    private static Sector sectorFor(final CompanionEntity companion, final LivingEntity focus) {
        final Vec3 toward = focus.position().subtract(companion.position());
        if (toward.horizontalDistanceSqr() < 0.04D) return Sector.FORWARD;
        final float difference = Mth.wrapDegrees(yawFor(toward) - companion.getYRot());
        final float absolute = Math.abs(difference);
        if (absolute < 18.0F) return Sector.FORWARD;
        if (absolute < 112.0F) return difference < 0.0F ? Sector.LEFT : Sector.RIGHT;
        return difference < 0.0F ? Sector.BACK_LEFT : Sector.BACK_RIGHT;
    }

    private static void beginGesture(final CompanionEntity companion, final GaitState state,
                                     final CompanionLookIntent intent, final long now,
                                     final long duration, final long cooldown) {
        state.activeIntent = intent;
        state.activeUntil = now + duration;
        state.nextGestureAt = now + cooldown;
        companion.setLookIntent(intent);
    }

    private static void stabilizeBodyToTravel(final CompanionEntity companion, final Vec3 velocity) {
        final float desired = yawFor(velocity);
        final float current = companion.getYRot();
        final float delta = Mth.wrapDegrees(desired - current);
        final float stabilized = current + Mth.clamp(delta, -18.0F, 18.0F);
        companion.setYRot(stabilized);
        companion.yBodyRot = stabilized;
        companion.yBodyRotO = stabilized;
        companion.setYHeadRot(stabilized);
    }

    private static float yawFor(final Vec3 vector) {
        return (float) (Math.atan2(vector.z, vector.x) * (180.0D / Math.PI)) - 90.0F;
    }

    private enum Sector { FORWARD, LEFT, RIGHT, BACK_LEFT, BACK_RIGHT }

    private static final class GaitState {
        private UUID owner;
        private UUID focusId;
        private long activeUntil;
        private long nextGestureAt;
        private CompanionLookIntent activeIntent = CompanionLookIntent.FORWARD;
        private Sector lastSector = Sector.FORWARD;

        private GaitState(final UUID owner) {
            this.owner = owner;
        }

        private void clearFocus() {
            focusId = null;
            activeUntil = 0L;
            nextGestureAt = 0L;
            activeIntent = CompanionLookIntent.FORWARD;
            lastSector = Sector.FORWARD;
        }
    }
}
