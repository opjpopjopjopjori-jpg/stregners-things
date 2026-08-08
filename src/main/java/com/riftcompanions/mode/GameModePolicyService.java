package com.riftcompanions.mode;

import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.team.TeamPlanService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;

/** Explicit gameplay-mode contract; commands are never treated as player wrongdoing. */
public final class GameModePolicyService {
    private GameModePolicyService() {}

    public static ModeDisposition apply(ServerPlayer player) {
        if (player == null) return ModeDisposition.OBSERVER;
        if (player.isSpectator()) {
            // Spectator must never preserve an executing survival plan in the
            // background. Cancellation is safe and leaves Follow/Recall/Journal available.
            TeamPlanService.cancel(player);
            for (CompanionRole role : CompanionRole.values()) {
                CompanionLifecycleService.findForOwner(player, role).ifPresent(companion -> {
                    if (companion.getCompanionState() != CompanionState.DOWNED) companion.setCompanionState(CompanionState.HOLDING, "SPECTATOR_MODE_PAUSED");
                });
            }
            return ModeDisposition.OBSERVER;
        }
        for (CompanionRole role : CompanionRole.values()) {
            CompanionLifecycleService.findForOwner(player, role).ifPresent(companion -> {
                if (companion.getCompanionState() == CompanionState.HOLDING && "SPECTATOR_MODE_PAUSED".equals(companion.getLastReasonCode())) {
                    companion.setCompanionState(CompanionState.FOLLOWING, "SPECTATOR_MODE_ENDED");
                }
            });
        }
        if (player.isCreative()) {
            return ModeDisposition.CALM;
        }
        if (player.level().getDifficulty() == Difficulty.PEACEFUL) {
            return ModeDisposition.PEACEFUL;
        }
        return ModeDisposition.SURVIVAL;
    }

    public enum ModeDisposition {
        SURVIVAL,
        CALM,
        PEACEFUL,
        OBSERVER;

        public boolean permitsAutomaticSurvivalPlans() { return this == SURVIVAL; }
        public boolean permitsThreatScan() { return this == SURVIVAL; }
    }
}
