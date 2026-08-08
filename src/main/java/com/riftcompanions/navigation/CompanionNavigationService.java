package com.riftcompanions.navigation;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.team.TeamBlackboard;
import com.riftcompanions.spatial.SpatialEtiquetteService;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative navigation safety layer over vanilla PathNavigation.
 * It does not replace Minecraft's path search with an unbounded A* scan. It
 * validates local goals/path samples, rate-limits replans, detects no-progress,
 * tries bounded alternatives, and escalates to the existing safe recovery path.
 */
public final class CompanionNavigationService {
    private static final Map<UUID, Progress> PROGRESS = new HashMap<>();

    private CompanionNavigationService() {}

    public static NavigationResult moveTo(final CompanionEntity companion, final ServerPlayer owner,
                                          final BlockPos requestedTarget, final double speed,
                                          final NavigationIntent intent, final TeamBlackboard board,
                                          final long now) {
        if (companion == null || owner == null || requestedTarget == null || board == null
                || !(companion.level() instanceof ServerLevel level) || owner.level() != level) {
            return NavigationResult.rejected("NAVIGATION_CONTEXT_INVALID", 0);
        }
        if (!CompanionConfig.CUSTOM_NAVIGATION_ENABLED.get()) {
            return legacyMove(companion, requestedTarget, speed, now);
        }
        if (!level.hasChunkAt(requestedTarget)) {
            return NavigationResult.rejected("NAVIGATION_TARGET_CHUNK_UNLOADED", 0);
        }

        final Progress progress = PROGRESS.computeIfAbsent(companion.getUUID(), ignored -> new Progress(owner.getUUID()));
        progress.owner = owner.getUUID();
        final double targetShift = CompanionConfig.NAVIGATION_TARGET_SHIFT_REPATH_DISTANCE.get();
        final boolean targetChanged = progress.requested == null || progress.requested.distSqr(requestedTarget) > targetShift * targetShift
                || progress.intent != intent;
        if (targetChanged) progress.reset(requestedTarget, intent, companion.blockPosition(), now);

        final double distance = horizontalDistanceSquared(companion.blockPosition(), requestedTarget);
        if (distance <= 2.25D && isWalkable(level, companion, companion.blockPosition())) {
            companion.getNavigation().stop();
            progress.markProgress(companion.blockPosition(), distance, now);
            return NavigationResult.arrived(requestedTarget);
        }

        final boolean stalled = progress.observe(companion.blockPosition(), distance, now);
        final boolean navigationDone = companion.getNavigation().isDone();
        if (!stalled && !navigationDone && now < progress.nextRepathAt) {
            return NavigationResult.waiting(progress.selected == null ? requestedTarget : progress.selected, progress.recoveryAttempts);
        }

        if (stalled) {
            progress.recoveryAttempts++;
            companion.getNavigation().stop();
            progress.nextRepathAt = now;
            if (progress.recoveryAttempts > CompanionConfig.NAVIGATION_MAX_RECOVERY_ATTEMPTS.get()) {
                return NavigationResult.stuck("NAVIGATION_NO_PROGRESS", progress.selected, progress.recoveryAttempts);
            }
        }

        final List<BlockPos> candidates = candidateGoals(requestedTarget, progress.recoveryAttempts,
                CompanionConfig.NAVIGATION_MAX_CANDIDATES.get());
        int pathAttempts = 0;
        for (final BlockPos candidate : candidates) {
            if (!isCandidateAllowed(level, companion, owner, board, candidate, intent, now)) continue;
            if (pathAttempts++ >= CompanionConfig.NAVIGATION_MAX_PATH_ATTEMPTS.get()) break;
            final Path path = companion.getNavigation().createPath(candidate, 1);
            if (path == null || !path.canReach() || !pathIsSafe(level, companion, board, path,
                    CompanionConfig.NAVIGATION_MAX_SAFE_DROP.get())) {
                continue;
            }
            if (companion.getNavigation().moveTo(path, speed)) {
                progress.markPath(candidate, companion.blockPosition(), horizontalDistanceSquared(companion.blockPosition(), candidate), now,
                        CompanionConfig.NAVIGATION_REPATH_INTERVAL_TICKS.get());
                return NavigationResult.moving(candidate, progress.recoveryAttempts);
            }
        }

        progress.recoveryAttempts++;
        progress.nextRepathAt = now + CompanionConfig.NAVIGATION_REPATH_INTERVAL_TICKS.get();
        if (progress.recoveryAttempts > CompanionConfig.NAVIGATION_MAX_RECOVERY_ATTEMPTS.get()) {
            return NavigationResult.stuck("NAVIGATION_NO_SAFE_REACHABLE_CANDIDATE", requestedTarget, progress.recoveryAttempts);
        }
        return NavigationResult.retrying("NAVIGATION_ALTERNATE_CANDIDATE_PENDING", requestedTarget, progress.recoveryAttempts);
    }

