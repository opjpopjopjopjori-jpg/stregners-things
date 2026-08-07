package com.riftcompanions.debug;

import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamBlackboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

/** Local-only diagnostics export; it never uploads data or contacts the internet. */
public final class DiagnosticsService {
    private DiagnosticsService() {}

    public static DiagnosticResult exportReport(final ServerPlayer player) {
        final TeamBlackboard board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        final StringBuilder report = new StringBuilder();
        report.append("Rift Companions diagnostic report\n");
        report.append("Created: ").append(Instant.now()).append('\n');
        report.append("Player: ").append(player.getGameProfile().getName()).append('\n');
        report.append("Dimension: ").append(player.level().dimension().location()).append('\n');
        report.append("Plan: ").append(board.plan().type()).append('/').append(board.plan().status()).append('\n');
        report.append("Formation: ").append(board.formation()).append('\n');
        report.append("Danger: ").append(board.dangerScore()).append('\n');
        report.append("Reasons: ").append(board.lastReasonCodes()).append('\n');
        report.append("SafeMode: ").append(board.safeMode().enabled()).append(" / ").append(board.safeMode().reason())
                .append(" / ").append(board.safeMode().detail()).append('\n');
        report.append("Requested duo: ").append(board.requestedPair().map(pair -> pair.name()).orElse("none")).append('\n');
        report.append("Open intention: ").append(com.riftcompanions.intention.IntentionService.summary(board)).append('\n');
        report.append("Personal policy overrides: ").append(board.playerPolicy().overrides()).append('\n');
        final var worldRules = com.riftcompanions.mode.VanillaRulePolicyService.snapshot(player);
        report.append("Vanilla rules: difficulty=").append(worldRules.difficulty())
                .append(" mob_griefing=").append(worldRules.mobGriefing())
                .append(" keep_inventory=").append(worldRules.keepInventory())
                .append(" daylight_cycle=").append(worldRules.daylightCycle())
                .append(" hardcore=").append(worldRules.hardcore()).append('\n');
        report.append("Tutorial: delivered=").append(board.onboarding().deliveredCount()).append('/').append(board.onboarding().totalHints())
                .append(" dismissed=").append(board.onboarding().dismissed())
                .append(" next=").append(board.onboarding().recommendedHint().map(hint -> hint.id()).orElse("complete")).append('\n');
        report.append("Gifted readiness: ").append(board.giftedBehavior().summary())
                .append(" suggestion=").append(board.giftedBehavior().lastSuggestion()).append('\n');
        report.append("Guardian review: ").append(board.guardianReview().status()).append(" / ")
                .append(board.guardianReview().summary()).append(" / ").append(board.guardianReview().reasonCode()).append('\n');
        final var performance = com.riftcompanions.performance.CompanionPerformanceMonitor.snapshot(player.getUUID());
        report.append("Developer timing enabled: ").append(performance.enabled()).append(" sampled_us=")
                .append(performance.sampledTotalMicros()).append(" avg_us=").append(performance.averageTotalMicros())
                .append(" ai_us=").append(performance.latestCompanionMicros()).append(" nav_us=")
                .append(performance.latestNavigationMicros()).append(" director_us=").append(performance.latestDirectorMicros()).append('\n');
        final var chat = com.riftcompanions.dialogue.DialogueService.get().debugSnapshot(player.getUUID(), player.level().getGameTime());
        report.append("Dialogue debug: trigger=").append(chat.trigger()).append(" outcome=").append(chat.outcome())
                .append(" normal_budget_remaining=").append(chat.normalBudgetRemainingTicks()).append('\n');
        report.append("Recent actions: ").append(board.actionLedger().recent(8).stream()
                .map(action -> action.type() + ":" + action.phase() + ":" + action.reasonCode()).toList()).append('\n');
        report.append("Companion fault records: ").append(board.faultRecords()).append('\n');
        report.append("Playtest acceptance: ").append(com.riftcompanions.playtest.PlaytestTelemetryService.summary(board.playtestTelemetry())).append('\n');
        report.append("Compatibility packs: ").append(com.riftcompanions.compat.CompatibilityPackRegistry.allStatuses()).append('\n');
        report.append("Chat profile: ").append(com.riftcompanions.config.CompanionConfig.CHAT_PROFILE.get()).append('\n');
        report.append("Low effects: ").append(com.riftcompanions.config.CompanionConfig.LOW_EFFECTS.get()).append('\n');
        for (final CompanionRole role : CompanionRole.values()) {
            final var companion = CompanionLifecycleService.findForOwner(player, role);
            if (companion.isPresent()) {
                final var c = companion.get();
                report.append(role.id()).append(": state=").append(c.getCompanionState())
                        .append(" energy=").append(c.getEnergy())
                        .append(" strain=").append(c.getHiveStrain())
                        .append(" focus=").append(c.getFocus())
                        .append(" inventory=").append(c.getPersonalInventory().occupiedSlots())
                        .append(" reason=").append(c.getLastReasonCode()).append('\n');
                final var navigation = com.riftcompanions.navigation.CompanionNavigationService.debugSnapshot(c.getUUID());
                report.append("  navigation: intent=").append(navigation.intent())
                        .append(" requested=").append(navigation.requestedTarget())
                        .append(" selected=").append(navigation.selectedTarget())
                        .append(" outcome=").append(navigation.outcome())
                        .append(" retries=").append(navigation.recoveryAttempts())
                        .append(" reason=").append(navigation.reasonCode()).append('\n');
            }
        }
        report.append("Recent decisions:\n");
        for (final DecisionTraceEntry entry : DecisionTraceService.recent(player.getUUID())) {
            report.append(entry.gameTime()).append(" [").append(entry.category()).append("] ").append(entry.detail()).append('\n');
        }
        try {
            final Path directory = player.server.getWorldPath(LevelResource.ROOT).resolve("riftcompanions-reports");
            Files.createDirectories(directory);
            final Path file = directory.resolve("report-" + player.level().getGameTime() + ".txt");
            Files.writeString(file, report.toString(), StandardCharsets.UTF_8);
            return DiagnosticResult.success("REPORT_EXPORTED", "Exported the local report to " + file.getFileName());
        } catch (final IOException exception) {
            return DiagnosticResult.failure("REPORT_EXPORT_FAILED", "Could not write a local report: " + exception.getClass().getSimpleName());
        }
    }

