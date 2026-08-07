package com.riftcompanions.team;

import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.behavior.GuardianBehaviorService;
import com.riftcompanions.doctrine.PlayerHabitService;
import com.riftcompanions.doctrine.TeamDoctrine;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.formation.FormationCoordinator;
import com.riftcompanions.formation.FormationType;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.relationship.RelationService;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.SafeTeleport;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.server.BaseMilestoneService;
import com.riftcompanions.world.assessment.StructurePerimeterAssessment;
import com.riftcompanions.feature.FeatureFlag;
import com.riftcompanions.feature.FeatureFlags;
import com.riftcompanions.safety.SafeModeService;
import com.riftcompanions.transaction.ActionPhase;
import com.riftcompanions.transaction.ActionTransaction;
import com.riftcompanions.transaction.ActionType;
import com.riftcompanions.arc.ArcMilestone;
import com.riftcompanions.arc.ArcService;
import com.riftcompanions.consequence.ConsequenceService;
import com.riftcompanions.consequence.ConsequenceType;
import com.riftcompanions.scene.SetPieceService;
import com.riftcompanions.scene.SetPieceType;
import com.riftcompanions.mode.VanillaRulePolicyService;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Bounded plan manager. Only RETREAT can start automatically in the current
 * policy; DEFEND and STRUCTURE_ENTRY are explicit proposals requiring the
 * player's approval before they change team behavior.
 */
public final class TeamPlanService {
    private TeamPlanService() {}

    public static boolean beginRetreat(final ServerPlayer player, final String initiatingReason) {
        if (player == null || SafeModeService.enabled(player) || !FeatureFlags.enabled(FeatureFlag.TEAM_PLANS)) return false;
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        final long activeCount = java.util.Arrays.stream(CompanionRole.values())
                .filter(role -> CompanionLifecycleService.findForOwner(player, role).isPresent()).count();
        if (activeCount == 0L) {
            return false;
        }
        final ThreatSnapshot snapshot = ThreatSnapshot.assess(player);
        board.updateThreat(snapshot.dangerScore(), snapshot.reasonCodes());
        if (board.plan().isActive() && board.plan().type() == TeamPlanType.RETREAT) {
            return false;
        }
        final var reasons = new ArrayList<>(snapshot.reasonCodes());
        if (!reasons.contains(initiatingReason)) {
            reasons.add(initiatingReason);
        }
        final TeamPlan plan = TeamPlan.active(TeamPlanType.RETREAT, CompanionRole.GUARDIAN,
                player.level().getGameTime(), 400L, "Reach the most recent safe waypoint without splitting the team.", reasons);
        board.startPlan(plan);
        BaseMilestoneService.onPlanCreatedAtSafeBase(player, plan);
        recordPlanAction(board, ActionType.PLAN_CREATE, plan, player.level().getGameTime(), "RETREAT_PLAN_CREATED");
        board.setFormation(FormationType.RETREAT);
        if (SafeTeleport.isSafeStanding(player.serverLevel(), player, player.blockPosition())) {
            board.setLastSafeWaypoint(player.blockPosition());
        }
        for (final CompanionRole role : CompanionRole.values()) {
            CompanionLifecycleService.findForOwner(player, role).ifPresent(companion ->
                    companion.setCompanionState(CompanionState.RETREATING, "TEAM_PLAN_RETREAT"));
        }
        CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                .ifPresent(guardian -> DialogueService.get().speak(guardian, "retreat_start", 1));
        data.markChanged();
        PlayerHabitService.observe(player, TeamDoctrine.STAY_TOGETHER);
        return true;
    }

    public static PlanResult proposeDefend(final ServerPlayer player, final List<String> reasonCodes) {
        return propose(player, TeamPlanType.DEFEND, CompanionRole.GUARDIAN, 160L, 260L,
                "Hold a safe line and prevent the team from being separated.",
                "Guardian guards the nearest threat while the team holds formation.",
                "Regroup and retreat if the line breaks or a P0 emergency appears.",
                "Player health critical, route lost, or defend timeout.", reasonCodes, "guard_order", null);
    }