    /** Records the outcome after the caller has handled it, for owner-only developer diagnostics. */
    public static void recordOutcome(final UUID entity, final NavigationResult result) {
        if (entity == null || result == null) return;
        final Progress progress = PROGRESS.get(entity);
        if (progress != null) progress.recordOutcome(result);
    }

    public static NavigationDebugSnapshot debugSnapshot(final UUID entity) {
        if (entity == null) return NavigationDebugSnapshot.empty();
        final Progress progress = PROGRESS.get(entity);
        return progress == null ? NavigationDebugSnapshot.empty() : progress.debugSnapshot();
    }

    public static void clearEntity(final UUID entity) {
        if (entity != null) PROGRESS.remove(entity);
    }

    public static void clearOwner(final UUID owner) {
        if (owner == null) return;
        final Iterator<Map.Entry<UUID, Progress>> iterator = PROGRESS.entrySet().iterator();
        while (iterator.hasNext()) {
            if (owner.equals(iterator.next().getValue().owner)) iterator.remove();
        }
        FormationSlotReservationService.clearOwner(owner);
    }

    private static NavigationResult legacyMove(final CompanionEntity companion, final BlockPos target, final double speed, final long now) {
        final Path path = companion.getNavigation().createPath(target, 1);
        if (path == null || !companion.getNavigation().moveTo(path, speed)) {
            return NavigationResult.retrying("LEGACY_NAVIGATION_PATH_FAILED", target, 1);
        }
        return NavigationResult.moving(target, 0);
    }

