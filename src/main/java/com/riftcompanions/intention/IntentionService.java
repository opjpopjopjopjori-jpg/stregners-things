package com.riftcompanions.intention;

import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.feature.FeatureFlag;
import com.riftcompanions.feature.FeatureFlags;
import com.riftcompanions.memory.MemoryRecord;
import com.riftcompanions.memory.MemoryType;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamBlackboard;
import com.riftcompanions.transaction.ActionPhase;
import com.riftcompanions.transaction.ActionTransaction;
import com.riftcompanions.transaction.ActionType;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Optional intentions are discovered from real safe/base context and completed
 * only by validated world events. They are neither timers nor loot quests.
 */
public final class IntentionService {
    private IntentionService() {}

    public static void tick(final ServerPlayer player, final TeamBlackboard board, final long now) {
        if (!FeatureFlags.enabled(FeatureFlag.COMPANION_INTENTIONS) || board.safeMode().enabled()
                || board.plan().isActive() || board.plan().awaitsApproval() || board.openIntention().isPresent()) return;
        final long day = player.level().getDayTime() / 24000L;
        if (board.lastIntentionOfferDay() == day || !board.isAtSafeBase(player)) return;
        final Optional<IntentionDefinition> next = nextDefinition(player, board);
        if (next.isEmpty()) return;
        final CompanionIntention intention = CompanionIntention.create(next.get(), day, player.getUUID());
        board.offerIntention(intention);
        board.setLastIntentionOfferDay(day);
        TeamSavedData.get(player.server).markChanged();
        CompanionLifecycleService.findForOwner(player, intention.role())
                .ifPresent(companion -> DialogueService.get().speak(companion, next.get().dialogueTrigger(), 3));
    }

    public static IntentionResult accept(final ServerPlayer player) {
        return transition(player, true);
    }

    public static IntentionResult defer(final ServerPlayer player) {
        return transition(player, false);
    }

    private static IntentionResult transition(final ServerPlayer player, final boolean accept) {
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        final CompanionIntention intention = board.openIntention().orElse(null);
        if (intention == null) return IntentionResult.failure("NO_OPEN_INTENTION", "No optional companion intention is available right now.");
        final long day = player.level().getDayTime() / 24000L;
        final boolean changed = accept ? intention.accept(day) : intention.defer(day);
        if (!changed) return IntentionResult.failure("INTENTION_STATE_REJECTED", "That intention cannot change state right now.");
        data.markChanged();
        return IntentionResult.success(accept ? "INTENTION_ACCEPTED" : "INTENTION_DEFERRED",
                accept ? "Optional intention accepted. It can still be completed later without pressure." : "Optional intention deferred. It remains available later.");
    }

    public static void observeCompletion(final ServerPlayer player, final String completionKey) {
        if (!FeatureFlags.enabled(FeatureFlag.COMPANION_INTENTIONS) || completionKey == null) return;
        final TeamSavedData data = TeamSavedData.get(player.server);
        final TeamBlackboard board = data.blackboard(player.getUUID());
        final CompanionIntention intention = board.openIntention().filter(value -> value.status() == IntentionStatus.ACTIVE).orElse(null);
        if (intention == null) return;
        final IntentionDefinition definition = IntentionDefinitionRegistry.get(intention.definitionId()).orElse(null);
        if (definition == null || !completionKey.equals(definition.completionKey())) return;
        final long now = player.level().getGameTime();
        final var transactionBegin = board.actionLedger().begin(ActionType.INTENTION_COMPLETE,
                "intention:" + intention.actionId(), definition.id(), now, 200L);
        if (transactionBegin.reused()) return;
        final ActionTransaction transaction = transactionBegin.transaction();
        if (transaction.phase() == ActionPhase.PREPARED) transaction.reserve("INTENTION_COMPLETION_VALIDATED");
        if (transaction.phase() == ActionPhase.RESERVED) transaction.applied("INTENTION_STATE_UPDATED");
        if (!intention.complete(player.level().getDayTime() / 24000L)) {
            transaction.rollback(now, "INTENTION_ALREADY_TERMINAL");
            return;
        }
        transaction.commit(now, "INTENTION_COMPLETED");
        board.addMemory(new MemoryRecord(MemoryType.INTENTION, now / 24000L, definition.rewardSummary(), 82));
        data.markChanged();
        CompanionLifecycleService.findForOwner(player, intention.role())
                .ifPresent(companion -> DialogueService.get().speak(companion, "intention_complete", 3));
    }

    public static String summary(final TeamBlackboard board) {
        final CompanionIntention intention = board.openIntention().orElse(null);
        if (intention == null) return "No optional intention is active.";
        final IntentionDefinition definition = IntentionDefinitionRegistry.get(intention.definitionId()).orElse(null);
        return definition == null ? intention.role().personalName() + " has an unavailable content definition."
                : intention.role().personalName() + " — " + intention.status() + ": " + definition.optionalAction();
    }

    private static Optional<IntentionDefinition> nextDefinition(final ServerPlayer player, final TeamBlackboard board) {
        return java.util.Arrays.stream(CompanionRole.values())
                .filter(role -> CompanionLifecycleService.findForOwner(player, role).isPresent()
                        || TeamSavedData.get(player.server).entry(player.getUUID(), role)
                        .map(entry -> entry.lifecycle() == com.riftcompanions.entity.CompanionLifecycle.RESTING).orElse(false))
                .flatMap(role -> IntentionDefinitionRegistry.forRole(role).stream())
                .filter(definition -> !board.hasCompletedIntention(definition.id()))
                .sorted(Comparator.comparing(IntentionDefinition::id))
                .findFirst();
    }

    public record IntentionResult(boolean successful, String code, String detail) {
        public static IntentionResult success(final String code, final String detail) { return new IntentionResult(true, code, detail); }
        public static IntentionResult failure(final String code, final String detail) { return new IntentionResult(false, code, detail); }
    }
}
