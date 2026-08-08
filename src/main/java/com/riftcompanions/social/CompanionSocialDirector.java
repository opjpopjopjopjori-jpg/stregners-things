package com.riftcompanions.social;

import com.riftcompanions.debug.DecisionTraceService;
import com.riftcompanions.dialogue.DialogueService;
import com.riftcompanions.entity.CompanionAction;
import com.riftcompanions.entity.CompanionEntity;
import com.riftcompanions.entity.CompanionRole;
import com.riftcompanions.entity.CompanionState;
import com.riftcompanions.feature.FeatureFlag;
import com.riftcompanions.feature.FeatureFlags;
import com.riftcompanions.server.CompanionLifecycleService;
import com.riftcompanions.server.TeamSavedData;
import com.riftcompanions.team.TeamBlackboard;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Low-frequency, server-owned social direction for a two-companion team.
 *
 * <p>It reacts only to player-visible local facts: a player-selected safe
 * interaction block, weather, horizon time, a nearby campfire, and safe-base
 * state. It never reads containers, scans hidden terrain, drives player input,
 * changes AI/combat authority, or sends a line during danger. A reply is queued
 * only after the lead's authored dialogue line was actually delivered.</p>
 */
public final class CompanionSocialDirector {
    private static final long START_COOLDOWN_TICKS = 1200L;
    private static final long PLAYER_CUE_LIFETIME_TICKS = 240L;
    private static final long REPLY_DELAY_TICKS = 36L;
    private static final double SOCIAL_RADIUS = 12.0D;
    private static final Map<UUID, Long> LAST_STARTED = new HashMap<>();
    private static final Map<UUID, RecentCue> RECENT_CUES = new HashMap<>();
    private static final Map<UUID, PendingReply> PENDING_REPLIES = new HashMap<>();
    private static final Map<UUID, SocialTrace> LAST_TRACES = new HashMap<>();

    private CompanionSocialDirector() {}

    /** Called from the bounded Team Director cadence, not every entity tick. */
    public static void tick(final ServerPlayer player, final TeamBlackboard board, final long now) {
        if (player == null || board == null) return;
        if (!FeatureFlags.enabled(FeatureFlag.COMPANION_SOCIAL)) {
            PENDING_REPLIES.remove(player.getUUID());
            return;
        }
        if (!safeForSocial(player, board, now)) {
            PENDING_REPLIES.remove(player.getUUID());
            return;
        }

        final PendingReply pending = PENDING_REPLIES.get(player.getUUID());
        if (pending != null) {
            if (now >= pending.replyAt()) {
                deliverReply(player, pending, now);
            }
            return;
        }

        final long last = LAST_STARTED.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (last != Long.MIN_VALUE && now - last < START_COOLDOWN_TICKS) return;
        final List<CompanionEntity> pair = availablePair(player);
        if (pair.size() != 2) return;
        final SocialCue cue = chooseCue(player, board, now);
        if (cue == null) return;
        final CompanionEntity lead = chooseLead(pair, cue);
        final CompanionEntity reply = pair.stream().filter(candidate -> candidate != lead).findFirst().orElse(null);
        if (lead == null || reply == null) return;

        final DialogueService.SpeakResult result = DialogueService.get().speak(lead, cue.leadTrigger(), 3);
        if (!result.sent()) return;
        lead.beginVisualAction(cue.leadAction(), 28L);
        reply.beginVisualAction(CompanionAction.SOCIAL_LISTEN, REPLY_DELAY_TICKS + 18L);
        PENDING_REPLIES.put(player.getUUID(), new PendingReply(cue, lead.getUUID(), reply.getUUID(), now + REPLY_DELAY_TICKS));
        LAST_TRACES.put(player.getUUID(), new SocialTrace(cue, lead.getRole(), reply.getRole(), "LEAD_DELIVERED", now));
        LAST_STARTED.put(player.getUUID(), now);
        RECENT_CUES.remove(player.getUUID());
        DecisionTraceService.log(player, "SOCIAL", "LEAD " + cue + " " + lead.getRole());
    }

