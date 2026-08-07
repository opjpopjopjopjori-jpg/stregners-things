package com.riftcompanions.conversation;

import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamBlackboard;
import com.riftcompanions.team.TeamPlanService;
import com.riftcompanions.world.BaseAnchorType;
import com.riftcompanions.world.TeamAnchor;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-side contextual replies assembled only from player-visible team state. */
public final class ConversationService {
    private static final Map<UUID, Map<String, Long>> TOPIC_COOLDOWNS = new HashMap<>();
    private ConversationService() {}

    public static ConversationResult respond(final ServerPlayer player, final CompanionRole role, final ConversationTopic topic) {
        final CompanionEntity companion = CompanionLifecycleService.findForOwner(player, role).orElse(null);
        if (companion == null) {
            return ConversationResult.failure("COMPANION_NOT_AVAILABLE", "That companion is not nearby right now.");
        }
        if (companion.distanceToSqr(player) > 6.0D * 6.0D) {
            return ConversationResult.failure("TOO_FAR_FOR_CONVERSATION", "Move closer to the companion before starting a conversation.");
        }
        final TeamBlackboard board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        final long now = player.level().getGameTime();
        final String cooldownKey = role.id() + ":" + topic.name();
        final Long lastTopic = TOPIC_COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>()).get(cooldownKey);
        if (lastTopic != null && now - lastTopic < 200L && topic != ConversationTopic.ACCEPT_PLAN && topic != ConversationTopic.DELAY_PLAN) {
            return ConversationResult.success("TOPIC_COOLDOWN", "We talked about that recently. Check the Team Journal for the latest details.");
        }
        if (companion.getCompanionState() == CompanionState.DOWNED) {
            return ConversationResult.success("DOWNED_STATUS", "They need rescue. Stay close and make the area safe.");
        }
        if (companion.getCompanionState() == CompanionState.RETREATING || board.plan().isActive()) {
            if (topic != ConversationTopic.PLAN && topic != ConversationTopic.ARE_YOU_OKAY
                    && topic != ConversationTopic.ACCEPT_PLAN && topic != ConversationTopic.DELAY_PLAN) {
                return ConversationResult.success("TACTICAL_STATUS_ONLY", "This is not a good time for a long conversation. Plan: " + board.plan().type() + " / " + board.plan().status());
            }
        }
        if (topic == ConversationTopic.ACCEPT_PLAN) {
            final TeamPlanService.PlanResult result = TeamPlanService.acceptCurrentPlan(player);
            return result.successful() ? ConversationResult.success(result.code(), result.detail()) : ConversationResult.failure(result.code(), result.detail());
        }
        if (topic == ConversationTopic.DELAY_PLAN) {
            final TeamPlanService.PlanResult result = TeamPlanService.declineCurrentPlan(player);
            return result.successful() ? ConversationResult.success(result.code(), result.detail()) : ConversationResult.failure(result.code(), result.detail());
        }
        final String answer = switch (topic) {
            case PLAN -> planReply(board);
            case ARE_YOU_OKAY -> healthReply(companion);
            case MEMORY -> memoryReply(board);
            case PLACE -> placeReply(board);
            case ROLE_POLICY -> roleReply(companion);
            case OFFER_REST -> offerRest(player, companion, board);
            case WORLD -> worldReply(player, companion);
            case TEAM -> teamReply(player, board);
            case LAST_ENCOUNTER -> lastEncounterReply(board);
            case CHECK_IN -> checkInReply(player, companion, board);
            case ACCEPT_PLAN, DELAY_PLAN -> throw new IllegalStateException("handled above");
        };
        companion.beginVisualAction(com.riftcompanions.entity.CompanionAction.TALK, 24L);
        TOPIC_COOLDOWNS.get(player.getUUID()).put(cooldownKey, now);
        return ConversationResult.success("CONVERSATION_REPLY", answer);
    }

    public static void clearSession(final UUID player) {
        TOPIC_COOLDOWNS.remove(player);
    }

    private static String planReply(final TeamBlackboard board) {
        if (board.plan().type() == com.riftcompanions.team.TeamPlanType.NONE) {
            return "There is no active plan. We should choose a small goal before nightfall.";
        }
        return "Plan: " + board.plan().type() + ". " + board.plan().objective()
                + (board.plan().awaitsApproval() ? " The decision is yours: accept or decline." : "");
    }

    private static String healthReply(final CompanionEntity companion) {
        final String special = switch (companion.getRole()) {
            case SEER -> "Hive strain: " + Math.round(companion.getHiveStrain());
            case SCOUT -> "Focus: " + Math.round(companion.getFocus());
            default -> "Energy: " + Math.round(companion.getEnergy());
        };
        return "Current state: " + companion.getCompanionState() + ". " + special + ".";
    }

    private static String memoryReply(final TeamBlackboard board) {
        final var memories = board.memories();
        if (memories.isEmpty()) {
            return "No important memory has been recorded yet. We need a journey worth remembering.";
        }
        return memories.get(memories.size() - 1).summary();
    }

    private static String placeReply(final TeamBlackboard board) {
        if (board.lastReasonCodes().isEmpty()) {
            return "There is no special warning for this place. Stay alert, but do not guess.";
        }
        return "Recent signals: " + String.join(", ", board.lastReasonCodes());
    }

    private static String roleReply(final CompanionEntity companion) {
        return switch (companion.getRole()) {
            case GUARDIAN -> "I protect the team and keep a route back. I will not take the final decision from you.";
            case SEER -> "I will share the signals I notice, but I will not claim more than the evidence proves.";
            case GIFTED -> "I can help with my power, but we should agree on the timing and limits first.";
            case SCOUT -> "I find the route and return. Scouting is never an excuse to disappear.";
        };
    }

    private static String worldReply(final ServerPlayer player, final CompanionEntity companion) {
        final long time = player.level().getDayTime() % 24000L;
        final String light = player.serverLevel().getMaxLocalRawBrightness(player.blockPosition()) <= 5 ? "Low light makes the next move harder to read."
                : "The ground is visible enough to make a careful choice.";
        final String weather = player.serverLevel().isThundering() ? "Thunder is close. Keep the route simple."
                : player.serverLevel().isRaining() ? "Rain can hide sound and footing."
                : time >= 12000L && time <= 23000L ? "Night is changing the risk around us."
                : "The weather is stable for now.";
        return switch (companion.getRole()) {
            case GUARDIAN -> weather + " " + light + " I am watching the way back.";
            case SEER -> light + " I will describe what is visible, not what I cannot prove.";
            case GIFTED -> weather + " Tell me early if you need space or protection.";
            case SCOUT -> weather + " " + light + " I can watch a short safe route, never the unseen distance.";
        };
    }

    private static String teamReply(final ServerPlayer player, final TeamBlackboard board) {
        final List<String> present = new java.util.ArrayList<>();
        for (CompanionRole role : CompanionRole.values()) {
            CompanionEntity companion = CompanionLifecycleService.findForOwner(player, role).orElse(null);
            if (companion != null) present.add(role.personalName() + "=" + companion.getCompanionState());
        }
        return present.isEmpty() ? "No companion is currently nearby. Call a role before asking for a team check."
                : "Team check: " + String.join(", ", present) + ". Formation: " + board.formation() + ".";
    }

    private static String lastEncounterReply(final TeamBlackboard board) {
        final var memories = board.memories();
        for (int index = memories.size() - 1; index >= 0; index--) {
            final var memory = memories.get(index);
            if (memory.type() == com.riftcompanions.memory.MemoryType.THREAT_OBSERVATION
                    || memory.type() == com.riftcompanions.memory.MemoryType.RETREAT
                    || memory.type() == com.riftcompanions.memory.MemoryType.TEAM_RESCUE) {
                return memory.summary();
            }
        }
        return "No important encounter has been recorded yet. We should observe before we assume a pattern.";
    }

    private static String checkInReply(final ServerPlayer player, final CompanionEntity companion, final TeamBlackboard board) {
        final String resource = switch (companion.getRole()) {
            case SEER -> "Strain " + Math.round(companion.getHiveStrain()) + ", energy " + Math.round(companion.getEnergy());
            case SCOUT -> "Focus " + Math.round(companion.getFocus()) + ", energy " + Math.round(companion.getEnergy());
            default -> "Energy " + Math.round(companion.getEnergy());
        };
        final String safety = board.dangerScore() >= 35 ? "The team is still under pressure."
                : player.getHealth() <= player.getMaxHealth() * 0.50F ? "Your health needs attention."
                : "The immediate situation is manageable.";
        return companion.getRole().personalName() + ": " + resource + ". " + safety;
    }

    private static String offerRest(final ServerPlayer player, final CompanionEntity companion, final TeamBlackboard board) {
        final TeamAnchor target = switch (companion.getRole()) {
            case GUARDIAN -> board.anchor(BaseAnchorType.GUARD_POST).or(() -> board.anchor(BaseAnchorType.HOME)).orElse(null);
            case SEER -> board.anchor(BaseAnchorType.JOURNAL).or(() -> board.anchor(BaseAnchorType.HOME)).orElse(null);
            case GIFTED -> board.anchor(BaseAnchorType.QUIET).or(() -> board.anchor(BaseAnchorType.REST)).or(() -> board.anchor(BaseAnchorType.HOME)).orElse(null);
            case SCOUT -> board.anchor(BaseAnchorType.LOOKOUT).or(() -> board.anchor(BaseAnchorType.HOME)).orElse(null);
        };
        if (target == null || !target.dimension().equals(player.level().dimension().location()) || !player.serverLevel().hasChunkAt(target.position())) {
            return "There is no suitable loaded rest anchor. Set a Home or Rest anchor first.";
        }
        return companion.beginBaseActivity(target.position()) ? "Okay. I will move to the rest point and stay close." : "I cannot move to that rest point safely right now.";
    }

    public record ConversationResult(boolean successful, String code, String text) {
        public static ConversationResult success(final String code, final String text) { return new ConversationResult(true, code, text); }
        public static ConversationResult failure(final String code, final String text) { return new ConversationResult(false, code, text); }
    }
}
