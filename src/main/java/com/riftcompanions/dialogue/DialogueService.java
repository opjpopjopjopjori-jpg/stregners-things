package com.riftcompanions.dialogue;

import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.performance.CompanionPerformanceMonitor;
import com.riftcompanions.performance.PerformanceWorkType;
import com.riftcompanions.server.TeamSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A bounded bark scheduler. Alerts can pre-empt ambient lines, while normal
 * dialogue remains capped to avoid chat spam during long play sessions.
 *
 * <p>The result-returning API lets the social director schedule a reply only
 * after the lead line was actually delivered. Dialogue remains authored English
 * data; no external AI, internet service, actor voice, or generated player text
 * is used at runtime.</p>
 */
public final class DialogueService {
    private static final DialogueService INSTANCE = new DialogueService();
    private static final long NORMAL_CHAT_BUDGET = 600L; // 30 seconds; max two normal barks/minute.

    private final Map<UUID, Long> lastNormalMessageAt = new HashMap<>();
    private final Map<UUID, Map<String, Long>> lastLineAt = new HashMap<>();
    private final Map<UUID, DialogueDebugState> debugState = new HashMap<>();

    private DialogueService() {}

    public static DialogueService get() {
        return INSTANCE;
    }

    /** @param requestedPriority 0=critical, 4=ambient. */
    public SpeakResult speak(final CompanionEntity companion, final String trigger, final int requestedPriority) {
        return speakInternal(companion, trigger, requestedPriority, false);
    }

    /** A single director-approved social reply may follow an already delivered lead line. */
    public SpeakResult speakPairedReply(final CompanionEntity companion, final String trigger, final int requestedPriority) {
        return speakInternal(companion, trigger, requestedPriority, true);
    }