    public static PlanResult proposeStructureEntry(final ServerPlayer player, final StructurePerimeterAssessment assessment) {
        if (player == null || assessment == null || assessment.focus() == null) {
            return PlanResult.failure("STRUCTURE_TARGET_INVALID", "A visible, loaded structure focus is required for this plan.");
        }
        final List<String> reasons = new ArrayList<>(assessment.reasonCodes());
        var profile = com.riftcompanions.encounter.EncounterProfileRegistry.structureProfile(assessment);
        reasons.add("STRUCTURE_FOCUS_" + assessment.focus().getX() + "_" + assessment.focus().getY() + "_" + assessment.focus().getZ());
        reasons.add("ENCOUNTER_" + profile.id().toUpperCase(java.util.Locale.ROOT));
        return propose(player, TeamPlanType.STRUCTURE_ENTRY, CompanionRole.GUARDIAN, 200L, 480L,
                "Investigate the player-marked " + profile.id() + " entrance without splitting the team.",
                "Guardian secures the entrance; the team uses a cave formation and observes visible evidence.",
                "Mark the entrance and return if exits or conditions are unsafe.",
                "Threat escalation, player cancel, route failure, or timeout.", reasons, "structure_found",
                new PlanTarget(player.level().dimension().location(), assessment.focus(), "STRUCTURE_ENTRY"));
    }

    private static PlanResult propose(final ServerPlayer player, final TeamPlanType type, final CompanionRole leader,
                                      final long proposalLifetime, final long activeTimeout, final String objective,
                                      final String planA, final String planB, final String abortCondition,
                                      final List<String> reasons, final String dialogueTrigger, final PlanTarget target) {
        if (player == null || SafeModeService.enabled(player) || !FeatureFlags.enabled(FeatureFlag.TEAM_PLANS)) {
            return PlanResult.failure("TEAM_PLANS_DISABLED_OR_SAFE_MODE", "Team plans are unavailable while the feature is disabled or Safe Mode is active.");
        }
        if (type == TeamPlanType.DEFEND && !VanillaRulePolicyService.allowsCombatPlanning(player)) {
            return PlanResult.failure(VanillaRulePolicyService.combatPlanningBlockCode(player),
                    "Combat planning is paused by the current Vanilla game mode or Peaceful difficulty. Follow, Recall, and exploration guidance remain available.");
        }
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        if (board.plan().isActive() || board.plan().awaitsApproval()) {
            return PlanResult.failure("PLAN_ALREADY_ACTIVE_OR_AWAITING_APPROVAL", "A plan is already active or awaiting approval. Cancel or finish it first.");
        }
        if (board.recentlySucceededPlan(type, player.level().getGameTime(), 400L) && !reasons.contains("PLAYER_HEALTH_CRITICAL")) {
            return PlanResult.failure("PLAN_VARIETY_COOLDOWN", "The same plan just succeeded. Use a different option unless the danger changed.");
        }
        final long activeCount = java.util.Arrays.stream(CompanionRole.values())
                .filter(role -> CompanionLifecycleService.findForOwner(player, role).isPresent()).count();
        if (activeCount == 0L) {
            return PlanResult.failure("NO_ACTIVE_TEAM", "No loaded companion is available to draft a plan.");
        }
        final TeamPlan plan = TeamPlan.awaitingApprovalAt(type, leader, player.level().getGameTime(), proposalLifetime,
                activeTimeout, objective, planA, planB, abortCondition, reasons, target);
        board.startPlan(plan);
        BaseMilestoneService.onPlanCreatedAtSafeBase(player, plan);
        recordPlanAction(board, ActionType.PLAN_CREATE, plan, player.level().getGameTime(), "PLAN_AWAITING_APPROVAL");
        data.markChanged();
        com.riftcompanions.onboarding.OnboardingService.onPlanProposal(player);
        CompanionLifecycleService.findForOwner(player, leader).ifPresent(companion -> DialogueService.get().speak(companion, dialogueTrigger, 1));
        return PlanResult.success("PLAN_AWAITING_APPROVAL", "The plan is ready. Accept or decline it in the Team Journal or with a command.");
    }