    private static List<BlockPos> candidateGoals(final BlockPos requested, final int recoveryAttempt, final int maximum) {
        final List<BlockPos> candidates = new ArrayList<>();
        candidates.add(requested.immutable());
        final int[][] offsets = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
                {2, 0}, {-2, 0}, {0, 2}, {0, -2},
                {2, 1}, {-2, 1}, {2, -1}, {-2, -1}
        };
        final int rotate = offsets.length == 0 ? 0 : Math.floorMod(recoveryAttempt, offsets.length);
        for (int index = 0; index < offsets.length && candidates.size() < Math.max(1, maximum); index++) {
            final int[] offset = offsets[(index + rotate) % offsets.length];
            candidates.add(requested.offset(offset[0], 0, offset[1]).immutable());
        }
        return candidates;
    }

    private static boolean isCandidateAllowed(final ServerLevel level, final CompanionEntity companion,
                                              final ServerPlayer owner, final TeamBlackboard board,
                                              final BlockPos candidate, final NavigationIntent intent, final long now) {
        if (!level.hasChunkAt(candidate) || board.hasAvoidanceAnnotation(level.dimension().location(), candidate, 2.0D)) return false;
        if (SpatialEtiquetteService.isInteractionRadiusReserved(owner.getUUID(), candidate, now)) return false;
        if (!isWalkable(level, companion, candidate)) return false;
        if (intent == NavigationIntent.FORMATION && FormationSlotReservationService.isReservedByOther(owner.getUUID(),
                companion.getUUID(), candidate, now, CompanionConfig.NAVIGATION_SLOT_RESERVATION_RADIUS.get())) return false;
        return true;
    }

    private static boolean pathIsSafe(final ServerLevel level, final CompanionEntity companion,
                                      final TeamBlackboard board, final Path path, final int maxDrop) {
        final int count = path.getNodeCount();
        if (count <= 0 || count > CompanionConfig.NAVIGATION_MAX_PATH_NODES.get()) return false;
        BlockPos previous = companion.blockPosition();
        // Path length is capped by config, so validating every node remains
        // bounded while reliably catching a single unsafe cliff descent.
        for (int index = 0; index < count; index++) {
            final Node node = path.getNode(index);
            final BlockPos position = new BlockPos(node.x, node.y, node.z);
            if (!sampleNodeAllowed(level, companion, board, previous, position, maxDrop)) return false;
            previous = position;
        }
        return true;
    }

    private static boolean sampleNodeAllowed(final ServerLevel level, final CompanionEntity companion,
                                             final TeamBlackboard board, final BlockPos previous,
                                             final BlockPos position, final int maxDrop) {
        if (!level.hasChunkAt(position) || board.hasAvoidanceAnnotation(level.dimension().location(), position, 1.5D)) return false;
        if (previous.getY() - position.getY() > Math.max(1, maxDrop)) return false;
        return isWalkable(level, companion, position);
    }

    /**
     * More permissive than teleport safety for stairs, but intentionally rejects
     * doors, ladders, vines, cobwebs, fluids, hazards, leaves, and narrow block
     * interactions that routinely produce loops or cliff-side failures.
     */
    private static boolean isWalkable(final ServerLevel level, final CompanionEntity companion, final BlockPos feet) {
        if (feet.getY() <= level.getMinBuildHeight() || feet.getY() >= level.getMaxBuildHeight() - 2) return false;
        final BlockPos floor = feet.below();
        final BlockPos head = feet.above();
        final BlockState floorState = level.getBlockState(floor);
        final BlockState feetState = level.getBlockState(feet);
        final BlockState headState = level.getBlockState(head);
        if (!level.getFluidState(floor).isEmpty() || !level.getFluidState(feet).isEmpty() || !level.getFluidState(head).isEmpty()) return false;
        if (isForbiddenTraversalBlock(floorState) || isForbiddenTraversalBlock(feetState) || isForbiddenTraversalBlock(headState)) return false;
        if (floorState.getCollisionShape(level, floor).isEmpty()) return false;
        if (!feetState.getCollisionShape(level, feet).isEmpty() || !headState.getCollisionShape(level, head).isEmpty()) return false;
        return level.noCollision(companion, companion.getBoundingBox().move(
                feet.getX() + 0.5D - companion.getX(), feet.getY() - companion.getY(), feet.getZ() + 0.5D - companion.getZ()));
    }

    private static boolean isForbiddenTraversalBlock(final BlockState state) {
        return state.is(BlockTags.DOORS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.BEDS)
                || state.is(BlockTags.PRESSURE_PLATES) || state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.SMOKER) || state.is(Blocks.ENCHANTING_TABLE)
                || state.is(Blocks.LADDER) || state.is(Blocks.VINE) || state.is(Blocks.WEEPING_VINES)
                || state.is(Blocks.TWISTING_VINES) || state.is(Blocks.COBWEB)
                || state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS)
                || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(BlockTags.FENCE_GATES);
    }

    private static double horizontalDistanceSquared(final BlockPos one, final BlockPos two) {
        final double x = one.getX() - two.getX();
        final double z = one.getZ() - two.getZ();
        return x * x + z * z;
    }

    private static final class Progress {
        private UUID owner;
        private BlockPos requested;
        private BlockPos selected;
        private NavigationIntent intent;
        private BlockPos lastPosition;
        private double lastDistance = Double.MAX_VALUE;
        private long lastProgressAt;
        private long nextRepathAt;
        private int recoveryAttempts;
        private NavigationOutcome lastOutcome = NavigationOutcome.WAITING_FOR_REPATH;
        private String lastReasonCode = "PATH_NOT_REQUESTED";

        private Progress(final UUID owner) {
            this.owner = owner;
        }

        private void reset(final BlockPos target, final NavigationIntent newIntent, final BlockPos current, final long now) {
            requested = target.immutable();
            selected = null;
            intent = newIntent;
            lastPosition = current.immutable();
            lastDistance = Double.MAX_VALUE;
            lastProgressAt = now;
            nextRepathAt = now;
            recoveryAttempts = 0;
            lastOutcome = NavigationOutcome.WAITING_FOR_REPATH;
            lastReasonCode = "PATH_REQUEST_RESET";
        }

        private boolean observe(final BlockPos current, final double distance, final long now) {
            final boolean moved = lastPosition == null || lastPosition.distSqr(current) >= 1.0D;
            final boolean closer = distance + 0.35D < lastDistance;
            if (moved || closer) markProgress(current, distance, now);
            return now - lastProgressAt >= CompanionConfig.NAVIGATION_STUCK_TIMEOUT_TICKS.get();
        }

        private void markProgress(final BlockPos current, final double distance, final long now) {
            lastPosition = current.immutable();
            lastDistance = distance;
            lastProgressAt = now;
        }

        private void markPath(final BlockPos target, final BlockPos current, final double distance,
                              final long now, final long repathInterval) {
            selected = target.immutable();
            markProgress(current, distance, now);
            recoveryAttempts = 0;
            nextRepathAt = now + Math.max(4L, repathInterval);
        }

        private void recordOutcome(final NavigationResult result) {
            lastOutcome = result.outcome();
            lastReasonCode = result.reasonCode() == null ? "NAVIGATION_RESULT_UNKNOWN" : result.reasonCode();
            if (result.selectedTarget() != null) selected = result.selectedTarget().immutable();
            recoveryAttempts = Math.max(recoveryAttempts, Math.max(0, result.recoveryAttempts()));
        }

        private NavigationDebugSnapshot debugSnapshot() {
            return new NavigationDebugSnapshot(intent == null ? "NONE" : intent.name(), requested, selected,
                    recoveryAttempts, lastProgressAt, nextRepathAt,
                    lastOutcome == null ? "NONE" : lastOutcome.name(), lastReasonCode);
        }
    }
}
