package com.riftcompanions.onboarding;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.debug.DecisionTraceService;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.server.TeamSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-owned one-time guidance delivery. It sends concise English system text
 * only after a real local event and never changes plan, combat, inventory, or
 * player input state.
 */
public final class OnboardingService {
    private OnboardingService() {}

    public static void onFirstCompanionActive(final ServerPlayer player, final CompanionRole role) {
        offer(player, OnboardingHint.FIRST_COMPANION, role == null ? "first_companion" : "first_companion:" + role.id());
        final var rules = com.riftcompanions.mode.VanillaRulePolicyService.snapshot(player);
        if (rules.playerCreative() || rules.playerSpectator() || rules.peaceful() || !rules.mobGriefing()
                || rules.keepInventory() || !rules.daylightCycle() || rules.hardcore()) {
            onWorldRulePolicy(player);
        }
    }

    public static void onPlanProposal(final ServerPlayer player) {
        offer(player, OnboardingHint.FIRST_PLAN_PROPOSAL, "plan_proposal");
    }

    public static void onStuckRecovery(final ServerPlayer player) {
        offer(player, OnboardingHint.FIRST_STUCK_RECOVERY, "stuck_recovery");
    }

    public static void onSafeMode(final ServerPlayer player) {
        offer(player, OnboardingHint.FIRST_SAFE_MODE, "safe_mode");
    }

    public static void onWorldRulePolicy(final ServerPlayer player) {
        offer(player, OnboardingHint.WORLD_RULE_POLICY, "world_rule_policy");
    }

    public static Result status(final ServerPlayer player) {
        if (player == null || player.server == null) return Result.failure("NO_SERVER", "No logical server is available.");
        final OnboardingState state = TeamSavedData.get(player.server).blackboard(player.getUUID()).onboarding();
        final OnboardingHint next = state.recommendedHint().orElse(null);
        final String nextLabel = next == null ? "complete" : next.id();
        return Result.success("TUTORIAL_STATUS", "Guidance " + state.deliveredCount() + "/" + state.totalHints()
                + ", dismissed=" + state.dismissed() + ", next=" + nextLabel + ".");
    }

    public static Result dismiss(final ServerPlayer player) {
        if (player == null || player.server == null) return Result.failure("NO_SERVER", "No logical server is available.");
        final OnboardingState state = TeamSavedData.get(player.server).blackboard(player.getUUID()).onboarding();
        state.dismiss();
        TeamSavedData.get(player.server).markChanged();
        DecisionTraceService.log(player, "TUTORIAL", "Guidance dismissed by player");
        return Result.success("TUTORIAL_DISMISSED", "Optional guidance is dismissed. Use /companions tutorial resume to enable it again.");
    }

    public static Result resume(final ServerPlayer player) {
        if (player == null || player.server == null) return Result.failure("NO_SERVER", "No logical server is available.");
        final OnboardingState state = TeamSavedData.get(player.server).blackboard(player.getUUID()).onboarding();
        state.resume();
        TeamSavedData.get(player.server).markChanged();
        DecisionTraceService.log(player, "TUTORIAL", "Guidance resumed by player");
        final OnboardingHint recommendation = state.recommendedHint().orElse(null);
        if (recommendation != null) {
            player.sendSystemMessage(Component.literal("[Rift Guide] " + recommendation.title() + ": " + recommendation.detail()));
        }
        return Result.success("TUTORIAL_RESUMED", "Optional guidance is active again.");
    }

    private static void offer(final ServerPlayer player, final OnboardingHint hint, final String traceDetail) {
        if (player == null || player.server == null || !CompanionConfig.TUTORIAL_ENABLED.get()) return;
        final OnboardingState state = TeamSavedData.get(player.server).blackboard(player.getUUID()).onboarding();
        if (!state.markDelivered(hint)) return;
        player.sendSystemMessage(Component.literal("[Rift Guide] " + hint.title() + ": " + hint.detail()));
        TeamSavedData.get(player.server).markChanged();
        DecisionTraceService.log(player, "TUTORIAL", traceDetail + " -> " + hint.id());
    }

    public record Result(boolean successful, String code, String detail) {
        public static Result success(final String code, final String detail) { return new Result(true, code, detail); }
        public static Result failure(final String code, final String detail) { return new Result(false, code, detail); }
    }
}
