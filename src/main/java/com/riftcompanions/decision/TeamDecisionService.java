package com.riftcompanions.decision;

import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamBlackboard;
import com.riftcompanions.team.events.TeamEventType;
import net.minecraft.server.level.ServerPlayer;

/** Classifies event ownership without letting multiple companions create competing plans. */
public final class TeamDecisionService {
    private TeamDecisionService() {}

    public static void observeEvent(ServerPlayer player, TeamEventType eventType) {
        if (player == null || eventType == null) return;
        TeamBlackboard board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        DecisionDirective directive = directiveFor(eventType);
        boolean changed = board.decisionState().update(directive.domain, directive.informationLead, directive.safetyLead,
                eventType.name(), player.level().getGameTime());
        if (changed && board.decisionState().canDiscuss(player.level().getGameTime())) {
            CompanionEntity speaker = CompanionLifecycleService.findForOwner(player, directive.informationLead).orElse(null);
            if (speaker != null && directive.trigger != null) {
                DialogueService.get().speak(speaker, directive.trigger, directive.priority);
                board.decisionState().markDiscussion(player.level().getGameTime());
            }
            TeamSavedData.get(player.server).markChanged();
        }
    }

    private static DecisionDirective directiveFor(TeamEventType event) {
        return switch (event) {
            case PLAYER_HEALTH_CRITICAL, PLAYER_FALL_RISK, COMPANION_DOWNED, EXPLOSIVE_NEAR_PROTECTED_AREA, HOSTILE_CROWD ->
                    new DecisionDirective(DecisionDomain.SURVIVAL_CRISIS, CompanionRole.GUARDIAN, CompanionRole.GUARDIAN, "decision_survival", 1);
            case STRUCTURE_REQUESTED, UNKNOWN_MOB_FIRST_SEEN, HIVE_LINKED_TARGET_DETECTED ->
                    new DecisionDirective(DecisionDomain.STRUCTURE_OR_MYSTERY, CompanionRole.SEER, CompanionRole.GUARDIAN, "decision_mystery", 2);
            case ROUTE_BLOCKED -> new DecisionDirective(DecisionDomain.ROUTE_OR_SCOUT, CompanionRole.SCOUT, CompanionRole.GUARDIAN, "decision_route", 2);
            case POWER_UNAVAILABLE -> new DecisionDirective(DecisionDomain.POWER_DECISION, CompanionRole.GIFTED, CompanionRole.GUARDIAN, "decision_power", 2);
            case PLAN_CANCELLED, NIGHT_APPROACHING, BIOME_RISK_CHANGED, BASE_RETURN_AFTER_CRISIS, MEMORY_LANDMARK_REVISITED ->
                    new DecisionDirective(DecisionDomain.CALM_OR_BASE, CompanionRole.GUARDIAN, CompanionRole.GUARDIAN, null, 3);
        };
    }

    private record DecisionDirective(DecisionDomain domain, CompanionRole informationLead, CompanionRole safetyLead, String trigger, int priority) {}
}
