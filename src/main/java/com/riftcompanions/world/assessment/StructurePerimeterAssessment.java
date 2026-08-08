package com.riftcompanions.world.assessment;

import net.minecraft.core.BlockPos;

import java.util.List;

/** Result of a player-requested bounded scan, not x-ray structure knowledge. */
public record StructurePerimeterAssessment(
        BlockPos focus,
        String profileId,
        int dangerScore,
        int visibleEntrances,
        boolean spawnerSeen,
        boolean redstoneOrTrapSeen,
        List<String> reasonCodes
) {
    public StructurePerimeterAssessment {
        focus = focus == null ? null : focus.immutable();
        dangerScore = Math.max(0, Math.min(100, dangerScore));
        visibleEntrances = Math.max(0, visibleEntrances);
        reasonCodes = List.copyOf(reasonCodes == null ? List.of() : reasonCodes.stream().limit(8).toList());
    }
}