    /**
     * Observes only a block the player actively clicked. Chests and containers
     * are intentionally ignored; this is atmosphere direction, not inventory
     * inspection or hidden-world knowledge.
     */
    public static void observePlayerInteraction(final ServerPlayer player, final BlockPos position) {
        if (player == null || position == null || player.server == null || !FeatureFlags.enabled(FeatureFlag.COMPANION_SOCIAL)) return;
        final ServerLevel level = player.serverLevel();
        if (!level.hasChunkAt(position)) return;
        final BlockState state = level.getBlockState(position);
        final SocialCue cue;
        if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
            cue = SocialCue.CAMPFIRE;
        } else if (state.is(BlockTags.BEDS)) {
            cue = SocialCue.BASE;
        } else if (isVisibleWorkBlock(state.getBlock())) {
            cue = SocialCue.WORK;
        } else {
            return;
        }
        RECENT_CUES.put(player.getUUID(), new RecentCue(cue, player.level().getGameTime()));
    }

    public static SocialStatus status(final UUID owner, final long now) {
        if (owner == null || !FeatureFlags.enabled(FeatureFlag.COMPANION_SOCIAL)) return SocialStatus.disabled();
        final SocialTrace trace = LAST_TRACES.get(owner);
        final PendingReply pending = PENDING_REPLIES.get(owner);
        if (trace == null) return new SocialStatus(true, "NONE", "IDLE", null, null, 0L, "No recent social exchange.");
        final long pendingTicks = pending == null ? 0L : Math.max(0L, pending.replyAt() - now);
        final String summary = trace.phase().equals("REPLY_DELIVERED")
                ? trace.lead().personalName() + " and " + trace.reply().personalName() + " completed a " + trace.cue().name().toLowerCase(java.util.Locale.ROOT) + " exchange."
                : trace.lead().personalName() + " began a " + trace.cue().name().toLowerCase(java.util.Locale.ROOT) + " exchange.";
        return new SocialStatus(true, trace.cue().name(), trace.phase(), trace.lead(), trace.reply(), pendingTicks, summary);
    }

    public static void clearSession(final UUID owner) {
        if (owner == null) return;
        LAST_STARTED.remove(owner);
        RECENT_CUES.remove(owner);
        PENDING_REPLIES.remove(owner);
        LAST_TRACES.remove(owner);
    }

    private static void deliverReply(final ServerPlayer player, final PendingReply pending, final long now) {
        PENDING_REPLIES.remove(player.getUUID());
        final TeamBlackboard board = TeamSavedData.get(player.server).blackboard(player.getUUID());
        if (!safeForSocial(player, board, now)) return;
        final CompanionEntity reply = CompanionLifecycleService.findLoaded(player.server, pending.replyUuid()).orElse(null);
        final CompanionEntity lead = CompanionLifecycleService.findLoaded(player.server, pending.leadUuid()).orElse(null);
        if (!eligible(reply, player) || !eligible(lead, player) || reply.distanceToSqr(lead) > SOCIAL_RADIUS * SOCIAL_RADIUS) return;
        final DialogueService.SpeakResult result = DialogueService.get().speakPairedReply(reply, pending.cue().replyTrigger(), 3);
        if (!result.sent()) return;
        reply.beginVisualAction(CompanionAction.SOCIAL_REASSURE, 26L);
        LAST_TRACES.put(player.getUUID(), new SocialTrace(pending.cue(), lead.getRole(), reply.getRole(), "REPLY_DELIVERED", now));
        DecisionTraceService.log(player, "SOCIAL", "REPLY " + pending.cue() + " " + reply.getRole());
    }

    private static boolean safeForSocial(final ServerPlayer player, final TeamBlackboard board, final long now) {
        if (board.safeMode().enabled() || board.isQuiet(now) || board.plan().isActive() || board.plan().awaitsApproval()) return false;
        if (player.isInLava() || player.isOnFire() || player.fallDistance > 3.0F || player.getHealth() <= player.getMaxHealth() * 0.45F) return false;
        if (board.dangerScore() >= 35) return false;
        return player.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                player.getBoundingBox().inflate(14.0D), net.minecraft.world.entity.monster.Monster::isAlive).isEmpty();
    }

    private static List<CompanionEntity> availablePair(final ServerPlayer player) {
        final List<CompanionEntity> active = new ArrayList<>();
        for (final CompanionRole role : CompanionRole.values()) {
            final CompanionEntity companion = CompanionLifecycleService.findForOwner(player, role).orElse(null);
            if (eligible(companion, player)) active.add(companion);
        }
        active.sort(Comparator.comparing(entity -> entity.getRole().ordinal()));
        return active.size() == 2 && active.get(0).distanceToSqr(active.get(1)) <= SOCIAL_RADIUS * SOCIAL_RADIUS ? active : List.of();
    }

    private static boolean eligible(final CompanionEntity companion, final ServerPlayer player) {
        if (companion == null || player == null || companion.level() != player.level()) return false;
        if (companion.distanceToSqr(player) > SOCIAL_RADIUS * SOCIAL_RADIUS) return false;
        return switch (companion.getCompanionState()) {
            case DOWNED, RETREATING, FIGHTING, STUCK_RECOVERY, EXHAUSTED, RECOVERING, RESTING -> false;
            default -> true;
        };
    }

    private static SocialCue chooseCue(final ServerPlayer player, final TeamBlackboard board, final long now) {
        final RecentCue recent = RECENT_CUES.get(player.getUUID());
        if (recent != null && now - recent.observedAt() <= PLAYER_CUE_LIFETIME_TICKS) return recent.cue();
        if (hasCampfire(player.serverLevel(), player.blockPosition())) return SocialCue.CAMPFIRE;
        if (player.serverLevel().isThundering() || player.serverLevel().isRaining()) return SocialCue.WEATHER;
        final long time = player.level().getDayTime() % 24000L;
        if ((time >= 11600L && time <= 12800L) || (time >= 22800L && time <= 200L)) return SocialCue.HORIZON;
        final String profile = board.encounterContext().profileId();
        if ("dungeon".equals(profile) || "mineshaft".equals(profile) || "stronghold".equals(profile)) return SocialCue.CAVE;
        if ("village".equals(profile)) return SocialCue.VILLAGE;
        if (board.isAtSafeBase(player)) return SocialCue.BASE;
        return player.isSprinting() ? SocialCue.TRAVEL : SocialCue.CALM;
    }

    private static CompanionEntity chooseLead(final List<CompanionEntity> pair, final SocialCue cue) {
        final EnumSet<CompanionRole> preference = switch (cue) {
            case CAMPFIRE, BASE -> EnumSet.of(CompanionRole.GUARDIAN, CompanionRole.GIFTED);
            case WEATHER -> EnumSet.of(CompanionRole.SCOUT, CompanionRole.GUARDIAN);
            case HORIZON -> EnumSet.of(CompanionRole.SEER, CompanionRole.SCOUT);
            case WORK -> EnumSet.of(CompanionRole.GUARDIAN, CompanionRole.SCOUT);
            case CAVE -> EnumSet.of(CompanionRole.SEER, CompanionRole.GUARDIAN);
            case VILLAGE -> EnumSet.of(CompanionRole.GUARDIAN, CompanionRole.SCOUT);
            case TRAVEL -> EnumSet.of(CompanionRole.SCOUT, CompanionRole.GUARDIAN);
            case CALM -> EnumSet.of(CompanionRole.GIFTED, CompanionRole.SEER);
        };
        return pair.stream().filter(companion -> preference.contains(companion.getRole())).findFirst().orElse(pair.get(0));
    }

    private static boolean hasCampfire(final ServerLevel level, final BlockPos origin) {
        for (final BlockPos pos : BlockPos.betweenClosed(origin.offset(-4, -1, -4), origin.offset(4, 1, 4))) {
            if (!level.hasChunkAt(pos)) continue;
            if (level.getBlockState(pos).is(Blocks.CAMPFIRE) || level.getBlockState(pos).is(Blocks.SOUL_CAMPFIRE)) return true;
        }
        return false;
    }

    private static boolean isVisibleWorkBlock(final Block block) {
        return block == Blocks.CRAFTING_TABLE || block == Blocks.FURNACE || block == Blocks.SMOKER
                || block == Blocks.BLAST_FURNACE || block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL
                || block == Blocks.DAMAGED_ANVIL || block == Blocks.GRINDSTONE || block == Blocks.STONECUTTER
                || block == Blocks.CARTOGRAPHY_TABLE || block == Blocks.LOOM;
    }

    private record RecentCue(SocialCue cue, long observedAt) {}
    private record PendingReply(SocialCue cue, UUID leadUuid, UUID replyUuid, long replyAt) {}
    private record SocialTrace(SocialCue cue, CompanionRole lead, CompanionRole reply, String phase, long at) {}

    /** Compact display-only owner projection; no dialogue body or private world data is exposed. */
    public record SocialStatus(boolean enabled, String cue, String phase, CompanionRole lead, CompanionRole reply,
                               long pendingReplyTicks, String summary) {
        public static SocialStatus disabled() {
            return new SocialStatus(false, "NONE", "DISABLED", null, null, 0L, "Companion social exchanges are disabled by the feature flag.");
        }
    }
}