    private SpeakResult speakInternal(final CompanionEntity companion, final String trigger, final int requestedPriority,
                                      final boolean pairedReply) {
        if (companion == null || companion.level().isClientSide) {
            return SpeakResult.notSent("INVALID_CONTEXT");
        }
        final Optional<ServerPlayer> owner = companion.getOwnerPlayer();
        if (owner.isEmpty()) {
            return SpeakResult.notSent("OWNER_UNAVAILABLE");
        }
        final ServerPlayer player = owner.get();
        final long now = companion.level().getGameTime();
        final long performanceStartedAt = CompanionPerformanceMonitor.start();
        try {
            recordDebug(player.getUUID(), trigger, "REQUESTED", now);
            if (!com.riftcompanions.config.CompanionConfig.CHAT_PROFILE.get().allowsPriority(requestedPriority)) {
                recordDebug(player.getUUID(), trigger, "CHAT_PROFILE_BLOCKED", now);
                return SpeakResult.notSent("CHAT_PROFILE_BLOCKED");
            }
            if (requestedPriority >= 2 && isMuted(companion.getRole())) {
                recordDebug(player.getUUID(), trigger, "ROLE_MUTED", now);
                return SpeakResult.notSent("ROLE_MUTED");
            }
            if (requestedPriority >= 3 && TeamSavedData.get(player.server).blackboard(player.getUUID()).isQuiet(now)
                    && !"post_crisis".equals(trigger) && !"recovery".equals(trigger)) {
                recordDebug(player.getUUID(), trigger, "QUIET_WINDOW_BLOCKED", now);
                return SpeakResult.notSent("QUIET_WINDOW_BLOCKED");
            }
            final List<DialogueLine> candidates = DialogueReloadListener.INSTANCE.find(companion.getRole(), trigger).stream()
                    .filter(line -> line.priority() <= requestedPriority)
                    .filter(line -> available(player.getUUID(), line, now))
                    .sorted(Comparator.comparingInt(DialogueLine::priority))
                    .toList();
            // Enhanced: check DialogueDatabase for additional professional dialogue lines
            // when JSON reload listener has no matches (prevents NO_AVAILABLE_LINE errors)
            List<DialogueLine> combinedCandidates = new java.util.ArrayList<>(candidates);
            if (candidates.isEmpty()) {
                for (String text : com.riftcompanions.dialogue.DialogueDatabase.getDialogue(trigger)) {
                    combinedCandidates.add(new DialogueLine("db_" + trigger + "_" + text.hashCode(), companion.getRole(), trigger, 2, 1200L, text));
                }
            }
            final List<DialogueLine> finalCandidates = combinedCandidates.isEmpty() ? candidates : combinedCandidates;
            if (finalCandidates.isEmpty()) {
                recordDebug(player.getUUID(), trigger, "NO_AVAILABLE_LINE", now);
                return SpeakResult.notSent("NO_AVAILABLE_LINE");
            }
            final DialogueLine selected = finalCandidates.get(companion.getRandom().nextInt(finalCandidates.size()));
            final Long lastNormal = lastNormalMessageAt.get(player.getUUID());
            if (selected.priority() >= 2 && !pairedReply && lastNormal != null && now - lastNormal < NORMAL_CHAT_BUDGET) {
                recordDebug(player.getUUID(), trigger, "NORMAL_BUDGET_COOLDOWN", now);
                return SpeakResult.notSent("NORMAL_BUDGET_COOLDOWN");
            }
            lastLineAt.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).put(selected.id(), now);
            // A paired reply shares the lead line's existing normal-chat budget;
            // it cannot open a new chatter window or bypass the director cooldown.
            if (selected.priority() >= 2 && !pairedReply) {
                lastNormalMessageAt.put(player.getUUID(), now);
            }
            player.sendSystemMessage(Component.literal("[" + companion.getRole().personalName() + "] " + selected.text()));
            final String outcome = "SENT:" + selected.id();
            recordDebug(player.getUUID(), trigger, outcome, now);
            return SpeakResult.sent(selected.id());
        } finally {
            CompanionPerformanceMonitor.record(player.getUUID(), PerformanceWorkType.DIALOGUE, performanceStartedAt);
        }
    }

    /** Bounded, non-persistent developer projection. It contains no dialogue body or player chat history. */
    public DialogueDebugSnapshot debugSnapshot(final UUID player, final long now) {
        final DialogueDebugState state = debugState.get(player);
        final long lastNormal = lastNormalMessageAt.getOrDefault(player, Long.MIN_VALUE);
        final long remaining = lastNormal == Long.MIN_VALUE ? 0L : Math.max(0L, NORMAL_CHAT_BUDGET - (now - lastNormal));
        return state == null ? new DialogueDebugSnapshot("", "NO_CHAT_ACTIVITY", 0L)
                : new DialogueDebugSnapshot(state.trigger, state.outcome, remaining);
    }

    private static boolean isMuted(final com.riftcompanions.entity.CompanionRole role) {
        return switch (role) {
            case GUARDIAN -> com.riftcompanions.config.CompanionConfig.MUTE_GUARDIAN.get();
            case SEER -> com.riftcompanions.config.CompanionConfig.MUTE_SEER.get();
            case GIFTED -> com.riftcompanions.config.CompanionConfig.MUTE_GIFTED.get();
            case SCOUT -> com.riftcompanions.config.CompanionConfig.MUTE_SCOUT.get();
        };
    }

    private boolean available(final UUID player, final DialogueLine line, final long now) {
        final Long last = lastLineAt.computeIfAbsent(player, ignored -> new HashMap<>()).get(line.id());
        return last == null || now - last >= line.cooldownTicks();
    }

    private void recordDebug(final UUID player, final String trigger, final String outcome, final long now) {
        if (player == null) return;
        final String safeTrigger = trigger == null ? "" : trigger.substring(0, Math.min(64, trigger.length()));
        final String safeOutcome = outcome == null ? "" : outcome.substring(0, Math.min(96, outcome.length()));
        debugState.put(player, new DialogueDebugState(safeTrigger, safeOutcome, Math.max(0L, now)));
    }

    public void clearSession(final UUID player) {
        lastNormalMessageAt.remove(player);
        lastLineAt.remove(player);
        debugState.remove(player);
    }

    private record DialogueDebugState(String trigger, String outcome, long at) {}

    public record DialogueDebugSnapshot(String trigger, String outcome, long normalBudgetRemainingTicks) {}

    public record SpeakResult(boolean sent, String outcome) {
        public static SpeakResult sent(final String lineId) {
            return new SpeakResult(true, "SENT:" + (lineId == null ? "" : lineId));
        }

        public static SpeakResult notSent(final String outcome) {
            return new SpeakResult(false, outcome == null ? "" : outcome);
        }
    }
}
