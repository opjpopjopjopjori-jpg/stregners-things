package com.riftcompanions.interrupt;

import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.scene.SetPieceService;
import com.riftcompanions.team.events.TeamEventType;
import net.minecraft.server.level.ServerPlayer;

/**
 * Explicit cleanup contract for emergency interruption. It does not interrupt a
 * valid player rescue for ordinary alerts and never spends/refunds a power by
 * guessing; Hive channels own their own safety cancellation path.
 */
public final class ActionInterruptService {
    private ActionInterruptService() {}

    public static void applyForEvent(final ServerPlayer player, final TeamEventType eventType) {
        if (player == null || eventType == null) return;
        final ActionInterruptPriority priority = switch (eventType.priority()) {
            case P0_FATAL_EMERGENCY -> ActionInterruptPriority.P0_FATAL;
            case P1_IMMEDIATE_COMBAT -> ActionInterruptPriority.P1_IMMEDIATE;
            case P2_PLAN_STATE -> ActionInterruptPriority.P2_PLAYER_COMMAND;
            case P3_DISCOVERY -> ActionInterruptPriority.P3_PLAN;
            case P4_MEMORY_AMBIENT -> ActionInterruptPriority.P4_AMBIENT;
        };
        if (priority == ActionInterruptPriority.P0_FATAL || priority == ActionInterruptPriority.P1_IMMEDIATE) {
            if (priority == ActionInterruptPriority.P0_FATAL) SetPieceService.beginSilentWalk(player, 160L);
            for (final CompanionRole role : CompanionRole.values()) {
                final CompanionEntity companion = CompanionLifecycleService.findForOwner(player, role).orElse(null);
                if (companion == null || companion.getCompanionState() == CompanionState.DOWNED || companion.isBeingRescued()) continue;
                if (companion.getCompanionState() == CompanionState.SCOUTING) {
                    companion.cancelScout("INTERRUPTED_BY_" + eventType.name());
                }
                if (companion.getCompanionState() == CompanionState.BASE_ACTIVITY || companion.getCompanionState() == CompanionState.OBSERVING) {
                    companion.clearFormationSlot("INTERRUPTED_BY_" + eventType.name());
                    companion.setCompanionState(CompanionState.FOLLOWING, "INTERRUPTED_BY_" + eventType.name());
                }
            }
            if (eventType == TeamEventType.PLAYER_FALL_RISK || eventType == TeamEventType.PLAYER_HEALTH_CRITICAL) {
                com.riftcompanions.hive.control.HiveChannelManager.cancelForSafety(player, "Immediate danger interrupted the focus safely.");
            }
        }
    }
}
