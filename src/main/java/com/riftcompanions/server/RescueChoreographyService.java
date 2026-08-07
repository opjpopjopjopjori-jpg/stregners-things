package com.riftcompanions.server;

import com.riftcompanions.entity.CompanionAction;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import net.minecraft.server.level.ServerPlayer;

/**
 * Readable, non-autonomous rescue support. It assigns posture/cues only; no
 * companion breaks blocks, teleports, spends power, or takes control from the player.
 */
public final class RescueChoreographyService {
    private RescueChoreographyService() {}

    public static void onCompanionDowned(final ServerPlayer player, final CompanionEntity downed) {
        if (player == null || downed == null) return;
        for (final CompanionRole role : CompanionRole.values()) {
            final CompanionEntity companion = CompanionLifecycleService.findForOwner(player, role).orElse(null);
            if (companion == null || companion == downed || companion.getCompanionState() == CompanionState.DOWNED) continue;
            switch (role) {
                case GUARDIAN -> companion.setCompanionState(CompanionState.GUARDING, "RESCUE_CHOREOGRAPHY_GUARD_LINE");
                case SEER -> {
                    companion.setCompanionState(CompanionState.OBSERVING, "RESCUE_CHOREOGRAPHY_OBSERVE_RISK");
                    companion.beginVisualAction(CompanionAction.SEER_NOTICE, 16L);
                }
                case GIFTED -> companion.beginVisualAction(CompanionAction.GIFTED_NOTICE, 12L);
                case SCOUT -> companion.beginVisualAction(CompanionAction.SCOUT_POINT, 16L);
            }
        }
    }
}
