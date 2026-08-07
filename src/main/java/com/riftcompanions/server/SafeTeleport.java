package com.riftcompanions.server;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.entity.CompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Optional;
import java.util.UUID;

/**
 * The only legal recall implementation. It does not load chunks, cross
 * dimensions, place blocks, or guess that a location is safe. Companion
 * destinations additionally require living-entity clearance and an ephemeral
 * per-owner reservation so a recall burst cannot stack companions together.
 */
public final class SafeTeleport {
    private static final long COMPANION_DESTINATION_RESERVATION_TICKS = 60L;

    private SafeTeleport() {}

    public static boolean recallNearPlayer(final CompanionEntity companion, final ServerPlayer player, final int radius) {
        if (companion == null || player == null || !(companion.level() instanceof ServerLevel level) || player.level() != level) {
            return false;
        }
        return reserveCompanionSpot(level, companion, player, player.blockPosition(), radius)
                .map(destination -> teleport(companion, destination))
                .orElse(false);
    }

    /** Used by first spawn and safe-base restore before the entity has entered the level. */
    public static Optional<BlockPos> reserveCompanionSpawnSpot(final ServerLevel level, final CompanionEntity companion,
                                                                 final ServerPlayer owner, final BlockPos around, final int radius) {
        return reserveCompanionSpot(level, companion, owner, around, radius);
    }

    public static boolean rescueToCompanion(final Entity target, final CompanionEntity companion, final int radius) {
        if (target == null || companion == null || !(companion.level() instanceof ServerLevel level) || target.level() != level) {
            return false;
        }
        return findSafeSpot(level, target, companion.blockPosition(), radius)
                .map(destination -> teleport(target, destination))
                .orElse(false);
    }

    /**
     * Generic bounded safe-space finder for non-companion relocation checks.
     * It still rejects block hazards and any living entity occupying the result,
     * but it does not allocate a companion reservation.
     */
    public static Optional<BlockPos> findSafeSpot(final ServerLevel level, final Entity entity, final BlockPos around, final int radius) {
        if (level == null || entity == null || around == null) return Optional.empty();
        return scanSafeSpots(level, entity, around, radius, null, null, level.getGameTime());
    }

    /** Clears non-persistent recall/spawn claims when an integrated-player session ends. */
    public static void clearOwnerReservations(final UUID owner) {
        RecallPlacementReservationService.clearOwner(owner);
    }

    /** Clears an individual short-lived claim when lifecycle code discards an entity before expiry. */
    public static void releaseCompanionReservation(final UUID owner, final UUID companion) {
        RecallPlacementReservationService.release(owner, companion);
    }

    private static Optional<BlockPos> reserveCompanionSpot(final ServerLevel level, final CompanionEntity companion,
                                                             final ServerPlayer owner, final BlockPos around, final int radius) {
        if (level == null || companion == null || owner == null || owner.level() != level || around == null) return Optional.empty();
        final long now = level.getGameTime();
        final UUID ownerId = owner.getUUID();
        final UUID companionId = companion.getUUID();
        for (int distance = 1; distance <= Math.max(1, radius); distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dz = -distance; dz <= distance; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != distance) continue;
                    for (int dy = -2; dy <= 4; dy++) {
                        final BlockPos candidate = around.offset(dx, dy, dz);
                        if (RecallPlacementReservationService.isReservedByOther(ownerId, companionId, candidate, now, recallSeparation())
                                || !isSafeStanding(level, companion, candidate)) {
                            continue;
                        }
                        if (RecallPlacementReservationService.claim(ownerId, companionId, candidate, now,
                                now + COMPANION_DESTINATION_RESERVATION_TICKS, recallSeparation())) {
                            return Optional.of(candidate.immutable());
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> scanSafeSpots(final ServerLevel level, final Entity entity, final BlockPos around,
                                                      final int radius, final UUID owner, final UUID companion, final long now) {
        // Close cardinal/diagonal expansion only. This remains local, loaded,
        // and bounded; it never scans distant terrain or loads a chunk.
        for (int distance = 1; distance <= Math.max(1, radius); distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dz = -distance; dz <= distance; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != distance) continue;
                    for (int dy = -2; dy <= 4; dy++) {
                        final BlockPos candidate = around.offset(dx, dy, dz);
                        if (owner != null && companion != null
                                && RecallPlacementReservationService.isReservedByOther(owner, companion, candidate, now, recallSeparation())) {
                            continue;
                        }
                        if (isSafeStanding(level, entity, candidate)) return Optional.of(candidate.immutable());
                    }
                }
            }
        }
        return Optional.empty();
    }

    public static boolean isSafeStanding(final ServerLevel level, final Entity entity, final BlockPos feet) {
        if (level == null || entity == null || feet == null || !level.hasChunkAt(feet)
                || feet.getY() <= level.getMinBuildHeight() || feet.getY() >= level.getMaxBuildHeight() - 2) {
            return false;
        }
        final BlockPos floor = feet.below();
        final BlockPos head = feet.above();
        final BlockState floorState = level.getBlockState(floor);
        final BlockState feetState = level.getBlockState(feet);
        final BlockState headState = level.getBlockState(head);

        if (!floorState.isFaceSturdy(level, floor, Direction.UP)
                || isHazard(floorState) || isHazard(feetState) || isHazard(headState)
                || !level.getFluidState(feet).isEmpty() || !level.getFluidState(head).isEmpty()
                || !feetState.getCollisionShape(level, feet).isEmpty()
                || !headState.getCollisionShape(level, head).isEmpty()) {
            return false;
        }

        final double x = feet.getX() + 0.5D;
        final double y = feet.getY();
        final double z = feet.getZ() + 0.5D;
        final AABB translated = entity.getBoundingBox().move(x - entity.getX(), y - entity.getY(), z - entity.getZ());
        return level.noCollision(entity, translated) && hasLivingEntityClearance(level, entity, translated);
    }

    /** A block-safe square is still rejected when another living body occupies it. */
    private static boolean hasLivingEntityClearance(final ServerLevel level, final Entity entity, final AABB destination) {
        return level.getEntities(entity, destination.inflate(0.06D), candidate -> candidate instanceof LivingEntity
                && candidate.isAlive() && !candidate.isSpectator()).isEmpty();
    }

    private static double recallSeparation() {
        return Math.max(1.0D, CompanionConfig.NAVIGATION_SLOT_RESERVATION_RADIUS.get());
    }

    private static boolean isHazard(final BlockState state) {
        return state.is(Blocks.LAVA)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.POWDER_SNOW);
    }

    private static boolean teleport(final Entity entity, final BlockPos destination) {
        entity.teleportTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D);
        if (entity instanceof CompanionEntity companion) {
            companion.getNavigation().stop();
            companion.clearFormationSlot("SAFE_RECALL_DESTINATION_REASSIGN");
            companion.setLastSafeWaypoint(destination);
        }
        return true;
    }
}
