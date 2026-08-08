package com.riftcompanions.hive.control;

import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.team.TeamDirector;
import com.riftcompanions.team.events.TeamEventType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** Tactical reactions after a real Hive control state begins; no forced player camera or stacked auto-casts. */
public final class HiveTeamReactionService {
    private HiveTeamReactionService() {}

    public static void onApplied(ServerPlayer player, CompanionEntity seer, HiveControlMode mode, Mob target, int affectedCount) {
        com.riftcompanions.server.CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                .ifPresent(guardian -> {
                    guardian.setCompanionState(CompanionState.GUARDING, "HIVE_CONTROL_WINDOW");
                    DialogueService.get().speak(guardian, "guard_order", 1);
                });
        com.riftcompanions.server.CompanionLifecycleService.findForOwner(player, CompanionRole.SCOUT)
                .ifPresent(scout -> DialogueService.get().speak(scout, "route_blocked", 2));
        // Gifted remains a reserve. No automatic large power stacks over Will's action.
        TeamDirector.submit(player, TeamEventType.HIVE_LINKED_TARGET_DETECTED,
                java.util.List.of("HIVE_CONTROL_" + mode, "TARGETS_" + affectedCount));
    }
}