    public static DiagnosticResult why(final ServerPlayer player, final CompanionRole role) {
        final var companion = CompanionLifecycleService.findForOwner(player, role);
        if (companion.isEmpty()) {
            return DiagnosticResult.failure("COMPANION_NOT_LOADED", "That companion is not currently loaded.");
        }
        final var board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        final var c = companion.get();
        String limitation = switch (role) {
            case SEER -> "Hive strain=" + Math.round(c.getHiveStrain());
            case GIFTED -> "Energy=" + Math.round(c.getEnergy());
            case SCOUT -> "Focus=" + Math.round(c.getFocus());
            case GUARDIAN -> "Combat profile=" + com.riftcompanions.config.CompanionConfig.COMBAT_PROFILE.get();
        };
        return DiagnosticResult.success("WHY", role.personalName() + " is " + c.getCompanionState()
                + " because " + c.getLastReasonCode() + ". Plan=" + board.plan().type() + "/" + board.plan().status()
                + ". " + limitation);
    }

    public static DiagnosticResult repairSafeState(final ServerPlayer player) {
        final TeamBlackboard board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        if (board.plan().isActive() || board.plan().awaitsApproval()) {
            final var repairedPlan = board.plan();
            board.completePlan(com.riftcompanions.team.TeamPlanStatus.CANCELLED);
            com.riftcompanions.behavior.GuardianBehaviorService.recordAfterAction(player, repairedPlan,
                    com.riftcompanions.team.TeamPlanStatus.CANCELLED, "DIAGNOSTIC_SAFE_REPAIR");
        }
        com.riftcompanions.server.GuardianBraceService.cancelForOwner(player, "DIAGNOSTIC_SAFE_REPAIR");
        board.setFormation(com.riftcompanions.formation.FormationType.FOLLOW);
        board.clearFocusTarget();
        for (final CompanionRole role : CompanionRole.values()) {
            CompanionLifecycleService.findForOwner(player, role).ifPresent(companion -> {
                companion.getNavigation().stop();
                companion.setTarget(null);
                if (companion.getCompanionState() != com.riftcompanions.entity.CompanionState.DOWNED) {
                    companion.setCompanionState(com.riftcompanions.entity.CompanionState.FOLLOWING, "DIAGNOSTIC_SAFE_REPAIR");
                }
            });
        }
        TeamSavedData.get(player.server).markChanged();
        DecisionTraceService.log(player, "REPAIR", "Reset plan/focus/navigation to safe follow state");
        return DiagnosticResult.success("SAFE_REPAIR_COMPLETE", "Stopped the plan and safely returned companions to Follow mode.");
    }

    public record DiagnosticResult(boolean successful, String code, String detail) {
        public static DiagnosticResult success(final String code, final String detail) { return new DiagnosticResult(true, code, detail); }
        public static DiagnosticResult failure(final String code, final String detail) { return new DiagnosticResult(false, code, detail); }
    }
}