    public static PlanResult acceptCurrentPlan(final ServerPlayer player) {
        if (player == null || SafeModeService.enabled(player) || !FeatureFlags.enabled(FeatureFlag.TEAM_PLANS)) {
            return PlanResult.failure("TEAM_PLANS_DISABLED_OR_SAFE_MODE", "Team plans are unavailable while the feature is disabled or Safe Mode is active.");
        }
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        final TeamPlan plan = board.plan();
        if (!plan.hasValidTarget(player)) {
            plan.finish(TeamPlanStatus.FAILED_SAFE);
            GuardianBehaviorService.recordAfterAction(player, plan, TeamPlanStatus.FAILED_SAFE, "PLAN_TARGET_INVALID_ON_ACCEPT");
            board.setFormation(FormationType.FOLLOW);
            data.markChanged();
            return PlanResult.failure("PLAN_TARGET_INVALID_OR_UNLOADED", "The player-visible plan target is no longer valid or loaded. The team returned to follow mode.");
        }
        if (!plan.activate(player.level().getGameTime())) {
            return PlanResult.failure("NO_PLAN_AWAITING_APPROVAL", "No plan is currently awaiting approval.");
        }
        recordPlanAction(board, ActionType.PLAN_ACTIVATE, plan, player.level().getGameTime(), "PLAN_ACTIVATED");
        ConsequenceService.record(player, ConsequenceType.TACTICAL, "plan_accept:" + plan.actionId(),
                "Player approved the " + plan.type() + " plan after reviewing the team reasons.", 82);
        switch (plan.type()) {
            case DEFEND -> {
                board.setFormation(FormationType.COMBAT);
                CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                        .ifPresent(companion -> companion.setCompanionState(CompanionState.GUARDING, "TEAM_PLAN_DEFEND"));
                for (final CompanionRole role : List.of(CompanionRole.SEER, CompanionRole.GIFTED, CompanionRole.SCOUT)) {
                    CompanionLifecycleService.findForOwner(player, role)
                            .ifPresent(companion -> companion.setCompanionState(CompanionState.FOLLOWING, "TEAM_PLAN_DEFEND"));
                }
            }
            case STRUCTURE_ENTRY -> {
                board.setFormation(FormationType.CAVE);
                CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                        .ifPresent(companion -> companion.setCompanionState(CompanionState.GUARDING, "TEAM_PLAN_STRUCTURE_ENTRY"));
                CompanionLifecycleService.findForOwner(player, CompanionRole.SEER)
                        .ifPresent(companion -> companion.setCompanionState(CompanionState.OBSERVING, "TEAM_PLAN_STRUCTURE_ENTRY"));
                for (final CompanionRole role : List.of(CompanionRole.GIFTED, CompanionRole.SCOUT)) {
                    CompanionLifecycleService.findForOwner(player, role)
                            .ifPresent(companion -> companion.setCompanionState(CompanionState.FOLLOWING, "TEAM_PLAN_STRUCTURE_ENTRY"));
                }
            }
            default -> { }
        }
        data.markChanged();
        com.riftcompanions.server.CompanionPresentationSoundService.playPlanAccepted(player);
        return PlanResult.success("PLAN_ACCEPTED", "Activated " + plan.type() + ". The player remains responsible for the final decision and decisive action.");
    }

