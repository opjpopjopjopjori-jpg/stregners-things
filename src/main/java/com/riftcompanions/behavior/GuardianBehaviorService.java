package com.riftcompanions.behavior;

import com.riftcompanions.debug.DecisionTraceService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamPlan;
import com.riftcompanions.team.TeamPlanStatus;
import net.minecraft.server.level.ServerPlayer;

/**
 * Guardian reviews explain a completed, cancelled, or failed plan once. They
 * do not reopen a plan, override a player choice, or create more chat pressure.
 */
public final class GuardianBehaviorService {
    private GuardianBehaviorService() {}

    public static void recordAfterAction(final ServerPlayer player, final TeamPlan plan,
                                         final TeamPlanStatus status, final String reasonCode) {
        if (player == null || player.server == null) return;
        final long now = player.level().getGameTime();
        final String summary = summaryFor(plan, status);
        TeamSavedData.get(player.server).blackboard(player.getUUID()).guardianReview()
                .record(plan, status, summary, reasonCode, now);
        TeamSavedData.get(player.server).markChanged();
        DecisionTraceService.log(player, "GUARDIAN_REVIEW", (plan == null ? "NONE" : plan.type())
                + " / " + status + " / " + reasonCode);
    }

    private static String summaryFor(final TeamPlan plan, final TeamPlanStatus status) {
        final String planName = plan == null ? "team action" : plan.type().name().replace('_', ' ').toLowerCase(java.util.Locale.ROOT);
        if (status == null) return "The " + planName + " was returned to a safe follow state for review.";
        return switch (status) {
            case SUCCEEDED -> "The " + planName + " reached its bounded objective. Keep the route and conditions in mind before repeating it.";
            case CANCELLED -> "The " + planName + " was cleared. The player decision was respected and the team returned to follow mode.";
            case ABORTED -> "The " + planName + " ended before completion. The team returned to a simple, safe formation.";
            case FAILED_SAFE -> "The " + planName + " stopped safely because a target, route, or condition was no longer valid.";
            case FAILED -> "The " + planName + " did not hold under current conditions. Reassess the route before trying again.";
            case DRAFT, AWAITING_APPROVAL, ACTIVE -> "The " + planName + " remains open and does not yet have an after-action result.";
        };
    }
}
