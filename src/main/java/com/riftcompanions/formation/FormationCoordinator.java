package com.riftcompanions.formation;

import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.SafeTeleport;
import com.riftcompanions.navigation.FormationSlotReservationService;
import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.spatial.SpatialEtiquetteService;
import com.riftcompanions.team.TeamBlackboard;
import com.riftcompanions.team.TeamPlanType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Allocates safe, role-specific slots at a bounded cadence. It never forces a
 * chunk load, places blocks, or asks pathfinding to walk through interaction
 * blocks just to make a formation look pretty.
 */
public final class FormationCoordinator {
    private static final long ASSIGNMENT_LIFETIME = 30L;

    private FormationCoordinator() {}

    public static FormationType choose(final ServerPlayer player, final TeamBlackboard board) {
        if (board.plan().isActive()) {
            if (board.plan().type() == TeamPlanType.RETREAT) {
                return FormationType.RETREAT;
            }
            if (board.plan().type() == TeamPlanType.DEFEND) {
                return FormationType.COMBAT;
            }
            if (board.plan().type() == TeamPlanType.STRUCTURE_ENTRY) {
                return FormationType.CAVE;
            }
        }
        final boolean hostileNearby = !player.level().getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(11.0D)).isEmpty();
        if (hostileNearby) {
            return FormationType.COMBAT;
        }
        if (board.encounterContext().recommendedFormation() == FormationType.CAVE || board.encounterContext().recommendedFormation() == FormationType.RETREAT) {
            return board.encounterContext().recommendedFormation();
        }
        return isTightSpace(player.serverLevel(), player.blockPosition()) ? FormationType.CAVE : FormationType.FOLLOW;
    }

    public static void update(final ServerPlayer player, final TeamBlackboard board, final long now) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        final FormationType formation = choose(player, board);
        board.setFormation(formation);
        final Vec3 forward = forward(player);
        final Vec3 right = new Vec3(forward.z, 0.0D, -forward.x);
        final Map<CompanionRole, FormationSlot> slots = slotsFor(formation);
        for (final CompanionRole role : CompanionRole.values()) {
            final CompanionEntity companion = CompanionLifecycleService.findForOwner(player, role).orElse(null);
            if (companion == null || companion.level() != level || blocksFormation(companion.getCompanionState())) {
                continue;
            }
            // A future role gets a safe generic rear slot without requiring Team Director changes.
            final FormationSlot slot = slots.getOrDefault(role, new FormationSlot(role, -2.6D, 0.0D, 0.0D));
            final Vec3 desired = slot.resolve(player.position(), forward, right);
            final BlockPos safe = findSafeSlot(level, companion, desired, player, board, now);
            if (safe != null && FormationSlotReservationService.claim(player.getUUID(), companion.getUUID(), safe, now,
                    now + ASSIGNMENT_LIFETIME, CompanionConfig.NAVIGATION_SLOT_RESERVATION_RADIUS.get())) {
                companion.assignFormationSlot(safe, formation, now + ASSIGNMENT_LIFETIME);
            } else {
                FormationSlotReservationService.release(player.getUUID(), companion.getUUID());
                companion.clearFormationSlot("FORMATION_SLOT_UNSAFE_OR_RESERVED");
            }
        }
    }

    private static boolean blocksFormation(final CompanionState state) {
        // Combat/guard movement owns its own local target path. Assigning a
        // formation slot here would overwrite a melee response and make an
        // attacked companion appear frozen.
        return state == CompanionState.DOWNED || state == CompanionState.SCOUTING || state == CompanionState.RETURNING_HOME
                || state == CompanionState.STUCK_RECOVERY || state == CompanionState.FIGHTING || state == CompanionState.GUARDING;
    }

    private static Map<CompanionRole, FormationSlot> slotsFor(final FormationType type) {
        final EnumMap<CompanionRole, FormationSlot> slots = new EnumMap<>(CompanionRole.class);
        switch (type) {
            case FOLLOW -> {
                // Nobody occupies the direct center of the player camera.
                slots.put(CompanionRole.GUARDIAN, new FormationSlot(CompanionRole.GUARDIAN, 1.25D, 1.55D, 0));
                slots.put(CompanionRole.SEER, new FormationSlot(CompanionRole.SEER, -2.00D, -1.20D, 0));
                slots.put(CompanionRole.GIFTED, new FormationSlot(CompanionRole.GIFTED, -0.65D, -2.05D, 0));
                slots.put(CompanionRole.SCOUT, new FormationSlot(CompanionRole.SCOUT, 0.10D, 2.70D, 0));
            }
            case CAVE -> {
                slots.put(CompanionRole.GUARDIAN, new FormationSlot(CompanionRole.GUARDIAN, 2.25D, 0.0D, 0));
                slots.put(CompanionRole.SEER, new FormationSlot(CompanionRole.SEER, -1.70D, -0.65D, 0));
                slots.put(CompanionRole.GIFTED, new FormationSlot(CompanionRole.GIFTED, -0.55D, 0.85D, 0));
                slots.put(CompanionRole.SCOUT, new FormationSlot(CompanionRole.SCOUT, -3.10D, 0.20D, 0));
            }
            case COMBAT -> {
                slots.put(CompanionRole.GUARDIAN, new FormationSlot(CompanionRole.GUARDIAN, 2.55D, 0.20D, 0));
                slots.put(CompanionRole.SEER, new FormationSlot(CompanionRole.SEER, -3.10D, -1.65D, 0));
                slots.put(CompanionRole.GIFTED, new FormationSlot(CompanionRole.GIFTED, -1.30D, 2.25D, 0));
                slots.put(CompanionRole.SCOUT, new FormationSlot(CompanionRole.SCOUT, 0.85D, -3.10D, 0));
            }
            case RETREAT -> {
                // Guardian guards the rear; vulnerable/support roles stay near the player.
                slots.put(CompanionRole.GUARDIAN, new FormationSlot(CompanionRole.GUARDIAN, -3.10D, 0.0D, 0));
                slots.put(CompanionRole.SEER, new FormationSlot(CompanionRole.SEER, -0.90D, -1.75D, 0));
                slots.put(CompanionRole.GIFTED, new FormationSlot(CompanionRole.GIFTED, -0.65D, 1.65D, 0));
                slots.put(CompanionRole.SCOUT, new FormationSlot(CompanionRole.SCOUT, 1.70D, 2.15D, 0));
            }
            case BASE -> {
                slots.put(CompanionRole.GUARDIAN, new FormationSlot(CompanionRole.GUARDIAN, 2.00D, 1.75D, 0));
                slots.put(CompanionRole.SEER, new FormationSlot(CompanionRole.SEER, -1.80D, -1.30D, 0));
                slots.put(CompanionRole.GIFTED, new FormationSlot(CompanionRole.GIFTED, -0.90D, 1.85D, 0));
                slots.put(CompanionRole.SCOUT, new FormationSlot(CompanionRole.SCOUT, 2.70D, -1.55D, 0));
            }
        }
        return slots;
    }

    private static Vec3 forward(final ServerPlayer player) {
        final float radians = player.getYRot() * Mth.DEG_TO_RAD;
        final Vec3 vector = new Vec3(-Mth.sin(radians), 0.0D, Mth.cos(radians));
        return vector.lengthSqr() < 0.001D ? new Vec3(0, 0, 1) : vector.normalize();
    }

    private static BlockPos findSafeSlot(final ServerLevel level, final CompanionEntity companion, final Vec3 desired,
                                         final ServerPlayer player, final TeamBlackboard board, final long now) {
        final BlockPos playerPos = player.blockPosition();
        final BlockPos center = BlockPos.containing(desired.x, desired.y, desired.z);
        // A max 3x3x5 local search; no broad block scan and no chunk loading.
        for (int radius = 0; radius <= 1; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int dy = -2; dy <= 2; dy++) {
                        final BlockPos candidate = center.offset(dx, dy, dz);
                        if (candidate.distSqr(playerPos) < 1.6D * 1.6D || !level.hasChunkAt(candidate)) {
                            continue;
                        }
                        if (board.hasAvoidanceAnnotation(level.dimension().location(), candidate, 2.0D)) {
                            continue;
                        }
                        if (SpatialEtiquetteService.isInteractionRadiusReserved(player.getUUID(), candidate, now)) {
                            continue;
                        }
                        if (FormationSlotReservationService.isReservedByOther(player.getUUID(), companion.getUUID(), candidate, now,
                                CompanionConfig.NAVIGATION_SLOT_RESERVATION_RADIUS.get())) {
                            continue;
                        }
                        if (nearInteractionBlock(level, candidate)) {
                            continue;
                        }
                        if (SafeTeleport.isSafeStanding(level, companion, candidate)) {
                            return candidate.immutable();
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean nearInteractionBlock(final ServerLevel level, final BlockPos position) {
        for (final BlockPos check : List.of(position, position.north(), position.south(), position.east(), position.west())) {
            final BlockState state = level.getBlockState(check);
            if (state.is(BlockTags.BEDS) || state.is(BlockTags.PRESSURE_PLATES) || state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)
                    || state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE)
                    || state.is(Blocks.SMOKER) || state.is(Blocks.ENCHANTING_TABLE)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTightSpace(final ServerLevel level, final BlockPos playerPos) {
        int blockedSides = 0;
        for (final BlockPos side : List.of(playerPos.north(), playerPos.south(), playerPos.east(), playerPos.west())) {
            if (!level.getBlockState(side).getCollisionShape(level, side).isEmpty()) {
                blockedSides++;
            }
        }
        return blockedSides >= 2;
    }
}