    public static PlanResult declineCurrentPlan(final ServerPlayer player) {
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        final TeamPlan plan = board.plan();
        if (!plan.awaitsApproval()) {
            return PlanResult.failure("NO_PLAN_AWAITING_APPROVAL", "No pending plan is available to decline.");
        }
        plan.finish(TeamPlanStatus.CANCELLED);
        recordPlanAction(board, ActionType.PLAN_FINISH, plan, player.level().getGameTime(), "PLAN_DECLINED");
        GuardianBehaviorService.recordAfterAction(player, plan, TeamPlanStatus.CANCELLED, "PLAN_DECLINED_BY_PLAYER");
        ConsequenceService.record(player, ConsequenceType.RELATIONSHIP, "plan_decline:" + plan.actionId(),
                "Player deferred the " + plan.type() + " plan; the team returned to follow mode without blame.", 76);
        ArcService.observe(player, ArcMilestone.PLAN_DECLINED_SAFELY);
        board.setFormation(FormationType.FOLLOW);
        CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                .ifPresent(companion -> DialogueService.get().speak(companion, "plan_cancelled", 2));
        data.markChanged();
        return PlanResult.success("PLAN_DECLINED", "Your decision was respected. The team has returned to follow mode.");
    }

    public static void tick(final ServerPlayer player) {
        if (player == null || SafeModeService.enabled(player) || !FeatureFlags.enabled(FeatureFlag.TEAM_PLANS)) return;
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        final TeamPlan plan = board.plan();
        final long now = player.level().getGameTime();
        if (plan.isOpen() && !plan.hasValidTarget(player)) {
            finishPlan(player, TeamPlanStatus.FAILED_SAFE, "TEAM_PLAN_TARGET_INVALID", "Plan target is no longer valid or loaded; the team returned to follow mode.");
            return;
        }
        if (plan.awaitsApproval() && plan.timedOut(now)) {
            plan.finish(TeamPlanStatus.ABORTED);
            GuardianBehaviorService.recordAfterAction(player, plan, TeamPlanStatus.ABORTED, "PLAN_APPROVAL_TIMEOUT");
            board.setFormation(FormationType.FOLLOW);
            data.markChanged();
            return;
        }
        if (plan.isActive() && plan.type() == TeamPlanType.DEFEND && !VanillaRulePolicyService.allowsCombatPlanning(player)) {
            finishPlan(player, TeamPlanStatus.CANCELLED, VanillaRulePolicyService.combatPlanningBlockCode(player),
                    "Defend planning was paused by the current Vanilla game mode or Peaceful difficulty.");
            return;
        }
        if (!plan.isActive()) {
            return;
        }
        final ThreatSnapshot snapshot = ThreatSnapshot.assess(player);
        board.updateThreat(snapshot.dangerScore(), snapshot.reasonCodes());
        if (plan.timedOut(now)) {
            finishPlan(player, TeamPlanStatus.FAILED, "TEAM_PLAN_TIMEOUT", "Plan timed out; route or conditions need reassessment.");
            return;
        }
        switch (plan.type()) {
            case RETREAT -> {
                if (now - plan.startedAt() > 40L && snapshot.dangerScore() < 20) {
                    finishPlan(player, TeamPlanStatus.SUCCEEDED, "TEAM_PLAN_RETREAT_SUCCEEDED", "The team withdrew safely after a dangerous encounter.");
                }
            }
            case DEFEND -> {
                final boolean threatsRemain = !player.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                        player.getBoundingBox().inflate(12.0D)).isEmpty();
                if (now - plan.startedAt() > 40L && !threatsRemain) {
                    finishPlan(player, TeamPlanStatus.SUCCEEDED, "TEAM_PLAN_DEFEND_SUCCEEDED", "The team held the line and secured a brief safe window.");
                }
            }
            case STRUCTURE_ENTRY -> {
                // Entry does not auto-loot or force the player through a door.
                // It stays active only as a formation/guard protocol until cancelled or timed out.
            }
            default -> { }
        }
    }

    public static void cancel(final ServerPlayer player) {
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        if (board.plan().isActive() || board.plan().awaitsApproval()) {
            final TeamPlan active = board.plan();
            board.completePlan(TeamPlanStatus.CANCELLED);
            recordPlanAction(board, ActionType.PLAN_FINISH, active, player.level().getGameTime(), "PLAN_CANCELLED");
            GuardianBehaviorService.recordAfterAction(player, active, TeamPlanStatus.CANCELLED, "PLAYER_CANCELLED_PLAN");
            board.setFormation(FormationType.FOLLOW);
            returnCompanionsToFollow(player, "PLAYER_CANCELLED_PLAN");
            CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                    .ifPresent(companion -> DialogueService.get().speak(companion, "plan_cancelled", 2));
            data.markChanged();
        }
    }

    private static void finishPlan(final ServerPlayer player, final TeamPlanStatus status, final String stateReason, final String memory) {
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        final TeamPlanType completedPlanType = board.plan().type();
        board.completePlan(status);
        GuardianBehaviorService.recordAfterAction(player, board.plan(), status, stateReason);
        recordPlanAction(board, ActionType.PLAN_FINISH, board.plan(), player.level().getGameTime(), "PLAN_" + status.name());
        board.setFormation(FormationType.FOLLOW);
        board.beginQuietWindow(player.level().getGameTime(), status == TeamPlanStatus.SUCCEEDED ? 160L : 240L);
        if (status == TeamPlanStatus.SUCCEEDED) SetPieceService.request(player, SetPieceType.RETURN_HOME, 1200L);
        else SetPieceService.beginSilentWalk(player, 240L);
        final MemoryType type = completedPlanType == TeamPlanType.RETREAT ? MemoryType.RETREAT : MemoryType.MILESTONE;
        board.addMemory(new MemoryRecord(type, player.level().getGameTime() / 24000L, memory, status == TeamPlanStatus.SUCCEEDED ? 90 : 70));
        returnCompanionsToFollow(player, stateReason);
        CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                .ifPresent(companion -> DialogueService.get().speak(companion, status == TeamPlanStatus.SUCCEEDED ? "post_crisis" : "plan_cancelled", status == TeamPlanStatus.SUCCEEDED ? 3 : 2));
        if (status == TeamPlanStatus.SUCCEEDED) {
            if (completedPlanType == TeamPlanType.RETREAT) ArcService.observe(player, ArcMilestone.RETREAT_SUCCEEDED);
            board.recordPlanSuccess(completedPlanType, player.level().getGameTime());
            RelationService.recordPlanSuccess(player);
            CompanionLifecycleService.findForOwner(player, CompanionRole.GUARDIAN)
                    .ifPresent(companion -> DialogueService.get().speak(companion, "player_support", 3));
        }
        data.markChanged();
    }

    private static void recordPlanAction(final TeamBlackboard board, final ActionType type, final TeamPlan plan,
                                         final long now, final String reason) {
        if (board == null || plan == null) return;
        final ActionTransaction transaction = board.actionLedger().begin(type, "plan:" + plan.actionId(),
                plan.type().name(), now, 240L).transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("PLAN_TRANSACTION_VALIDATED");
        if (transaction.phase() == ActionPhase.RESERVED) transaction.applied(reason);
        if (transaction.phase() == ActionPhase.APPLIED) transaction.commit(now, reason);
    }

    private static void returnCompanionsToFollow(final ServerPlayer player, final String reason) {
        for (final CompanionRole role : CompanionRole.values()) {
            CompanionLifecycleService.findForOwner(player, role).ifPresent(companion ->
                    companion.setCompanionState(CompanionState.FOLLOWING, reason));
        }
    }

    public record PlanResult(boolean successful, String code, String detail) {
        public static PlanResult success(final String code, final String detail) { return new PlanResult(true, code, detail); }
        public static PlanResult failure(final String code, final String detail) { return new PlanResult(false, code, detail); }
    }
}
