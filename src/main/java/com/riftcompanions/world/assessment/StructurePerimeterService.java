package com.riftcompanions.world.assessment;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bounded player-looked-at perimeter scan. It inspects only already-loaded
 * blocks around the actual server raycast hit; it never claims to know a
 * dungeon layout behind walls.
 */
public final class StructurePerimeterService {
    private static final double INSPECT_RANGE = 12.0D;

    private StructurePerimeterService() {}

    public static Optional<StructurePerimeterAssessment> inspectLookTarget(final ServerPlayer player) {
        final ServerLevel level = player.serverLevel();
        final Vec3 start = player.getEyePosition();
        final Vec3 end = start.add(player.getViewVector(1.0F).scale(INSPECT_RANGE));
        final BlockHitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS || !level.hasChunkAt(hit.getBlockPos())) {
            return Optional.empty();
        }
        return Optional.of(scan(level, hit.getBlockPos()));
    }

    private static StructurePerimeterAssessment scan(final ServerLevel level, final BlockPos focus) {
        final List<String> reasons = new ArrayList<>();
        int entrances = 0;
        boolean spawner = false;
        boolean trap = false;
        boolean bell = false;
        boolean cobweb = false;
        boolean portalFrame = false;
        boolean ruinedPortalBlocks = false;
        int darkCells = 0;
        String observedOverrideProfile = null;
        int observedOverrideCaution = 0;
        // 9x5x9 = 405 max positions and only for an explicit player request.
        for (final BlockPos pos : BlockPos.betweenClosed(focus.offset(-4, -2, -4), focus.offset(4, 2, 4))) {
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            final BlockState state = level.getBlockState(pos);
            final StructureProfileOverride override = StructureProfileOverrideRegistry.forBlock(state.getBlock()).orElse(null);
            if (override != null && (observedOverrideProfile == null || override.cautionBias() > observedOverrideCaution)) {
                observedOverrideProfile = override.profileId();
                observedOverrideCaution = override.cautionBias();
            }
            if (state.is(Blocks.SPAWNER)) {
                spawner = true;
            }
            if (state.is(Blocks.BELL)) bell = true;
            if (state.is(Blocks.COBWEB)) cobweb = true;
            if (state.is(Blocks.END_PORTAL_FRAME)) portalFrame = true;
            if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)) ruinedPortalBlocks = true;
            if (state.is(Blocks.TNT) || state.is(Blocks.TRIPWIRE_HOOK) || state.is(Blocks.REDSTONE_WIRE)
                    || state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON) || state.is(Blocks.LAVA)) {
                trap = true;
            }
            if (state.isAir() && level.getMaxLocalRawBrightness(pos) <= 5) {
                darkCells++;
            }
        }
        for (final BlockPos side : List.of(focus.north(), focus.south(), focus.east(), focus.west())) {
            if (level.hasChunkAt(side) && level.getBlockState(side).isAir() && level.getBlockState(side.above()).isAir()) {
                entrances++;
            }
        }
        int danger = 0;
        if (spawner) {
            danger += 35;
            reasons.add("VISIBLE_SPAWNER");
        }
        if (trap) {
            danger += 30;
            reasons.add("VISIBLE_TRAP_OR_HAZARD");
        }
        if (darkCells >= 12) {
            danger += 18;
            reasons.add("LOW_LIGHT_AREA");
        }
        if (entrances <= 1) {
            danger += 12;
            reasons.add("LIMITED_VISIBLE_EXIT");
        }
        if (reasons.isEmpty()) {
            reasons.add("PLAYER_REQUESTED_PERIMETER_CHECK");
        }
        String profileId = bell ? "village" : portalFrame ? "stronghold" : spawner ? "dungeon" : cobweb ? "mineshaft" : ruinedPortalBlocks ? "ruined_portal" : "unknown";
        if (observedOverrideProfile != null) {
            profileId = observedOverrideProfile;
            danger += observedOverrideCaution;
            reasons.add("OBSERVED_STRUCTURE_OVERRIDE_" + observedOverrideProfile.toUpperCase(java.util.Locale.ROOT));
        }
        reasons.add("PROFILE_" + profileId.toUpperCase(java.util.Locale.ROOT));
        return new StructurePerimeterAssessment(focus, profileId, danger, entrances, spawner, trap, reasons);
    }
}
