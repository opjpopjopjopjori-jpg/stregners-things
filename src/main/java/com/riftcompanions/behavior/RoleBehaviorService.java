package com.riftcompanions.behavior;

import com.riftcompanions.config.CompanionConfig;
import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import net.minecraft.server.level.ServerPlayer;

/** Role-specific high-level decisions layered over shared navigation/team safety. */
public final class RoleBehaviorService {
    private RoleBehaviorService() {}

    public static void observeWillEvidence(ServerPlayer player, String evidenceKey, boolean confirmedTag) {
        if (player == null || player.server == null) return;
        CompanionEntity will = CompanionLifecycleService.findForOwner(player, CompanionRole.SEER).orElse(null);
        if (will == null || will.getCompanionState() == CompanionState.DOWNED) return;
        WillAwarenessState state = TeamSavedData.get(player.server).blackboard(player.getUUID()).willAwareness();
        long now = player.level().getGameTime();
        state.clearIfExpired(now);
        if (!state.observe(evidenceKey, confirmedTag, now) || state.stressedAt(now) || now - state.lastDialogueAt() < CompanionConfig.WILL_SIGNAL_DIALOGUE_COOLDOWN_TICKS.get()) return;
        String trigger = switch (state.level()) {
            case LOW -> "will_signal_low";
            case MEDIUM -> "will_signal_medium";
            case HIGH -> "will_signal_high";
            default -> "";
        };
        if (!trigger.isBlank()) {
            will.beginVisualAction(com.riftcompanions.entity.CompanionAction.SEER_NOTICE, 14L);
            DialogueService.get().speak(will, trigger, state.level() == WillSignalLevel.HIGH ? 1 : 3);
            state.markDialogue(now);
            TeamSavedData.get(player.server).markChanged();
        }
    }

    public static void markWillStressed(ServerPlayer player, long duration) {
        if (player == null || player.server == null) return;
        TeamSavedData.get(player.server).blackboard(player.getUUID()).willAwareness().markStressed(player.level().getGameTime(), duration);
        TeamSavedData.get(player.server).markChanged();
    }

    public static ScoutDecision evaluateScout(ServerPlayer player) {
        if (player == null) return ScoutDecision.reject("SCOUT_CONTEXT_INVALID");
        if (player.getHealth() <= player.getMaxHealth() * 0.35F) return ScoutDecision.reject("SCOUT_PLAYER_CRITICAL");
        boolean downed = java.util.Arrays.stream(CompanionRole.values()).map(role -> CompanionLifecycleService.findForOwner(player, role))
                .flatMap(java.util.Optional::stream).anyMatch(companion -> companion.getCompanionState() == CompanionState.DOWNED);
        if (downed) return ScoutDecision.reject("SCOUT_TEAMMATE_DOWNED");
        if (TeamSavedData.get(player.server).blackboard(player.getUUID()).encounterContext().profileId().equals("boss")) return ScoutDecision.reject("SCOUT_BOSS_CONTEXT");
        if (TeamSavedData.get(player.server).blackboard(player.getUUID()).dangerScore() >= 70) return ScoutDecision.reject("SCOUT_DANGER_TOO_HIGH");
        return ScoutDecision.allow();
    }

    public static void recordScoutReturn(ServerPlayer player, CompanionEntity scout) {
        if (player == null || scout == null) return;
        TeamSavedData.get(player.server).blackboard(player.getUUID()).addMemory(new com.riftcompanions.memory.MemoryRecord(
                com.riftcompanions.memory.MemoryType.SAFE_ROUTE, player.level().getDayTime()/24000L,
                "Scout returned with a route observation from loaded, visible terrain.", 68));
        TeamSavedData.get(player.server).markChanged();
    }

    public record ScoutDecision(boolean allowed, String code) {
        static ScoutDecision allow() { return new ScoutDecision(true, "SCOUT_ALLOWED"); }
        static ScoutDecision reject(String code) { return new ScoutDecision(false, code); }
    }
}
